package hr.yeti.usql;

import java.sql.SQLException;

@FunctionalInterface
public interface QueryDef<T> {
    T execute(Sql.Query query) throws SQLException;
}
