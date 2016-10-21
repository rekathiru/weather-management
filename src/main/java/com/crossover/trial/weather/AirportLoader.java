package com.crossover.trial.weather;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple airport loader which reads a file from disk and sends entries to the webservice
 *
 * @author code test administrator
 */
public class AirportLoader {

    /**
     * end point to supply updates
     */
    private WebTarget collect;
    private String filePath;

    public AirportLoader(String baseURL, String filePath) {
        Client client = ClientBuilder.newClient();
        collect = client.target(baseURL + "/collect");
        this.filePath = filePath;
    }

    public static void main(String args[]) throws IOException {
        String host = args != null && args.length >= 1 && args[0] != null ? args[0] : "localhost";
        String port = args != null && args.length >= 2 && args[1] != null ? args[1] : "9090";
        String airportsDataFilePath = args != null && args.length >= 3 && args[3] != null ? args[3] : null;
        String baseURL = "http://" + host + ":" + port;

        AirportLoader al = new AirportLoader(baseURL, airportsDataFilePath);
        al.upload();
        System.exit(0);
    }

    public void upload() {
        if (filePath == null) {
            filePath = AirportLoader.class.getResource(File.separator).getPath() + "airports.dat";
        }

        File airportDataFile = new File(filePath);
        if (!airportDataFile.exists() || airportDataFile.length() == 0) {
            System.err.println(airportDataFile + " is not a valid input");
            System.exit(1);
        }
        String sCurrentLine;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader((new FileInputStream(airportDataFile))));
            while ((sCurrentLine = reader.readLine()) != null) {
                String updatedLine = sCurrentLine.replace("\"", "");
                String[] elements = updatedLine.split(",");
                String iata = elements[4];
                String latString = elements[6];
                String longString = elements[7];
                        WebTarget path = collect.path("/airport/" + iata + "/" + latString + "/" + longString);
                        Response post = path.request().post(Entity.entity("", "application/json"));
                        System.out.println("collector.addAirportData: " + iata + " " + post.getStatus());
            }
        } catch (FileNotFoundException e) {
            System.err.println(filePath + " is not a valid input. Please specify the correct file path for airports.dat");
        } catch (IOException e) {
            System.err.println(" Error while reading the file " + filePath);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                System.err.println(" Error while Closing the Buffer Reader for the file " + filePath);
            }
        }

    }
}
