package hr.yeti.usql;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.ERROR;

public class Sql {

    private final System.Logger logger = System.getLogger(Sql.class.getName());

    private String url;
    private String user;
    private String password;
    private DataSource dataSource;

    public Sql(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Sql(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection connect() {
        try {
            if (dataSource != null) {
                return dataSource.getConnection();
            } else {
                return DriverManager.getConnection(url, user, password);
            }
        } catch (SQLException ex) {
            throw new USqlException(ex);
        }
    }

    public class Query {

        private final Connection conn;
        private final boolean tx;

        public Query(boolean tx) throws SQLException {
            this.conn = connect();
            this.tx = tx;
            if (tx) {
                this.conn.setAutoCommit(false);
            }
        }

        public <T> List<T> rows(String sql, RowMapper<T> rowMapper, Object... params) {
            List<Map<String, Object>> rows = rows(sql, params);

            return rows
                .stream()
                .map(rowMapper::map)
                .collect(Collectors.toList());
        }

        public List<Map<String, Object>> rows(String sql, Object... params) {
            List<Map<String, Object>> result = new ArrayList<>();

            Statement statement = null;
            ResultSet resultSet = null;

            try {
                boolean dynamic = Objects.nonNull(params) && params.length > 0;
                statement = dynamic ? conn.prepareStatement(sql) : conn.createStatement();
                if (dynamic) {
                    int paramIndex = 1;
                    for (Object param : params) {
                        ((PreparedStatement) statement).setObject(paramIndex++, param);
                    }
                    resultSet = ((PreparedStatement) statement).executeQuery();
                } else {
                    resultSet = statement.executeQuery(sql);
                }
                ResultSetMetaData metaData = resultSet.getMetaData();
                while (resultSet.next()) {
                    Map<String, Object> map = new HashMap<>(metaData.getColumnCount());
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        map.put(metaData.getColumnName(i), resultSet.getObject(metaData.getColumnName(i)));
                    }
                    result.add(map);
                }
            } catch (SQLException ex) {
                throw new USqlException(ex);
            } finally {
                try {
                    if (resultSet != null) {
                        resultSet.close();
                        statement.close();
                    }
                    if (!tx) {
                        conn.close();
                    }
                } catch (SQLException ex) {
                    logger.log(ERROR, ex);

                }
            }

            return result;
        }

        public long update(String sql, Object... params) {
            long updated;
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = conn.prepareStatement(sql);
                int paramIndex = 1;
                for (Object param : params) {
                    preparedStatement.setObject(paramIndex++, param);
                }
                updated = preparedStatement.executeUpdate();
            } catch (SQLException ex) {
                throw new USqlException(ex);
            } finally {
                try {
                    if (preparedStatement != null) {
                        preparedStatement.close();
                    }
                    if (!tx) {
                        conn.close();
                    }
                } catch (SQLException ex) {
                    logger.log(ERROR, ex);
                }
            }

            return updated;
        }

        public Connection getConn() {
            return conn;
        }
    }

    public <T> T query(QueryDef<T> queryDef) {
        try {
            return queryDef.execute(new Query(false));
        } catch (SQLException ex) {
            throw new USqlException(ex);
        }
    }

    public <T> T tx(TxDef<T> tTxDef) {
        return tx(tTxDef, e -> false);
    }

    public <T> T tx(TxDef<T> tTxDef, Predicate<Exception> rollback) {
        Query query = null;
        try {
            query = new Query(true);
            return tTxDef.execute(query);
        } catch (Exception ex1) {
            if (rollback.test(ex1)) {
                if (query != null) {
                    try {
                        query.conn.rollback();
                    } catch (SQLException ex2) {
                        logger.log(ERROR, ex2);
                    }
                }
            }
            throw new USqlException(ex1);
        } finally {
            try {
                if (query != null) {
                    query.conn.commit();
                    query.conn.close();
                }
            } catch (SQLException ex) {
                logger.log(ERROR, ex);
            }
        }
    }
}
