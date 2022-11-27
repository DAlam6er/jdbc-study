package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.util.ConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class BatchRunner
{
    public static void main(String[] args) throws SQLException
    {
        long flightId = 7;
        var deleteFlightSql = "DELETE FROM flight WHERE id = " + flightId;
        var deleteTicketsSql = "DELETE FROM ticket WHERE flight_id = " + flightId;

        // При работе с транзакциями работать в блоке try-with-resources не получится
        Connection connection = null;
        Statement statement = null;
        try
        {
            connection = ConnectionPool.get();
            connection.setAutoCommit(false);

            statement = connection.createStatement();
            statement.addBatch(deleteTicketsSql);
            statement.addBatch(deleteFlightSql);

            statement.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            if (connection != null) {
                // откатываем транзакцию
                connection.rollback();
            }
            throw ex;
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }
}
