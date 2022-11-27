package com.dmdev.jdbc.starter.util;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class ConnectionPool
{
    private static final String PASSWORD_KEY = "db.password";
    private static final String USERNAME_KEY = "db.username";
    private static final String URL_KEY = "db.url";
    private static final String DRIVER_KEY = "db.driver";
    private static final Integer DEFAULT_POOL_SIZE = 10;
    private static final String POOL_SIZE_KEY = "db.pool.size";

    private static BlockingQueue<Connection> pool;
    // коллекция для хранения исходных соединений
    private static List<Connection> sourceConnections;

    static {
        loadDriver();
        initConnectionPool();
    }

    private ConnectionPool() {}

    private static void loadDriver()
    {
        try {
            Class.forName(PropertiesUtil.get(DRIVER_KEY));
        } catch (ClassNotFoundException e) {
            // обязаны пробросить исключение, чтобы прекратить дальнейшее выполнение
            throw new RuntimeException(e);
        }
    }

    private static void initConnectionPool()
    {
        var poolSize = PropertiesUtil.get(POOL_SIZE_KEY);
        // если poolSize не установился
        var size = poolSize == null ? DEFAULT_POOL_SIZE : Integer.parseInt(poolSize);
        pool = new ArrayBlockingQueue<>(size);
        sourceConnections = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            var connection = open();
            var proxyConnection = (Connection)
                Proxy.newProxyInstance(
                ConnectionPool.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> method.getName().equals("close")
                    ? pool.add((Connection)proxy)
                    : method.invoke(connection, args));

            pool.add(proxyConnection);
            sourceConnections.add(connection);
        }
    }

    public static Connection get()
    {
        // извлекает соединение из головы очереди, если оно есть
        // если пул пустой, то ждёт пока элемент не станет доступным
        try {
            return pool.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Connection open()
    {
        try {
            return DriverManager.getConnection(
                PropertiesUtil.get(URL_KEY),
                PropertiesUtil.get(USERNAME_KEY),
                PropertiesUtil.get(PASSWORD_KEY)
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void close()
    {
        // Вызов метода close() у Proxy возвращает в пул, а не закрывает его
        try {
            for (Connection sourceConnection : sourceConnections) {
                sourceConnection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
