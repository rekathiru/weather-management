package com.crossover.trial.weather.enpoint;

import com.crossover.trial.weather.data.*;
import com.crossover.trial.weather.exception.WeatherUpdateException;
import com.crossover.trial.weather.service.WeatherCollectorService;
import com.google.gson.Gson;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A REST implementation of the WeatherCollector API. Accessible only to airport weather collection
 * sites via secure VPN.
 *
 * @author code test administrator
 */

@Path("/collect")
public class RestWeatherCollectorEndpoint implements WeatherCollectorEndpoint {
    public final static Logger LOGGER = Logger.getLogger(RestWeatherCollectorEndpoint.class.getName());

    /**
     * Health check for the endpoint
     *
     * @return 1 if the endpoint is alive functioning, 0 otherwise
     */
    @Override
    @GET
    @Path("/ping")
    public Response ping() {
        return Response.status(Response.Status.OK).entity("ready").build();
    }

    /**
     * Update the airports atmospheric information for a particular pointType with
     * json formatted data point information.
     *
     * @param iataCode      the 3 letter airport code
     * @param pointType     the point type, {@link DataPointType} for a complete list
     * @param datapointJson a json dict containing mean, first, second, thrid and count keys
     * @return HTTP Response code
     */
    @Override
    @POST
    @Path("/weather/{iata}/{pointType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWeather(@PathParam("iata") String iataCode,
                                  @PathParam("pointType") String pointType,
                                  String datapointJson) {

        if (iataCode == null || iataCode.length() != 3 || pointType == null || datapointJson == null) {
            LOGGER.log(Level.SEVERE, "Bad parameters iataCode [" + iataCode + ", ] pointType [" + pointType +
                    ",] datapoint [" + datapointJson + "]");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        //Checking whether atmospheric information exists or not to avoid further processing
        AtmosphericInformation atmInfo = AirportDataHolder.getInstance().findAtmosphericInformation(iataCode);
        if (atmInfo == null) {
            LOGGER.log(Level.SEVERE, "Atmospheric Information not found for the airport iataCode [" + iataCode + "]");
            return Response.status(Response.Status.NOT_FOUND).entity("Atmospheric Information not found for the " +
                    "airport iataCode [ " + iataCode + "], Please add the airport information.").build();
        }
        Gson gson = new Gson();
        DataPoint dataPoint = gson.fromJson(datapointJson, DataPoint.class);
        if (dataPoint == null) {
            LOGGER.log(Level.SEVERE, "Bad parameters for iataCode [ " + iataCode + "], " +
                    "pointType [ " + pointType + "], datapoint [" + datapointJson + "]");
            return Response.status(Response.Status.BAD_REQUEST).entity("Specify the data point correctly").build();
        }
        try {
            AtmosphericInformation updatedInfo = WeatherCollectorService.getInstance().
                    updateAtmosphericValues(atmInfo, pointType, dataPoint);
            boolean updated = AirportDataHolder.getInstance().updateAtmosphericInformation(iataCode, updatedInfo);
            if (!updated) {
                LOGGER.log(Level.SEVERE, "Atmospheric Information not found for the airport iataCode [ " + iataCode + "]");
                return Response.status(Response.Status.NOT_FOUND).entity("Atmospheric Information not found for the " +
                        "airport iataCode [ " + iataCode + "], Please add the airport information.").build();
            }
        } catch (WeatherUpdateException e) {
            LOGGER.log(Level.SEVERE, "Weather information cannot be updated due to pointType [ " + pointType + "] doesn't" +
                    " match with existing point types: TEMPERATURE, HUMIDTY, PRESSURE,CLOUDCOVER, PRECIPITATION ");
            return Response.status(Response.Status.BAD_REQUEST).entity("Weather information cannot be updated due " +
                    "to pointType pointType [ " + pointType + "] doesn't match with existing point types: " +
                    "TEMPERATURE, HUMIDTY, PRESSURE,CLOUDCOVER, PRECIPITATION, Please specify the correct point type. ").build();

        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Return a list of known airports as a json formatted list
     *
     * @return HTTP Response code and a json formatted list of IATA codes
     */
    @Override
    @GET
    @Path("/airports")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAirports() {
        Set<String> airports = WeatherCollectorService.getInstance().getAirports();
        return Response.status(Response.Status.OK).entity(airports).build();
    }

    /**
     * Retrieve airport data, including latitude and longitude for a particular airport
     *
     * @param iata the 3 letter airport code
     * @return an HTTP Response with a json representation of {@link AirportData}
     */
    @Override
    @GET
    @Path("/airport/{iata}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAirport(@PathParam("iata") String iata) {
        if (iata == null || iata.length() != 3) {
            LOGGER.log(Level.SEVERE, "Bad parameters iata [ " + iata + "]");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        AirportData airportData = AirportDataHolder.getInstance().findAirportData(iata);

        if (airportData == null) {
            LOGGER.log(Level.SEVERE, "Airport data not found for iata [" + iata + "] while retrieving.");
            return Response.status(Response.Status.NOT_FOUND).entity("Airport data not found for iata " +
                    "[ " + iata + "]").build();
        }
        return Response.status(Response.Status.OK).entity(airportData).build();
    }


    /**
     * Add a new airport to the known airport list.
     *
     * @param iata       the 3 letter airport code of the new airport
     * @param latString  the airport's latitude in degrees as a string [-90, 90]
     * @param longString the airport's longitude in degrees as a string [-180, 180]
     * @return HTTP Response code for the add operation
     */
    @Override
    @POST
    @Path("/airport/{iata}/{lat}/{long}")
    public Response addAirport(@PathParam("iata") String iata,
                               @PathParam("lat") String latString,
                               @PathParam("long") String longString) {
        if (iata == null || iata.length() != 3 || latString == null || longString == null) {
            LOGGER.log(Level.SEVERE, "Bad parameters iata [" + iata + "], latString [" + latString +
                    ",] longString [" + longString + "]");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Double latitude;
        Double longitude;
        try {
            latitude = Double.valueOf(latString);
            longitude = Double.valueOf(longString);
        } catch (NumberFormatException ex) {
            LOGGER.severe("Wrong airport coordinates latString [" + latString + ",] longString [" + longString + "]");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            LOGGER.severe("Wrong airport coordinates latString [" + latString + "], longString [" + longString + "]");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        AirportData airportData = new AirportData(iata, latitude, longitude);
        Boolean dataAdded = AirportDataHolder.getInstance().addAirportData(iata, airportData);
        if (!dataAdded) {
            return Response.status(Response.Status.CONFLICT).entity("Airport data already exists for: " + iata).build();
        }
        return Response.status(Response.Status.OK).build();
    }


    /**
     * Remove an airport from the known airport list
     *
     * @param iata the 3 letter airport code
     * @return HTTP Repsonse code for the delete operation
     */
    @Override
    @DELETE
    @Path("/airport/{iata}")
    public Response deleteAirport(@PathParam("iata") String iata) {
        if (iata == null || iata.length() != 3) {
            LOGGER.log(Level.SEVERE, "Bad parameters iataCode = " + iata);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        boolean dataRemoved = AirportDataHolder.getInstance().removeAirportData(iata);
        if (!dataRemoved) {
            LOGGER.log(Level.SEVERE, "Airport data not found for iata [" + iata + "] while deleting");
            return Response.status(Response.Status.NOT_FOUND).entity("Airport data not found for iata " +
                    "[" + iata + "]").build();
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * weather collect exit
     *
     * @return
     */
    @Override
    @GET
    @Path("/exit")
    public Response exit() {
        System.exit(0);
        return Response.noContent().build();
    }
}
