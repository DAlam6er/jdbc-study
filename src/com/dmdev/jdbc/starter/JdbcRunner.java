package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.util.ConnectionPool;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcRunner
{
    public static void main(String[] args) throws SQLException
    {
        //testSQLOperations();
        //testSQLInjection();

        Long flightId = 2L;
        var result = getPreparedTicketsByFlightId(flightId);
        System.out.println(result);

        result = getFlightsBetween(
            LocalDate.of(2020, 1, 1).atStartOfDay(),
            LocalDateTime.now());
        System.out.println(result);

        try {
            checkMetaData();
        } finally {
            ConnectionPool.close();
        }
    }

    private static void checkMetaData() throws SQLException
    {
        try (var connection = ConnectionPool.get())
        {
            var metaData = connection.getMetaData();
            var catalogs = metaData.getCatalogs();

            while (catalogs.next()) {
                System.out.println("Список репозиториев(каталогов) БД:");
                var catalog = catalogs.getString("TABLE_CAT");
                System.out.println("\t" + catalog);
                System.out.println("-----------------------------------------");

                var schemas = metaData.getSchemas();
                while (schemas.next()) {
                    System.out.print("\nСписок \"Схема\"-\"Каталог\" ");
                    var schema = schemas.getString("TABLE_SCHEM");
                    System.out.print(schema);
                    System.out.println("\t" + schemas.getString("TABLE_CATALOG"));
                    System.out.println("-----------------------------------------");
                    // Все таблицы из всех схем
                    //var tables = metaData.getTables(null, null, "%", null);

                    // Все типы, включая индексы, последовательности
                    //var tables = metaData.getTables(catalog, schema, "%", null);

                    // Только таблицы
                    var tables = metaData.getTables(
                        catalog, schema,
                        "%", new String[]{"TABLE", "SYSTEM TABLE"});
                    System.out.println("Список \"Таблица\"-\"Схема\"");
                    while (tables.next()) {
                        System.out.print("\t" + tables.getString("TABLE_NAME"));
                        System.out.println("\t" + tables.getString("TABLE_SCHEM"));
                    }

                    System.out.println("Список \"Таблица\"-\"Колонка\"");
                    var columns = metaData.getColumns(
                        catalog, schema, null, "%");
                    while (columns.next()) {
                        System.out.print("\t" + columns.getString("TABLE_NAME"));
                        System.out.println("\t" + columns.getString("COLUMN_NAME"));
                    }
                    System.out.println("-----------------------------------------");
                }
            }
        }
    }

    private static List<Long> getFlightsBetween(LocalDateTime start, LocalDateTime end)
        throws SQLException
    {
        String sql = """
            SELECT id
            FROM flight
            WHERE departure_date BETWEEN ? AND ?
        """;

        List<Long> result = new ArrayList<>();
        try (var connection = ConnectionPool.get();
             var preparedStatement = connection.prepareStatement(sql))
        {
            preparedStatement.setFetchSize(50);
            preparedStatement.setQueryTimeout(10);
            preparedStatement.setMaxRows(100);

            System.out.println(preparedStatement);
            preparedStatement.setTimestamp(1, Timestamp.valueOf(start));
            System.out.println(preparedStatement);
            preparedStatement.setTimestamp(2, Timestamp.valueOf(end));
            System.out.println(preparedStatement);

            var resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                // чтобы избежать лишнего boxing/unboxing, лучше использовать
                // resultSet.getObject() вместо resultSet.getLong()
                result.add(resultSet.getObject("id", Long.class));
            }
        }
        return result;
    }

    private static void testSQLInjection() throws SQLException
    {
        String flightId = "2";
        var result = getTicketsByFlightId(flightId);
        System.out.println(result);

        // пример SQL-инъекции
        //String flightIdInjected = "2 OR '' = ''";
        // еще более опасная SQL-инъекция
        //String flightIdInjected = "2 OR 1 = 1; DROP TABLE info;";
        String flightIdInjected = "2 OR 1 = 1";
        result = getTicketsByFlightId(flightIdInjected);
        System.out.println(result);
    }

    private static void testSQLOperations() throws SQLException
    {
        // используем text blocks из java 15
        // DDL-оператор
        String sql1 = """
                CREATE TABLE IF NOT EXISTS info (
                id SERIAL PRIMARY KEY ,
                data TEXT NOT NULL
            );
            """;

        // DML-оператор
        String sql2 = """
            INSERT INTO info (data)
            VALUES
            ('Test1'),
            ('Test2'),
            ('Test3'),
            ('Test4');
        """;

        String sql3 = """
            UPDATE info
            SET data = 'TestTest'
            WHERE id = 5;
        """;

        String sql4 = """
            SELECT *
            FROM ticket
        """;

        String sql5 = """
            INSERT INTO info (data)
            VALUES
            ('autogenerated');
        """;

        try (var connection = ConnectionPool.get();
             var statement = connection.createStatement())
        {
            System.out.println(connection.getTransactionIsolation());

            // execute()
            /*
            var executeResult = statement.execute(sql1);
            System.out.println(executeResult);
            System.out.println(statement.getUpdateCount());
             */

            // executeUpdate()
            /*
            var executeResult = statement.executeUpdate(sql3);
            System.out.println(executeResult);
             */

            // executeQuery()
            /*
            var resultSet = statement.executeQuery(sql4);
            while (resultSet.next()) {
                System.out.println(resultSet.getLong("id"));
                System.out.println(resultSet.getString("passenger_no"));
                System.out.println(resultSet.getBigDecimal("cost"));
                System.out.println("-----");
            }
             */

            // getting auto generated Keys
            var executeUpdate = statement.executeUpdate(
                sql5, Statement.RETURN_GENERATED_KEYS);
            // курсор, который ходит по каждой из строк,
            // содержащей единственный столбец с идентификатором
            var generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                var generatedId = generatedKeys.getInt("id");
                System.out.println(generatedId);
            }
        }
    }

    private static List<Long> getTicketsByFlightId(String flightId) throws SQLException
    {
        String sql = """
            SELECT id
            FROM ticket
            WHERE flight_id = %s
        """.formatted(flightId);
        List<Long> result = new ArrayList<>();
        try (var connection = ConnectionPool.get();
             var statement = connection.createStatement())
        {
            var resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                // getLong может выбросить Exception, если поле будет Null,
                // т.к. getLong возвращает long (примитивный тип)
                //result.add(resultSet.getLong("id"));
                // NULL safe
                result.add(resultSet.getObject("id", Long.class));
            }
        }
        return result;
    }

    private static List<Long> getPreparedTicketsByFlightId(Long flightId) throws SQLException
    {
        String sql = """
            SELECT id
            FROM ticket
            WHERE flight_id = ?
        """;
        List<Long> result = new ArrayList<>();
        try (var connection = ConnectionPool.get();
             var preparedStatement = connection.prepareStatement(sql))
        {
            preparedStatement.setLong(1, flightId);
            var resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                result.add(resultSet.getLong("id"));
            }
        }
        return result;
    }
}
