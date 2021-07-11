package hr.yeti.usql;

@FunctionalInterface
public interface TxDef<T> {
    T execute(Sql.Query query);
}