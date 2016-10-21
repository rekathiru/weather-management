package com.crossover.trial.weather.data;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Basic airport information.
 *
 * @author code test administrator
 */
public class AirportData {

    /**
     * the three letter IATA code
     */
    private String iata;

    /**
     * latitude value in degrees
     */
    private double latitude;

    /**
     * longitude value in degrees
     */
    private double longitude;

    public AirportData(String iata, double latitude, double longitude) {
        this.iata = iata;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getIata() {
        return iata;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }


    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }

    public boolean equals(Object other) {
        return other instanceof AirportData && ((AirportData) other).getIata().equals(this.getIata());

    }
}
