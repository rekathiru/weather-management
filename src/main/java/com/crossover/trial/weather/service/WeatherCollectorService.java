package com.crossover.trial.weather.service;

import com.crossover.trial.weather.data.AirportDataHolder;
import com.crossover.trial.weather.data.AtmosphericInformation;
import com.crossover.trial.weather.data.DataPoint;
import com.crossover.trial.weather.data.DataPointType;
import com.crossover.trial.weather.exception.WeatherUpdateException;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to handle weather query operations
 * <p>
 * * @author code test administrator
 */
public class WeatherCollectorService {
    public final static Logger LOGGER = Logger.getLogger(WeatherCollectorService.class.getName());
    private static volatile WeatherCollectorService weatherCollectorService;

    public static WeatherCollectorService getInstance() {
        if (weatherCollectorService == null) {
            synchronized (WeatherCollectorService.class) {
                if (weatherCollectorService == null) {
                    weatherCollectorService = new WeatherCollectorService();
                }
            }
        }
        return weatherCollectorService;
    }

    /**
     * Retrieve all the existing aiport information
     *
     * @return existing airport data
     */
    public Set<String> getAirports() {
        Set<String> airports = new HashSet<>();
        AirportDataHolder.getInstance().
                getAirportDataMap().
                forEach(airport -> airports.add(airport.getIata()));
        return airports;
    }

    /**
     * update atmospheric information with the given data point for the given point type
     *
     * @param atmosphericInformation the atmospheric information object to update
     * @param pointType              the data point type as a string
     * @param dp                     the actual data point
     */
    public AtmosphericInformation updateAtmosphericValues(AtmosphericInformation atmosphericInformation,
                                                          String pointType, DataPoint dp) throws WeatherUpdateException {

        if (pointType.equalsIgnoreCase(DataPointType.WIND.name())) {
            if (dp.getMean() >= 0) {
                atmosphericInformation.setWind(dp);
                atmosphericInformation.setLastUpdateTime(System.currentTimeMillis());
            }
        } else if (pointType.equalsIgnoreCase(DataPointType.TEMPERATURE.name())) {
            if (dp.getMean() >= -50 && dp.getMean() < 100) {
                atmosphericInformation.setTemperature(dp);
                atmosphericInformation.setLastUpdateTime(System.currentTimeMillis());
            }
        } else if (pointType.equalsIgnoreCase(DataPointType.HUMIDTY.name())) {
            if (dp.getMean() >= 0 && dp.getMean() < 100) {
                atmosphericInformation.setHumidity(dp);
                atmosphericInformation.setLastUpdateTime(System.currentTimeMillis());
            }
        } else if (pointType.equalsIgnoreCase(DataPointType.PRESSURE.name())) {
            if (dp.getMean() >= 650 && dp.getMean() < 800) {
                atmosphericInformation.setPressure(dp);
                atmosphericInformation.setLastUpdateTime(System.currentTimeMillis());
            }
        } else if (pointType.equalsIgnoreCase(DataPointType.CLOUDCOVER.name())) {
            if (dp.getMean() >= 0 && dp.getMean() < 100) {
                atmosphericInformation.setCloudCover(dp);
                atmosphericInformation.setLastUpdateTime(System.currentTimeMillis());
            }
        } else if (pointType.equalsIgnoreCase(DataPointType.PRECIPITATION.name())) {
            if (dp.getMean() >= 0 && dp.getMean() < 100) {
                atmosphericInformation.setPrecipitation(dp);
                atmosphericInformation.setLastUpdateTime(System.currentTimeMillis());
            }
        } else {
            LOGGER.log(Level.SEVERE, "Weather information cannot be updated due to [pointType] " + pointType + " doesn't" +
                    " match with existing point types: TEMPERATURE, HUMIDTY, PRESSURE,CLOUDCOVER, PRECIPITATION ");
            throw new WeatherUpdateException("couldn't update atmospheric data");
        }
        return atmosphericInformation;
    }
}
