package com.crossover.trial.weather;

import com.crossover.trial.weather.data.AirportData;
import com.crossover.trial.weather.data.AtmosphericInformation;
import com.crossover.trial.weather.data.DataPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class WeatherEndpointTest {
    public final static Logger LOGGER = Logger.getLogger(WeatherEndpointTest.class.getName());

    private static ExecutorService executor;

    private Gson gson = new Gson();

    private static DataPoint dataPoint;

    /**
     * end point for read queries
     */
    private static WebTarget query;

    /**
     * end point to supply updates
     */
    private static WebTarget collect;

    @BeforeClass
    public static void setUp() throws Exception {
        LOGGER.log(Level.INFO, "Setting up environment for testing....");

        //Starting the weather server
        executor = Executors.newSingleThreadExecutor();
        Runnable server = () -> WeatherServer.main(null);
        executor.execute(server);

        long portCheckTimeOut = 200000;
        long startTime = System.currentTimeMillis();
        boolean active = checkWeatherServiceActive();

        while (!active) {
            active = checkWeatherServiceActive();
            if (active) {
                LOGGER.log(Level.INFO, "Weather Server is ready to receive requests...");
                break;
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            if (duration > portCheckTimeOut) {
                System.out.println();
                LOGGER.log(Level.SEVERE, "Weather Server could not be started and timeout ["
                        + portCheckTimeOut + "] exceeded");
                assertFalse("Weather Server could not be started", true);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.log(Level.INFO, "Weather Server started validating thread got interrupted while sleeping...");
                assertFalse("Weather Server started validating thread got interrupted while sleeping...", true);
            }
        }

        //Loading the airports.dat with airport information
        String baseURL = "http://localhost:9090";
        AirportLoader airportLoader = new AirportLoader(baseURL, null);

        Client client = ClientBuilder.newClient();
        query = client.target(baseURL + "/query");
        collect = client.target(baseURL + "/collect");
        airportLoader.upload();
        dataPoint = new DataPoint.Builder()
                .withCount(10).withFirst(10).withMedian(20).withLast(30).withMean(22).build();
        WebTarget path = collect.path("/weather/BOS/" + "wind");
        Response response = path.request().post(Entity.entity(dataPoint, "application/json"));

        path = query.path("/weather/BOS/0");
        response = path.request().get();
        String responseString = response.readEntity(String.class);
        Type listType = new TypeToken<ArrayList<AtmosphericInformation>>() {
        }.getType();

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        List<AtmosphericInformation> ais = gson.fromJson(responseString, listType);
        assertEquals("Data point equality validation failed", ais.get(0).getWind(), dataPoint);


    }

    @Test
    public void testPing() throws Exception {

        WebTarget path = query.path("/ping");
        Response response = path.request().get();
        String responseString = response.readEntity(String.class);

        Type mapType = new TypeToken<HashMap<String, Object>>() {
        }.getType();

        Map<String, Object> pingResult = gson.fromJson(responseString, mapType);

        assertEquals("Ping result: data size didn't match with 1.0", 1.0, pingResult.get("datasize"));

        path = collect.path("/airports");
        response = path.request().get();
        responseString = response.readEntity(String.class);
        Type listType = new TypeToken<Set<String>>() {
        }.getType();

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        Set<String> airports = gson.fromJson(responseString, listType);
        assertEquals("Ping result: iata frequency didn't match with " + airports.size(),
                airports.size(), ((Map<String, Double>) pingResult.get("iata_freq")).entrySet().size());
    }

    @Test
    public void testGet() throws Exception {
        WebTarget path = query.path("/weather/BOS/0");
        Response response = path.request().get();
        String responseString = response.readEntity(String.class);

        Type listType = new TypeToken<ArrayList<AtmosphericInformation>>() {
        }.getType();

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        List<AtmosphericInformation> ais = gson.fromJson(responseString, listType);
        assertEquals("Data point of wind didn't match with existing data point", ais.get(0).getWind(), dataPoint);
    }

    @Test
    public void testCrudAirport() throws Exception {
        WebTarget path;
        Response response;
        String responseString;

        //delete airport test when airport not exists
        path = collect.path("/airport/AAA");
        response = path.request().delete();
        assertEquals("When deleting airport, if airport not exists, it should return " +
                        Response.Status.NOT_FOUND.getStatusCode(),
                Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        //Retrieve airport test when airport not exists
        path = collect.path("/airport/AAA");
        response = path.request().get();
        assertEquals("When retrieving airport, if airport not exists, it should return " +
                        Response.Status.NOT_FOUND.getStatusCode(),
                Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        //add airport test
        path = collect.path("/airport/AAA/45.09/-34.343");
        response = path.request().post(Entity.entity("", "application/json"));
        assertEquals("When adding, if no server errors, it should return ", Response.Status.OK.getStatusCode(),
                Response.Status.OK.getStatusCode(), response.getStatus());

        //Retrieve airport test when airport exists
        path = collect.path("/airport/AAA");
        response = path.request().get();
        responseString = response.readEntity(String.class);
        assertEquals("When retrieving airport, if airport exists, it should return " + Response.Status.OK.getStatusCode(),
                Response.Status.OK.getStatusCode(), response.getStatus());
        Type listType = new TypeToken<AirportData>() {
        }.getType();

        AirportData data = gson.fromJson(responseString, listType);
        assertEquals("Retrieved airport IATA doesn't match with AAA", "AAA", data.getIata());
        assertEquals("Retrieved airport latitude doesn't match with 45.09", 45.09, data.getLatitude(), 0.001);
        assertEquals("Retrieved airport longitude doesn't match with -34.343", -34.343, data.getLongitude(), 0.001);

        //If airport already exists
        path = collect.path("/airport/AAA/45.08/-35.343");
        response = path.request().post(Entity.entity("", "application/json"));
        assertEquals("When adding, if airport exists, it should return ", Response.Status.CONFLICT.getStatusCode(),
                Response.Status.CONFLICT.getStatusCode(), response.getStatus());

        //Delete airport test when airport exists
        path = collect.path("/airport/AAA");
        response = path.request().delete();
        assertEquals("When deleting airport, if airport exists, it should return " + Response.Status.OK.getStatusCode(),
                Response.Status.OK.getStatusCode(), response.getStatus());

        //Retrieve airport test when airport not exists after deletion
        path = collect.path("/airport/AAA");
        response = path.request().get();
        assertEquals("When retrieving airport, if airport not exists, it should return "
                        + Response.Status.NOT_FOUND.getStatusCode(),
                Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInputValidationCrudAirport() throws Exception {
        WebTarget path;
        Response response;

        //delete airport test when airport not exists
        path = collect.path("/airport/null");
        response = path.request().delete();
        assertEquals("When deleting airport, if IATA is null, it should return " +
                        Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        path = collect.path("/airport/JF");
        response = path.request().delete();
        assertEquals("When retrieving airport, if IATA is not valid, it should return " +
                        Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        //Retrieve airport test when airport not exists
        path = collect.path("/airport/null");
        response = path.request().get();
        assertEquals("When retrieving airport, if IATA is null, it should return " +
                        Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        path = collect.path("/airport/JF");
        response = path.request().get();
        assertEquals("When retrieving airport, if IATA is not valid, it should return " +
                        Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        //add airport test
        path = collect.path("/airport/null/45.09/-34.343");
        response = path.request().post(Entity.entity("", "application/json"));
        assertEquals("When adding, if IATA is null, it should return ", Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        path = collect.path("/airport/JF/45.09/-34.343");
        response = path.request().post(Entity.entity("", "application/json"));
        assertEquals("When adding, if IATA is not valid, it should return ", Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        //add airport test
        path = collect.path("/airport/PTH/null/-34.343");
        response = path.request().post(Entity.entity("", "application/json"));
        assertEquals("When adding, if IATA is null, it should return ", Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        //add airport test
        path = collect.path("/airport/POU/45.09/null");
        response = path.request().post(Entity.entity("", "application/json"));
        assertEquals("When adding, if IATA is null, it should return ", Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        //Retrieve airport test when airport exists
        path = collect.path("/airport/null");
        response = path.request().get();
        assertEquals("When retrieving airport, if IATA is null, it should return " + Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testValidationAtmosphericInformationUpdate() {
        WebTarget path;
        Response response;

        path = collect.path("/weather/JF/wind");
        response = path.request().post(Entity.entity(dataPoint, "application/json"));
        assertEquals("When retrieving airport, if IATA is not valid, it should return " +
                        Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        path = collect.path("/weather/null/wind");
        response = path.request().post(Entity.entity(dataPoint, "application/json"));
        assertEquals("When updating atmospheric information and IATA is null, it should return " +
                        Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        path = collect.path("/weather/JFK/null");
        response = path.request().post(Entity.entity(dataPoint, "application/json"));
        assertEquals("When updating atmospheric information and IATA is null, it should return " +
                        Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        path = collect.path("/weather/UYT/wind");
        response = path.request().post(Entity.entity(dataPoint, "application/json"));
        assertEquals("When updating atmospheric information and if airport not exists, it should return " +
                        Response.Status.NOT_FOUND.getStatusCode(),
                Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        path = collect.path("/weather/JFK/air");
        response = path.request().post(Entity.entity(dataPoint, "application/json"));
        assertEquals("When updating atmospheric information and if data point type doesn't match, it should return " +
                        Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetNearby() throws Exception {
        String responseString;
        WebTarget path;
        Response response;
        List<AtmosphericInformation> ais;
        Type listType;

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        // check datasize response
        path = collect.path("/weather/JFK/wind");
        response = path.request().post(Entity.entity(dataPoint, "application/json"));
        assertEquals("When updating atmospheric information and airport exists JFK, it should return " +
                        Response.Status.OK.getStatusCode(),
                Response.Status.OK.getStatusCode(), response.getStatus());

        double mean = dataPoint.getMean();
        dataPoint.setMean(40);
        path = collect.path("/weather/EWR/wind");
        response = path.request().post(Entity.entity(dataPoint, "application/json"));
        assertEquals("When updating atmospheric information and airport exists EWR, it should return " +
                        Response.Status.OK.getStatusCode(),
                Response.Status.OK.getStatusCode(), response.getStatus());

        path = query.path("/weather/EWR/0");
        response = path.request().get();
        responseString = response.readEntity(String.class);
        listType = new TypeToken<ArrayList<AtmosphericInformation>>() {
        }.getType();
        ais = gson.fromJson(responseString, listType);
        assertEquals("Data point of wind didn't match with existing data point after updated data " +
                "point with mean value for wind type", ais.get(0).getWind(), dataPoint);

        dataPoint.setMean(30);
        path = collect.path("/weather/LGA/wind");
        response = path.request().post(Entity.entity(dataPoint, "application/json"));
        assertEquals("When updating atmospheric information and if airport exists LGA, it should return " +
                        Response.Status.OK.getStatusCode(),
                Response.Status.OK.getStatusCode(), response.getStatus());

        path = query.path("/weather/LGA/0");
        response = path.request().get();
        responseString = response.readEntity(String.class);
        listType = new TypeToken<ArrayList<AtmosphericInformation>>() {
        }.getType();
        ais = gson.fromJson(responseString, listType);
        assertEquals("Data point of wind didn't match with existing data point after updated data " +
                "point with mean value", ais.get(0).getWind(), dataPoint);

        //Setting the data point with original value
        dataPoint.setMean(mean);

        path = query.path("/weather/JFK/200");
        response = path.request().get();
        responseString = response.readEntity(String.class);
        listType = new TypeToken<ArrayList<AtmosphericInformation>>() {
        }.getType();
        ais = gson.fromJson(responseString, listType);

        assertEquals("When retrieving nearby atmospheric information within 200 radium for JFK, the size should be 4",
                4, ais.size());
    }

    @Test
    public void testUpdate() throws Exception {
        String responseString;
        WebTarget path;
        Response response;
        List<AtmosphericInformation> ais;
        Type listType;

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        DataPoint windDp = new DataPoint.Builder()
                .withCount(10).withFirst(10).withMedian(20).withLast(30).withMean(22).build();
        path = collect.path("/weather/BOS/wind");
        response = path.request().post(Entity.entity(windDp, "application/json"));
        assertEquals("When updating atmospheric information and if airport exists, it should return " +
                        Response.Status.OK.getStatusCode(),
                Response.Status.OK.getStatusCode(), response.getStatus());


        path = query.path("/weather/BOS/0");
        response = path.request().get();
        responseString = response.readEntity(String.class);
        assertEquals("When retrieving atmospheric information for radius 0 and if airport exists, it should return " +
                        Response.Status.OK.getStatusCode(),
                Response.Status.OK.getStatusCode(), response.getStatus());
        listType = new TypeToken<ArrayList<AtmosphericInformation>>() {
        }.getType();
        ais = gson.fromJson(responseString, listType);
        assertEquals("Data point of wind didn't match with existing data point after updated data " +
                "point for BOS", ais.get(0).getWind(), dataPoint);

        path = query.path("/ping");
        response = path.request().get();
        responseString = response.readEntity(String.class);

        Type mapType = new TypeToken<HashMap<String, Object>>() {
        }.getType();

        Map<String, Object> pingResult = gson.fromJson(responseString, mapType);

        assertEquals(4.0, pingResult.get("datasize"));

        DataPoint cloudCoverDp = new DataPoint.Builder()
                .withCount(4).withFirst(10).withMedian(60).withLast(100).withMean(50).build();

        path = collect.path("/weather/BOS/cloudcover");
        response = path.request().post(Entity.entity(cloudCoverDp, "application/json"));
        assertEquals("When updating atmospheric information and airport exists BOS, it should return " +
                        Response.Status.OK.getStatusCode(),
                Response.Status.OK.getStatusCode(), response.getStatus());

        path = query.path("/weather/BOS/0");
        response = path.request().get();
        responseString = response.readEntity(String.class);

        Type atmType = new TypeToken<ArrayList<AtmosphericInformation>>() {
        }.getType();
        ais = gson.fromJson(responseString, atmType);
        assertEquals(ais.get(0).getWind(), windDp);
        assertEquals(ais.get(0).getCloudCover(), cloudCoverDp);
    }

    @AfterClass
    public static void shutdownServer() {
        //Stopping the server
        executor.shutdown();
    }

    private static boolean checkWeatherServiceActive() {
        Socket socket = null;
        String host = "localhost";
        int port = 9090;
        try {
            SocketAddress httpSockaddr = new InetSocketAddress(host, port);
            socket = new Socket();
            socket.connect(httpSockaddr, port);
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    return false;
                }
            }
        }
        return true;
    }

}