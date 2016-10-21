package com.crossover.trial.weather;

import com.crossover.trial.weather.enpoint.RestWeatherCollectorEndpoint;
import com.crossover.trial.weather.enpoint.RestWeatherQueryEndpoint;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.HttpServerProbe;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;


/**
 * This main method will be use by the automated functional grader. You shouldn't move this class or remove the
 * main method. You may change the implementation, but we encourage caution.
 *
 * @author code test administrator
 */
public class WeatherServer {

    /**
     * Main method of the wether server
     *
     * @param args host and the port of the server can be passed as arguements
     */
    public static void main(String[] args) {
        try {
            System.out.println("Starting Weather App local testing server......");
            String host = args != null && args.length >= 1 && args[0] != null ? args[0] : "localhost";
            String port = args != null && args.length >= 2 && args[1] != null ? args[1] : "9090";
            String baseURL = "http://" + host + ":" + port;

            System.out.println("Starting Weather App local testing server: " + baseURL);

            final ResourceConfig resourceConfig = new ResourceConfig();
            resourceConfig.register(RestWeatherCollectorEndpoint.class);
            resourceConfig.register(RestWeatherQueryEndpoint.class);

            HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(baseURL), resourceConfig, false);
            Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

            HttpServerProbe probe = new HttpServerProbe.Adapter() {
                public void onRequestReceiveEvent(HttpServerFilter filter, Connection connection, Request request) {
                    System.out.println(request.getRequestURI());
                }
            };
            server.getServerConfiguration().getMonitoringConfig().getWebServerConfig().addProbes(probe);

            // the autograder waits for this output before running automated tests, please don't remove it
            server.start();
            System.out.println(format("Weather Server started.\n url=%s\n", baseURL));

            // blocks until the process is terminated
            Thread.currentThread().join();
            server.shutdown();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(WeatherServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
