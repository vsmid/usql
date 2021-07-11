package hr.yeti.usql;

@FunctionalInterface
public interface QueryDef<T> {
    T execute(Sql.Query query);
}
