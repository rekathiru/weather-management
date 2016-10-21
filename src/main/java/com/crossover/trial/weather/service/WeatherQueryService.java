package com.crossover.trial.weather.service;

import com.crossover.trial.weather.data.AirportData;
import com.crossover.trial.weather.data.AirportDataHolder;
import com.crossover.trial.weather.data.AtmosphericInformation;
import com.google.gson.Gson;

import java.util.*;
import java.util.logging.Logger;

/**
 * Utility class to handle weather query operations
 * <p>
 * * @author code test administrator
 */
public class WeatherQueryService {
    public final static Logger LOGGER = Logger.getLogger(WeatherQueryService.class.getName());
    /**
     * earth radius in KM
     */
    public static final double R = 6372.8;

    /**
     * shared gson json to object factory
     */
    public static final Gson gson = new Gson();

    private static volatile WeatherQueryService weatherQueryService;

    AirportDataHolder dataHolder = AirportDataHolder.getInstance();

    public static WeatherQueryService getInstance() {
        if (weatherQueryService == null) {
            synchronized (WeatherQueryService.class) {
                if (weatherQueryService == null) {
                    weatherQueryService = new WeatherQueryService();
                }
            }
        }
        return weatherQueryService;
    }

    /**
     * Retrieve service health including total size of valid data points and request frequency information.
     *
     * @return health stats for the service as a string
     */
    public String ping() {
        Map<String, Object> retval = new HashMap<>();

        int datasize = 0;
        // we only count recent readings
        datasize = (int) (dataHolder.getAtmosphericInformationMap().
                stream().filter(ai -> (ai.getCloudCover() != null || ai.getHumidity() != null ||
                ai.getPrecipitation() != null || ai.getPressure() != null ||
                ai.getTemperature() != null || ai.getWind() != null) &&
                ai.getLastUpdateTime() > System.currentTimeMillis() - 86400000).count());


        retval.put("datasize", datasize);

        Map<String, Double> freq = new HashMap<>();
        Collection<AirportData> airportDatas = dataHolder.getAirportDataMap();
        Map<AirportData, Integer> requestFrequency = dataHolder.getRequestFrequencyMap();
        Map<Double, Integer> radiusFreq = dataHolder.getRadiusFreqMap();
        // fraction of queries
        for (AirportData data : airportDatas) {
            double frac = (double) requestFrequency.getOrDefault(data, 0) / requestFrequency.size();
            freq.put(data.getIata(), frac);
        }
        retval.put("iata_freq", freq);

        int m = radiusFreq.keySet().stream()
                .max(Double::compare)
                .orElse(1000.0).intValue() + 1;

        int[] hist = new int[m];
        for (Map.Entry<Double, Integer> e : radiusFreq.entrySet()) {
            int i = e.getKey().intValue() % 10;
            hist[i] += e.getValue();
        }
        retval.put("radius_freq", hist);

        return gson.toJson(retval);
    }

    /**
     * Given a query in json format {'iata': CODE, 'radius': km} extracts the requested airport information and
     * return a list of matching atmosphere information.
     *
     * @param iata         the iataCode
     * @param radiusString the radius in km
     * @return a list of atmospheric information
     */
    public List<AtmosphericInformation> weather(String iata, String radiusString) {
        double radius = radiusString == null || radiusString.trim().isEmpty() ? 0 : Double.valueOf(radiusString);
        updateRequestFrequency(iata, radius);

        List<AtmosphericInformation> retval = new ArrayList<>();
        retval.add(dataHolder.findAtmosphericInformation(iata));
        if (radius == 0) {
            return retval;
        } else {
            AirportData ad = dataHolder.findAirportData(iata);
            Collection<AirportData> airportDatas = dataHolder.getAirportDataMap();
            airportDatas.stream().filter(data -> calculateDistance(ad, data) <= radius).forEach(data -> {
                AtmosphericInformation ai = dataHolder.findAtmosphericInformation(data.getIata());
                if (ai.getCloudCover() != null || ai.getHumidity() != null || ai.getPrecipitation() != null
                        || ai.getPressure() != null || ai.getTemperature() != null || ai.getWind() != null) {
                    retval.add(ai);
                }
            });
        }
        return retval;
    }


    /**
     * Records information about how often requests are made
     *
     * @param iata   an iata code
     * @param radius query radius
     */
    public void updateRequestFrequency(String iata, Double radius) {
        AirportData airportData = dataHolder.findAirportData(iata);
        Map<AirportData, Integer> requestFrequency = dataHolder.getRequestFrequencyMap();
        Map<Double, Integer> radiusFreq = dataHolder.getRadiusFreqMap();

        requestFrequency.put(airportData, requestFrequency.getOrDefault(airportData, 0) + 1);
        radiusFreq.put(radius, radiusFreq.getOrDefault(radius, 0));
    }


    /**
     * Haversine distance between two airports.
     *
     * @param ad1 airport 1
     * @param ad2 airport 2
     * @return the distance in KM
     */
    public double calculateDistance(AirportData ad1, AirportData ad2) {
        double deltaLat = Math.toRadians(ad2.getLatitude() - ad1.getLatitude());
        double deltaLon = Math.toRadians(ad2.getLongitude() - ad1.getLongitude());
        double a = Math.pow(Math.sin(deltaLat / 2), 2) + Math.pow(Math.sin(deltaLon / 2), 2)
                * Math.cos(ad1.getLatitude()) * Math.cos(ad2.getLatitude());
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

}
