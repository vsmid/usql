# µsql - zero dependency sql 

JDK 11+ compatible.

#### Showcase
```java
// Configure driver or datasource connectivity
Sql sql = new Sql("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "", "");

// Single query
List<Map<String, Object>> row = sql.query(q -> q.rows("select name from person"));

// Transaction
List<Map<String, Object>> rows = sql.tx(q -> {
            q.update("insert into person(name) values(?)", "Leni");
            q.update("insert into person(name) values(?)", "Leni");
            return q.rows("select name from person");
 });

// Configure transaction rollback depending on exception
sql.tx(q -> {
            q.update("delete from person");
            q.update("insert into person(name) values(?)", "Leni");
            throw new RuntimeException();
        }, e -> true) // Rollback always, on any exception
)

// Create repository and reuse methods (for both query and tx)
QueryDef findByName(String name) {
    return query -> query.rows("select * from person where name=?;", name);
}
sql.query(findByName("Leni"));

// Use custom row mapper
sql.query(query -> query.rows("select name from person", row -> row.get("NAME").toString().length()))

// Or just get connection and do everything manually
Connection conn = sql.connect(); 
```

#### Dependency (not yet published to Maven Central so build locally)
```xml
<dependency>
    <groupId>hr.yeti</groupId>
    <artifactId>usql</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Build
```shell script
git clone https://github.com/vsmid/usql.git
cd usql
mvn clean install
```