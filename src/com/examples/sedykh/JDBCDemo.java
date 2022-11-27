package com.examples.sedykh;

// при работе с SQL в Java импорты должны быть только такими:
// Driver, DriverManager

// импорты из самого Connector запрещены!
// DriverManager сам найдёт то что нужно,
// код должен быть кросс-платформенным на случай смены базы данных
import java.sql.*;

public class JDBCDemo
{
    static final String DRIVER_NAME = "com.mysql.cj.jdbc.Driver";
    static final String CONNECTION_STRING =
        "jdbc:mysql://localhost:3306/?user=Anton&password=Lestat94"; //URL
    public static void main(String[] args)
    {
        /*
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while(drivers.hasMoreElements()) {
            System.out.println(drivers.nextElement());
        }
        */

        // 1. ЗАГРУЗИТЬ ДРАЙВЕР В ПАМЯТЬ
        try {
            // загружаем по имени, не используя импорт
            Class.forName(DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            System.out.println("Driver not found or wrong name!");
            return;
        }

        // объявляем снаружи, а не в try { } чтобы иметь возможность обратиться
        Connection conn = null;
        // 2. ПОДКЛЮЧЕНИЕ К СЕРВЕРУ БД
        try {
            conn = DriverManager.getConnection(CONNECTION_STRING);
        } catch (SQLException e) {
            System.out.println("Wrong connection: " + e.getMessage());
            return;
        }

        // 3. РАБОТАЕМ С БАЗОЙ
        try {
            // получим список каталогов на сервере БД
            ResultSet rs = conn.getMetaData().getCatalogs();
            while (rs.next()) {
                System.out.println(rs.getString("TABLE_CAT"));
            }

            Statement st = conn.createStatement();
            //st.executeUpdate("CREATE DATABASE db");
            st.executeUpdate("USE db");
            /*
            st.executeUpdate(
                "CREATE TABLE persons(name varchar(64), age int(3))");
            */
            /*
            st.executeUpdate(
                "INSERT INTO persons (name, age) VALUES ('Name21', 43)");
            */
            rs = st.executeQuery("SELECT * FROM persons ORDER BY name");
            while(rs.next()) {
                System.out.println(
                    "name: " + rs.getString("name") +
                    ", age: " + rs.getString("age"));
            }
        } catch(SQLException e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
}
