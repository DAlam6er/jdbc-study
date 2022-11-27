package com.dmdev.jdbc.starter.dao;

import com.dmdev.jdbc.starter.dto.TicketFilter;
import com.dmdev.jdbc.starter.entity.Ticket;
import com.dmdev.jdbc.starter.exception.DaoException;
import com.dmdev.jdbc.starter.util.ConnectionPool;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

// синглтон, потокобезопасный, т.к. нет состояния
// класс не должен быть final, т.к. во фреймворках очень часто создают Proxy на эти классы
// в Spring достаточно было бы повесить аннотацию @Repository,
// тогда он бы за нас создал 1 instance данного класса
public class TicketDao implements Dao<Long, Ticket>
{
    private static final TicketDao INSTANCE = new TicketDao();
    private static final String DELETE_SQL = """
        DELETE FROM ticket
        WHERE id = ?
        """;
    private static final String SAVE_SQL = """
        INSERT INTO ticket(passenger_no, passenger_name, flight_id, seat_no, cost)
        VALUES (?, ?, ?, ?, ?);
        """;

    private static final String UPDATE_SQL = """
        UPDATE ticket
        SET passenger_no = ?,
            passenger_name = ?,
            flight_id = ?,
            seat_no = ?,
            cost = ?
        WHERE id = ?
        """;

    // + подхода - нужно выполнить всего один запрос для получения сущности flight
    // - подхода - в TicketDao нужно знать о том, как билдить сущность flight
    //             кроме того, flight сам содержит ссылки на другие сущности,
    //             т.е. является сложной сущностью,
    //             т.е. необходимо писать запрос с большим количеством JOIN
    private static final String FIND_ALL_SQL = """
        SELECT  ticket.id,
                passenger_no,
                passenger_name,
                flight_id,
                seat_no,
                cost,
                f.flight_no,
                f.departure_date,
                f.departure_airport_code,
                f.arrival_date,
                f.arrival_airport_code,
                f.aircraft_id,
                f.status
        FROM ticket
        JOIN flight f
        ON ticket.flight_id = f.id
        """;

    private static final String FIND_BY_ID_SQL = FIND_ALL_SQL + """
        WHERE ticket.id = ?
        """;

    private final FlightDao flightDao = FlightDao.getInstance();

    private TicketDao() {}

    // возвращаем сущность с установленным идентификатором
    // CREATE
    @Override
    public Ticket save(Ticket ticket)
    {
        try (var connection = ConnectionPool.get();
             var preparedStatement =
                 connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, ticket.getPassengerNo());
            preparedStatement.setString(2, ticket.getPassengerName());
            preparedStatement.setLong(3, ticket.getFlight().id());
            preparedStatement.setString(4, ticket.getSeatNo());
            preparedStatement.setBigDecimal(5, ticket.getCost());

            preparedStatement.executeUpdate();
            var generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                // устанавливаем id для сущности
                ticket.setId(generatedKeys.getLong("id"));
            }
            return ticket;
        } catch (SQLException ex) {
            throw new DaoException(ex);
        }
    }

    // READ
    // сущность может быть и не найдена
    @Override
    public Optional<Ticket> findById(Long id)
    {
        try (var connection = ConnectionPool.get();
             var preparedStatement =
                 connection.prepareStatement(FIND_BY_ID_SQL))
        {
            preparedStatement.setLong(1, id);

            var resultSet = preparedStatement.executeQuery();
            Ticket ticket = null;
            if (resultSet.next()) {
                ticket = buildTicket(resultSet);
            }
            return Optional.ofNullable(ticket);
        } catch (SQLException ex) {
            throw new DaoException(ex);
        }
    }

    private Ticket buildTicket(ResultSet resultSet) throws SQLException
    {
        // Вариант 1 - с использованием JOIN
        /*
        var flight = new Flight(
            resultSet.getLong("flight_id"),
            resultSet.getString("flight_no"),
            resultSet.getTimestamp("departure_date").toLocalDateTime(),
            resultSet.getString("departure_airport_code"),
            resultSet.getTimestamp("arrival_date").toLocalDateTime(),
            resultSet.getString("arrival_airport_code"),
            resultSet.getInt("aircraft_id"),
            resultSet.getString("status")
        );
        return new Ticket(
            resultSet.getLong("id"),
            resultSet.getString("passenger_no"),
            resultSet.getString("passenger_name"),
            flight,
            resultSet.getString("seat_no"),
            resultSet.getBigDecimal("cost")
        );
         */
        // Вариант 2 - с использованием отдельного FlightDao
        // более гибкий, но более медленный
        /*
        return new Ticket(
            resultSet.getLong("id"),
            resultSet.getString("passenger_no"),
            resultSet.getString("passenger_name"),
            flightDao.findById(resultSet.getLong("flight_id"))
                .orElse(null),
            resultSet.getString("seat_no"),
            resultSet.getBigDecimal("cost")
        );
         */
        // Вариант 3 - не создавая отдельный Connection для получения объекта Flight:
        // Optional<Flight> findById(Long id, Connection connection)
        return new Ticket(
            resultSet.getLong("id"),
            resultSet.getString("passenger_no"),
            resultSet.getString("passenger_name"),
            flightDao.findById(resultSet.getLong("flight_id"),
                    resultSet.getStatement().getConnection())
                .orElse(null),
            resultSet.getString("seat_no"),
            resultSet.getBigDecimal("cost")
        );
    }

    public List<Ticket> findAll(TicketFilter filter)
    {
        List<Object> parameters = new ArrayList<>();
        List<String> whereSql = new ArrayList<>();

        // таких if может быть много, поэтому вместо них можно использовать библиотеку Querydsl
        // которая может работать как с Hibernate так и со схемой БД
        // и которая генерирует кучу классов, с помощью которых можно динамически строить WHERE условия
        if (filter.seatNo() != null) {
            whereSql.add("seat_no LIKE ?");
            // если используем в фильтре оператор LIKE с префиксом и постфиксом
            parameters.add("%" + filter.seatNo() + "%");
        }
        if (filter.passengerName() != null) {
            whereSql.add("passenger_name = ?");
            parameters.add(filter.passengerName());
        }

        // количество элементов на странице
        parameters.add(filter.limit());
        // на какой странице находимся
        parameters.add(filter.offset());

        var where = whereSql.stream()
            .collect(joining(" AND ", " WHERE ", " LIMIT ? OFFSET ?"));

        // Динамический фильтр
        var sql = FIND_ALL_SQL + (whereSql.isEmpty() ? "LIMIT ? OFFSET ?" : where);
        try (var connection = ConnectionPool.get();
             var preparedStatement =
                 connection.prepareStatement(sql))
        {
            for (int i = 0; i < parameters.size(); i++) {
                preparedStatement.setObject(i + 1, parameters.get(i));
            }
            var resultSet = preparedStatement.executeQuery();
            List<Ticket> tickets = new ArrayList<>();
            while (resultSet.next()) {
                tickets.add(buildTicket(resultSet));
            }
            return tickets;
        } catch (SQLException e) {
            throw new DaoException(e);
        }
    }

    // на практике используется только для справочных таблиц
    @Override
    public List<Ticket> findAll()
    {
        try (var connection = ConnectionPool.get();
             var preparedStatement =
                 connection.prepareStatement(FIND_ALL_SQL))
        {
            var resultSet = preparedStatement.executeQuery();
            List<Ticket> tickets = new ArrayList<>();
            while (resultSet.next()) {
                tickets.add(buildTicket(resultSet));
            }
            return tickets;
        } catch (SQLException ex) {
            throw new DaoException(ex);
        }
    }

    // UPDATE
    @Override
    public void update(Ticket ticket)
    {
        try (var connection = ConnectionPool.get();
             var preparedStatement =
                 connection.prepareStatement(UPDATE_SQL)) {
            preparedStatement.setString(1, ticket.getPassengerNo());
            preparedStatement.setString(2, ticket.getPassengerName());
            preparedStatement.setLong(3, ticket.getFlight().id());
            preparedStatement.setString(4, ticket.getSeatNo());
            preparedStatement.setBigDecimal(5, ticket.getCost());
            preparedStatement.setLong(6, ticket.getId());

            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException(ex);
        }
    }

    // удаляем одну сущность, используя id из Ticket
    // DELETE
    @Override
    public boolean delete(Long id)
    {
        try (var connection = ConnectionPool.get();
             var preparedStatement = connection.prepareStatement(DELETE_SQL)) {
            preparedStatement.setLong(1, id);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DaoException(ex);
        }
    }

    public static TicketDao getInstance()
    {
        return INSTANCE;
    }
}
