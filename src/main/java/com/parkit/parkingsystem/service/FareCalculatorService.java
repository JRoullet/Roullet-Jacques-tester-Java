package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    private static final double DISCOUNT = 0.95;
    private static final double NODISCOUNT = 1.0;

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, Boolean discount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        double inHour = ticket.getInTime().getTime();
        double outHour = ticket.getOutTime().getTime();
        double duration = (outHour - inHour) / (60 * 60 * 1000);
        double rate = discount ? DISCOUNT : NODISCOUNT;
        double carPrice = Math.round(duration * Fare.CAR_RATE_PER_HOUR * rate * 100.0) / 100.0;
        double bikePrice = Math.round(duration * Fare.BIKE_RATE_PER_HOUR * rate * 100.0) / 100.0;

        if (duration < 0.5) {
            ticket.setPrice(0);
        } else {
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    ticket.setPrice(carPrice);
                    break;
                }
                case BIKE: {
                    ticket.setPrice(bikePrice);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unkown Parking Type");
            }
        }
    }
}