package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.util.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionRunner {
  public static void main(String[] args) throws SQLException {
    long flightId = 7;
    var deleteFlightSql = "DELETE FROM flight WHERE id = ?";
    var deleteTicketsSql = "DELETE FROM ticket WHERE flight_id = ?";

    // При работе с транзакциями работать в блоке try-with-resources не получится
    Connection connection = null;
    PreparedStatement deleteFlightStatement = null;
    PreparedStatement deleteTicketsStatement = null;
    try {
      connection = ConnectionPool.get();
      deleteFlightStatement =
          connection.prepareStatement(deleteFlightSql);
      deleteTicketsStatement =
          connection.prepareStatement(deleteTicketsSql);
      // Отключаем автокоммит до выполнения любых запросов
      connection.setAutoCommit(false);

      deleteFlightStatement.setLong(1, flightId);
      deleteTicketsStatement.setLong(1, flightId);

      deleteTicketsStatement.executeUpdate();
      if (true) {
        throw new RuntimeException("Ooops");
      }
      deleteFlightStatement.executeUpdate();
      // фиксирование транзакции
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
      if (deleteFlightStatement != null) {
        deleteFlightStatement.close();
      }
      if (deleteTicketsStatement != null) {
        deleteTicketsStatement.close();
      }
    }
  }
}
