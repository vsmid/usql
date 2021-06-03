package hr.yeti.usql;

import java.util.Map;

@FunctionalInterface
public interface RowMapper<T> {
    T map(Map<String, Object> row);
}