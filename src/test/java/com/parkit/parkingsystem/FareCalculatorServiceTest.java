package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import java.util.Date;

//TODO tests d'intégration à faire via un conteneur docker (pour la BDD)

public class FareCalculatorServiceTest {

    private static final Logger log = LoggerFactory.getLogger(FareCalculatorServiceTest.class);
    private static final double DISCOUNT = 0.95;
    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;

    @BeforeAll
    public static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    public void setUpPerTest() {
        ticket = new Ticket();
    }

    @DisplayName("ETANT DONNE une voiture garée 1h, QUAND on calcule le tarif, ALORS le prix est celui d'une heure voiture")
    @Test
    public void calculateFareCar(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - ( 60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertThat(Fare.CAR_RATE_PER_HOUR).isEqualTo(ticket.getPrice());
    }

    @DisplayName("ETANT DONNE une voiture déjà venue, QUAND on calcule le tarif pour une heure de voiture, ALORS le prix est celui de 95% d'une heure de voiture")
    @Test
    public void calculateFareCarWithDiscount(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("123-CAR");

        fareCalculatorService.calculateFare(ticket, true);
        assertThat(Fare.CAR_RATE_PER_HOUR * DISCOUNT).isCloseTo(ticket.getPrice(), Percentage.withPercentage(1));
    }

    @DisplayName("ETANT DONNE une moto déjà venue, QUAND on calcule le tarif pour une heure de moto, ALORS le prix est celui de 95% d'une heure de moto")
    @Test
    public void calculateFareBikeWithDiscount(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("123-BIKE");

        fareCalculatorService.calculateFare(ticket, true);
        assertThat(Fare.BIKE_RATE_PER_HOUR * DISCOUNT).isEqualTo(ticket.getPrice());
    }

    @DisplayName("ETANT DONNE une moto garée 1h, QUAND on calcule le tarif, ALORS le prix est celui d'une heure moto")
    @Test
    public void calculateFareBike(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertThat(Fare.BIKE_RATE_PER_HOUR).isEqualTo(ticket.getPrice());
    }

    @DisplayName("ETANT DONNE un véhicule de type inconnu, QUAND on calcule le tarif, ALORS une erreur est levée")
    @Test
    public void calculateFareUnkownType(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, null,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        assertThatNullPointerException().isThrownBy(()->fareCalculatorService.calculateFare(ticket));
    }

    @DisplayName("ETANT DONNE une moto avec une heure d'entrée dans le futur, QUAND on calcule le tarif, ALORS une erreur est levée")
    @Test
    public void calculateFareBikeWithFutureInTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() + (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> fareCalculatorService.calculateFare(ticket)).withMessageContaining("Out time provided is incorrect:" + ticket.getOutTime().toString());

    }

    @DisplayName("ETANT DONNE une moto garée 45 minutes, QUAND on calcule le tarif, ALORS le prix est 75% du tarif horaire moto")
    @Test
    public void calculateFareBikeWithLessThanOneHourParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  45 * 60 * 1000) );//45 minutes parking time should give 3/4th parking fare
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertThat(ticket.getPrice()).isEqualTo((0.75 * Fare.BIKE_RATE_PER_HOUR));
    }

    @DisplayName("ETANT DONNE une voiture garée 45 minutes, QUAND on calcule le tarif, ALORS le prix est 75% du tarif horaire voiture")
    @Test
    public void calculateFareCarWithLessThanOneHourParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  45 * 60 * 1000) );//45 minutes parking time should give 3/4th parking fare
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertThat(ticket.getPrice()).isEqualTo(Math.round(0.75 * Fare.CAR_RATE_PER_HOUR * 100.0)/100.0);
    }

    @DisplayName("ETANT DONNE une voiture garée plus de 24h, QUAND on calcule le tarif, ALORS le prix est 24 fois le tarif horaire voiture")
    @Test
    public void calculateFareCarWithMoreThanADayParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  24 * 60 * 60 * 1000) );//24 hours parking time should give 24 * parking fare per hour
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertThat(ticket.getPrice()).isEqualTo((24 * Fare.CAR_RATE_PER_HOUR));
    }

    @DisplayName("ETANT DONNE une voiture garée moins de 30 minutes, QUAND on calcule le tarif, ALORS le prix est 0")
    @Test
    public void calculateFareCarWithLessThan30minutesParkingTime(){
        Date inTime = new Date();
        inTime.setTime((long) (System.currentTimeMillis() - ( 0.49 * 60 * 60 * 1000)));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertThat(ticket.getPrice()).isEqualTo(0);
    }

    @DisplayName("ETANT DONNE une moto garée moins de 30 minutes, QUAND on calcule le tarif, ALORS le prix est 0")
    @Test
    public void calculateFareBikeWithLessThan30minutesParkingTime(){
        Date inTime = new Date();
        inTime.setTime((long) (System.currentTimeMillis() - ( 0.49 * 60 * 60 * 1000)));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertThat(ticket.getPrice()).isEqualTo(0);
    }
}
