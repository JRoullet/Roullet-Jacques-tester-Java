package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FareCalculatorService {

    private static final double DISCOUNT = 0.95d;
    private static final double NODISCOUNT = 1.0d;

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, Boolean discount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        double inHourTime = ticket.getInTime().getTime();
        double outHourTime = ticket.getOutTime().getTime();
        double duration = (outHourTime - inHourTime) / (60 * 60 * 1000);

        double rate = discount ? DISCOUNT : NODISCOUNT;
        double price = 0.0d;

        if (duration > 0.5d) {
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    price = roundedPrice(duration * Fare.CAR_RATE_PER_HOUR * rate);
                    break;
                }
                case BIKE: {
                    price = roundedPrice(duration * Fare.BIKE_RATE_PER_HOUR * rate);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unkown Parking Type");
            }
        }
        ticket.setPrice(price);
    }

    /**
     * Specific function to round prices
     * 3 digits to pass tests
     */
    private static double roundedPrice(double price){
        BigDecimal bd = BigDecimal.valueOf(price).setScale(3, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}