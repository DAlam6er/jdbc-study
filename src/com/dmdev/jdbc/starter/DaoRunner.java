package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.dao.TicketDao;
import com.dmdev.jdbc.starter.dto.TicketFilter;
import com.dmdev.jdbc.starter.entity.Ticket;

import java.math.BigDecimal;
import java.util.Optional;

public class DaoRunner {
  public static void main(String[] args) {
    //createTest();
    //System.out.println(readTest());
    //updateTest();
    //deleteTest();
    //findAllTest();
    //filterTest();

    // complex mapping test
    var maybeTicket = TicketDao.getInstance().findById(5L);
    System.out.println(maybeTicket);
  }

  private static void findAllTest() {
    for (Ticket ticket : TicketDao.getInstance().findAll()) {
      System.out.println(ticket);
    }
    System.out.println("--------------------------------------------");
  }

  private static void filterTest() {
    // findAll with filter test
    var ticketFilter = new TicketFilter(3, 2, null, "A1");
    for (Ticket ticket : TicketDao.getInstance().findAll(ticketFilter)) {
      System.out.println(ticket);
    }
  }

  private static void createTest() {
    var ticketDao = TicketDao.getInstance();
    var ticket = new Ticket();
    ticket.setPassengerNo("1234567");
    ticket.setPassengerName("Test");
    //ticket.setFlight(3L);
    ticket.setSeatNo("B3");
    ticket.setCost(BigDecimal.TEN);
    var savedTicket = ticketDao.save(ticket);
    System.out.println(savedTicket);
  }

  private static void updateTest() {
    var ticketDao = TicketDao.getInstance();
    var maybeTicket = readTest();
    maybeTicket.ifPresent(ticket -> {
      ticket.setCost(BigDecimal.valueOf(188.88));
      ticketDao.update(ticket);
    });
  }

  private static Optional<Ticket> readTest() {
    var ticketDao = TicketDao.getInstance();
    return ticketDao.findById(2L);
  }

  private static void deleteTest() {
    var ticketDao = TicketDao.getInstance();
    var deleteResult = ticketDao.delete(56L);
    System.out.println(deleteResult);
  }
}
