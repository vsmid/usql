package hr.yeti.usql;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SqlTest {

    Sql sql = new Sql("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "", "");

    @Test
    @Order(0)
    public void should_create_connection_using_datasource() throws SQLException {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        ds.setUser("");
        ds.setPassword("");

        Connection connection = new Sql(ds).connect();

        assertNotNull(connection);

        connection.close();
    }

    @Test
    @Order(1)
    public void should_create_connection_using_driver_manager() throws SQLException {
        Connection connection = sql.connect();

        assertNotNull(connection);

        connection.close();
    }

    @Test
    @Order(2)
    public void should_execute_update() throws SQLException {
        sql.query(query -> query.update("create table person(name varchar(255))"));
        Long inserted = sql.query(query -> query.update("insert into person(name) values(?)", "Leni"));

        assertEquals(1, inserted);
    }

    @Test
    @Order(3)
    public void should_execute_rows() throws SQLException {
        List<Map<String, Object>> row = sql.query(query -> query.rows("select name from person"));

        assertEquals("Leni", row.get(0).get("NAME"));
    }

    @Test
    @Order(4)
    public void should_execute_transaction() {
        List<Map<String, Object>> rows = sql.tx(query -> {
            query.update("insert into person(name) values(?)", "Leni");
            query.update("insert into person(name) values(?)", "Leni");
            return query.rows("select name from person");
        });

        assertEquals(3, rows.size());
    }

    @Test
    @Order(5)
    public void should_rollback_transaction() throws SQLException {
        assertThrows(TxException.class, () -> sql.tx(query -> {
            query.update("delete from person");
            query.update("insert into person(name) values(?)", "Leni");
            throw new RuntimeException();
        }, e -> true));

        List<Map<String, Object>> rows = sql.query(query -> query.rows("select name from person"));

        assertEquals(3, rows.size());
    }

    @Test
    @Order(6)
    public void should_use_custom_row_mapper() throws SQLException {
        List<Integer> rows = sql.query(query -> query.rows(
                "select name from person",
                row -> row.get("NAME").toString().length())
        );

        assertEquals(3, rows.size());
    }
}
