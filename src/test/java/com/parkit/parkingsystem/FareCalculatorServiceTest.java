package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

//TODO tests d'intégration à faire via un conteneur docker (pour la BDD)

public class FareCalculatorServiceTest {

    private static final Logger log = LoggerFactory.getLogger(FareCalculatorServiceTest.class);
    private static final double DISCOUNT = 0.95d;
    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;
    private ParkingSpot parkingSpot;

    @BeforeAll
    public static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    public void setUpPerTest() {
        ticket = new Ticket();

        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - ( 60 * 60 * 1000) );
        Date outTime = new Date();
        parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
    }

    @DisplayName("ETANT DONNE une voiture garée 1h, QUAND on calcule le tarif, ALORS le prix est celui d'une heure voiture")
    @Test
    public void calculateFareCar(){

        fareCalculatorService.calculateFare(ticket);
        assertThat(ticket.getPrice()).isEqualTo(Fare.CAR_RATE_PER_HOUR);
    }

    @DisplayName("ETANT DONNE le retour d'un prix calculé, QUAND on effectu l'arrondi, ALORS le prix retourné est arrondi à 3 décimales")
    @Test
    public void calculatePriceReturnsRoundedPrice(){

        double actual = 0.5096d;
        double expected = 0.510d;
        double notExpected = 0.509d;

        assertThat(FareCalculatorService.roundedPrice(actual)).isEqualTo(FareCalculatorService.roundedPrice(expected));
        assertThat(FareCalculatorService.roundedPrice(actual)).isEqualTo(expected);
        assertThat(FareCalculatorService.roundedPrice(actual)).isNotEqualTo(notExpected);
    }

    @DisplayName("ETANT DONNE une voiture déjà venue, QUAND on calcule le tarif pour une heure de voiture, ALORS le prix est celui de 95% d'une heure de voiture")
    @Test
    public void calculateFareCarWithDiscount(){

        ticket.setVehicleRegNumber("123-CAR");

        fareCalculatorService.calculateFare(ticket, true);

        double expected = Fare.CAR_RATE_PER_HOUR * DISCOUNT;
        assertThat(ticket.getPrice()).isEqualTo(FareCalculatorService.roundedPrice(expected));
    }


    @DisplayName("ETANT DONNE une moto déjà venue, QUAND on calcule le tarif pour une heure de moto, ALORS le prix est celui de 95% d'une heure de moto")
    @Test
    public void calculateFareBikeWithDiscount(){
        parkingSpot.setParkingType(ParkingType.BIKE);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("123-BIKE");

        fareCalculatorService.calculateFare(ticket, true);
        assertThat(ticket.getPrice()).isEqualTo(Fare.BIKE_RATE_PER_HOUR * DISCOUNT);
    }


    //TODO tester si l'utilisateur entrant est un habitué

    @DisplayName("ETANT DONNE une voiture déjà venue, QUAND on calcule le tarif pour une heure de voiture, ALORS le prix est celui de 95% d'une heure de voiture")
    @Test
    public void calculateFareCarWithoutDiscount(){

        ticket.setVehicleRegNumber("123-CAR");
        fareCalculatorService.calculateFare(ticket, false);

        double expected = Fare.CAR_RATE_PER_HOUR;
        assertThat(ticket.getPrice()).isEqualTo(FareCalculatorService.roundedPrice(expected));
    }




    @DisplayName("ETANT DONNE une moto garée 1h, QUAND on calcule le tarif, ALORS le prix est celui d'une heure moto")
    @Test
    public void calculateFareBike(){
        parkingSpot.setParkingType(ParkingType.BIKE);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertThat(ticket.getPrice()).isEqualTo(Fare.BIKE_RATE_PER_HOUR);
    }

    @DisplayName("ETANT DONNE un véhicule de type inconnu, QUAND on calcule le tarif, ALORS une erreur est levée")
    @Test
    public void calculateFareUnkownType(){
        parkingSpot.setParkingType(null);
        ticket.setParkingSpot(parkingSpot);
        assertThatNullPointerException().isThrownBy(()->fareCalculatorService.calculateFare(ticket));
    }

    @DisplayName("ETANT DONNE une moto avec une heure d'entrée dans le futur, QUAND on calcule le tarif, ALORS une erreur est levée")
    @Test
    public void calculateFareBikeWithFutureInTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() + (  60 * 60 * 1000) );

        parkingSpot.setParkingType(ParkingType.BIKE);
        ticket.setInTime(inTime);
        ticket.setParkingSpot(parkingSpot);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> fareCalculatorService.calculateFare(ticket)).withMessageContaining("Out time provided is incorrect:" + ticket.getOutTime().toString());

    }

    @DisplayName("ETANT DONNE une moto garée 45 minutes, QUAND on calcule le tarif, ALORS le prix est 75% du tarif horaire moto")
    @Test
    public void calculateFareBikeWithLessThanOneHourParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  45 * 60 * 1000) );//45 minutes parking time should give 3/4th parking fare

        parkingSpot.setParkingType(ParkingType.BIKE);

        ticket.setInTime(inTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertThat(ticket.getPrice()).isEqualTo((0.75d * Fare.BIKE_RATE_PER_HOUR));
    }

    @DisplayName("ETANT DONNE une voiture garée 45 minutes, QUAND on calcule le tarif, ALORS le prix est 75% du tarif horaire voiture")
    @Test
    public void calculateFareCarWithLessThanOneHourParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  45 * 60 * 1000) );//45 minutes parking time should give 3/4th parking fare

        ticket.setInTime(inTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);

        assertThat(ticket.getPrice()).isEqualTo((0.75d * Fare.CAR_RATE_PER_HOUR));
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
