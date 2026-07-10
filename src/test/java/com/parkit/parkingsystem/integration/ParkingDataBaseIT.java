package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAOTest;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;

import static com.parkit.parkingsystem.constants.ParkingType.CAR;
import static junit.framework.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    //Instance du conteneur qui se lance et son setup
    @Container
    private static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test")
            .withUsername("root")
            .withPassword("rootroot")
            .withInitScript("Data.sql");


    private static DataBaseTestConfig dataBaseTestConfig;
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAOTest ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() {
        // Se réfère à mon instance de conteneur docker, c'est lui qui pointe sur le conteneur qui vient de demarrer
        // sinon la classe de config ne saurait pas sur quel port ecouter
        dataBaseTestConfig = new DataBaseTestConfig(
                mysqlContainer.getJdbcUrl(),
                mysqlContainer.getUsername(),
                mysqlContainer.getPassword()
        );
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAOTest();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown() {

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        int parkingSpot = parkingSpotDAO.getNextAvailableSlot(CAR);

        assertNotNull(ticket);
        assertEquals(0.0, ticket.getPrice());
        assertNotNull(ticket.getInTime());
        assertNull(ticket.getOutTime());
        assertEquals(1, ticket.getParkingSpot().getId());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertNotEquals(1, parkingSpot);

    }

    @Test
    public void testParkingLotExit() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        Ticket ticketBeforeExit = rewindTicketInTime(ticketDAO.getTicket("ABCDEF"), 5);

        assertNotNull(ticketBeforeExit);

        parkingService.processExitingVehicle();

        // Instead of applying DataBase changes with an embedded SQL request, I added a method to retrieve the ticket
        // (as our primary rule to retrieve the right ticket was reaching the one with: outTime = null)
        // This method was absent and is imo part of the primordial CRUD methods.
        // This method could be used for future implementations

        Ticket closedTicket = ticketDAO.getTicketById(ticketBeforeExit.getId());

        assertNotNull(closedTicket);
        assertNotNull(closedTicket.getOutTime());
        assertTrue(closedTicket.getPrice() >= 0);
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // 1er passage
        parkingService.processIncomingVehicle();
        rewindTicketInTime(ticketDAO.getTicket("ABCDEF"), 5);
        parkingService.processExitingVehicle();

        // 2e passage
        parkingService.processIncomingVehicle();
        Ticket secondTicket = ticketDAO.getTicket("ABCDEF");
        rewindTicketInTime(secondTicket, 2);
        parkingService.processExitingVehicle();

        Ticket closedTicket = ticketDAO.getTicketById(secondTicket.getId());

        assertNotNull(closedTicket);
        Assertions.assertThat(closedTicket.getPrice()).isEqualTo(2.85);
    }


    private static Date rewindHours(int nbHeures) {
        return new Date(System.currentTimeMillis() - (long) nbHeures * 60 * 60 * 1000);
    }

    private Ticket rewindTicketInTime(Ticket ticket, int nbHeures){
        ticket.setInTime(rewindHours(nbHeures));
        ticketDAO.updateInTime(ticket);
        return ticket;
    }
}
