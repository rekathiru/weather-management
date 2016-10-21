package com.crossover.trial.weather.enpoint;

import com.crossover.trial.weather.data.AirportData;
import com.crossover.trial.weather.data.AirportDataHolder;
import com.crossover.trial.weather.data.AtmosphericInformation;
import com.crossover.trial.weather.service.WeatherQueryService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Weather App REST endpoint allows clients to query, update and check health stats. Currently, all data is
 * held in memory. The end point deploys to a single container
 *
 * @author code test administrator
 */
@Path("/query")
public class RestWeatherQueryEndpoint implements WeatherQueryEndpoint {

    public final static Logger LOGGER = Logger.getLogger("WeatherQuery");

    /**
     * Retrieve service health including total size of valid data points and request frequency information.
     *
     * @return health stats for the service as a string
     */
    @Override
    @GET
    @Path("/ping")
    public String ping() {
        return WeatherQueryService.getInstance().ping();
    }

    /**
     * Given a query in json format {'iata': CODE, 'radius': km} extracts the requested airport information and
     * return a list of matching atmosphere information.
     *
     * @param iata         the iataCode
     * @param radiusString the radius in km
     * @return a list of atmospheric information
     */
    @Override
    @GET
    @Path("/weather/{iata}/{radius}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response weather(@PathParam("iata") String iata,
                            @PathParam("radius") String radiusString) {
        if (iata == null) {
            LOGGER.log(Level.SEVERE, "Bad parameters iata");
            return Response.status(Response.Status.BAD_REQUEST).entity("Please specify the correct IATA code.").build();
        } else if (radiusString == null) {
            LOGGER.log(Level.SEVERE, "Bad parameters radius");
            return Response.status(Response.Status.BAD_REQUEST).entity("Please specify the correct Radius.").build();
        }
        AirportData data = AirportDataHolder.getInstance().findAirportData(iata);
        if (data == null) {
            LOGGER.log(Level.SEVERE, "Airport data could not be found for iata [" + iata + "]");
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Airport data not found for iata [" + iata + "]").build();
        }
        List<AtmosphericInformation> retval = WeatherQueryService.getInstance().weather(iata, radiusString);
        return Response.status(Response.Status.OK).entity(retval).build();
    }
}
