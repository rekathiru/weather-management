package com.crossover.trial.weather.data;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * The class will hold all the airport and weather related information
 *
 * @author code test adminsitrator
 */
public class AirportDataHolder {
    public final static Logger LOGGER = Logger.getLogger(AirportDataHolder.class.getName());

    private static volatile AirportDataHolder instance;

    /**
     * Internal map to store airport data against IATA code
     */
    private Map<String, AirportData> airportDataMap;

    /**
     * Internal map to store atmospheric information against IATA code
     */
    private Map<String, AtmosphericInformation> atmosphericInformationMap;

    /**
     * Internal performance counter to better understand most requested information, this map can be improved but
     * for now provides the basis for future performance optimizations. Due to the stateless deployment architecture
     * we don't want to write this to disk, but will pull it off using a REST request and aggregate with other
     * performance metrics
     */
    private Map<AirportData, Integer> requestFrequencyMap;

    /**
     * Internal map to store radius frequency details
     */
    private Map<Double, Integer> radiusFreqMap;

    private AirportDataHolder() {
        airportDataMap = new ConcurrentHashMap<>();
        atmosphericInformationMap = new ConcurrentHashMap<>();
        requestFrequencyMap = new ConcurrentHashMap<>();
        radiusFreqMap = new ConcurrentHashMap<>();
    }

    public static AirportDataHolder getInstance() {
        if (instance == null) {
            synchronized (AirportDataHolder.class) {
                if (instance == null) {
                    instance = new AirportDataHolder();
                }
            }
        }
        return instance;

    }


    /**
     * Get request frequency map
     *
     * @return requestFrequencyMap
     */
    public Map<AirportData, Integer> getRequestFrequencyMap() {
        return requestFrequencyMap;
    }

    /**
     * Get radius frequency map
     *
     * @return radiusFreqMap
     */
    public Map<Double, Integer> getRadiusFreqMap() {
        return radiusFreqMap;
    }

    /**
     * Get all the airport data from airportDataMap
     *
     * @return all known airports
     */
    public Collection<AirportData> getAirportDataMap() {
        return airportDataMap.values();
    }

    /**
     * atmospheric information for each airport, idx corresponds with airportDataMap
     *
     * @return all known atmospheric information
     */
    public Collection<AtmosphericInformation> getAllAtmosphericInformation() {
        return atmosphericInformationMap.values();
    }

    /**
     * Add airport data to concurrent hash map. Hence it will guarantee concurrency and atmoic operation
     * of checking whether key exists, and then add key.
     *
     * @param iataCode iata code of airport
     * @param data     airport data
     * @return whether airport data added or not
     */
    public boolean addAirportData(String iataCode, AirportData data) {
        AirportData airportDataExisting = airportDataMap.putIfAbsent(iataCode, data);
        if (airportDataExisting == null) {
            AtmosphericInformation atm = new AtmosphericInformation();
            atmosphericInformationMap.put(iataCode, atm);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add atmospheric information to concurrent hash map. Hence it will guarantee concurrency.
     * No need to check whether key exists or not as no call to this method whenever it exists
     *
     * @param iataCode    iata code of airport
     * @param information atmospheric information
     */
    public void addAtmosphericInformation(String iataCode, AtmosphericInformation information) {
        atmosphericInformationMap.put(iataCode, information);

    }

    /**
     * Given an iataCode find the airport data
     *
     * @param iataCode as a string
     * @return airport data or null if not found
     */
    public AirportData findAirportData(String iataCode) {
        return airportDataMap.get(iataCode);
    }

    /**
     * Given an iataCode find the airport data
     *
     * @param iataCode as a string
     * @return airport data or null if not found
     */
    public AtmosphericInformation findAtmosphericInformation(String iataCode) {
        return atmosphericInformationMap.get(iataCode);
    }

    /**
     * Get all the atmospheric information
     *
     * @return all the atmospheric information available
     */
    public Collection<AtmosphericInformation> getAtmosphericInformationMap() {
        return atmosphericInformationMap.values();
    }

    /**
     * Remove airport data from the concurrent hash map while ensuring atomicity of removing data from it
     *
     * @param iataCode IATA code of airport
     * @return whether data removed or not
     */
    public boolean removeAirportData(String iataCode) {
        AirportData data = airportDataMap.remove(iataCode);
        if (data == null) {
            return false;
        } else {
            atmosphericInformationMap.remove(iataCode);
            return true;
        }
    }

    /**
     * Update atmospheric information while ensuring atomicity of checking weather
     * atmospheric information exists and update the value
     *
     * @param iataCode               IATA code of airport
     * @param atmosphericInformation updated atmospheric information
     * @return whether atmospheric information updated or not
     */
    public boolean updateAtmosphericInformation(String iataCode, AtmosphericInformation atmosphericInformation) {
        AtmosphericInformation atmosphericInformationExisting =
                atmosphericInformationMap.replace(iataCode, atmosphericInformation);
        return atmosphericInformationExisting != null;
    }
}
