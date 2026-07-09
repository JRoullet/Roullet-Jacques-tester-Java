package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAOTest;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static com.parkit.parkingsystem.constants.ParkingType.CAR;
import static junit.framework.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAOTest ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() {
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

    private static Date rewindDate(int nbdays) {
        return new Date(System.currentTimeMillis() - (long) nbdays * 24 * 60 * 60 * 1000);
    }

    @Test
    public void testParkingLotExit() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        Ticket ticketBeforeExit = ticketDAO.getTicket("ABCDEF");
        ticketBeforeExit.setInTime(rewindDate(1));
        ticketDAO.updateInTime(ticketBeforeExit);
        assertNotNull(ticketBeforeExit);
        try{
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        }
        catch (Exception e){

        }
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
}
