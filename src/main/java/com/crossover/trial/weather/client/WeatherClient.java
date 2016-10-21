package com.crossover.trial.weather.client;

import com.crossover.trial.weather.AirportLoader;
import com.crossover.trial.weather.data.DataPoint;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A reference implementation for the weather client. Consumers of the REST API can look at WeatherClient
 * to understand API semantics. This existing client populates the REST endpoint with dummy data useful for
 * testing.
 *
 * @author code test administrator
 */
public class WeatherClient {
    public final static Logger LOGGER = Logger.getLogger(WeatherClient.class.getName());
    private static final String BASE_URL = "http://localhost:9090";

    /**
     * end point for read queries
     */
    private WebTarget query;

    /**
     * end point to supply updates
     */
    private WebTarget collect;

    public WeatherClient() {
        Client client = ClientBuilder.newClient();
        query = client.target(BASE_URL + "/query");
        collect = client.target(BASE_URL + "/collect");
    }

    public static void main(String[] args) {
        WeatherClient wc = new WeatherClient();

        wc.pingCollect();
        wc.populate("wind", 0, 10, 6, 4, 20);
        wc.query("BOS");
        wc.query("JFK");
        wc.query("EWR");
        wc.query("LGA");
        wc.query("MMU");
        wc.pingQuery();

        System.out.print("complete");
        System.exit(0);
    }

    public void pingCollect() {
        LOGGER.log(Level.INFO, "collect.ping: " );
        WebTarget path = collect.path("/ping");
        Response response = path.request().get();
        LOGGER.log(Level.INFO, "collect.ping: " + response.readEntity(String.class) + "\n");
    }

    public void query(String iata) {
        LOGGER.log(Level.INFO, "query.retrieve: " );
        WebTarget path = query.path("/weather/" + iata + "/0");
        Response response = path.request().get();
        LOGGER.log(Level.INFO, "query." + iata + ".0: " + response.readEntity(String.class));
    }

    public void pingQuery() {
        LOGGER.log(Level.INFO, "query.ping: " );
        WebTarget path = query.path("/ping");
        Response response = path.request().get();
        LOGGER.log(Level.INFO, "query.ping: " + response.readEntity(String.class));
    }

    public void populate(String pointType, int first, int last, int mean, int median, int count) {
        LOGGER.log(Level.INFO, "collect.populate: " );

        WebTarget path = collect.path("/weather/BOS/" + pointType);
        DataPoint dp = new DataPoint.Builder()
                .withFirst(first).withLast(last).withMean(mean).withMedian(median).withCount(count)
                .build();
        Response response = path.request().post(Entity.entity(dp, "application/json"));
        LOGGER.log(Level.INFO, "query.ping: " + response.readEntity(String.class));
    }

    public void exit() {
        try {
            collect.path("/exit").request().get();
        } catch (Throwable t) {
            // swallow
        }
    }
}
