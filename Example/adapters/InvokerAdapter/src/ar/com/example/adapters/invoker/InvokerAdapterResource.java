/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package ar.com.example.adapters.invoker;

import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import com.ibm.json.java.JSONObject;
import com.worklight.adapters.rest.api.WLServerAPI;
import com.worklight.adapters.rest.api.WLServerAPIProvider;

@Path("/call")
public class InvokerAdapterResource {

  static Logger logger = Logger.getLogger(InvokerAdapterResource.class.getName());
  private static final String EXAMPLE_USERS_GETALL = "/ExampleAdapter/users/getAll";

  WLServerAPI api = WLServerAPIProvider.getWLServerAPI();

  /**
   * 
   * @param result
   * @param status
   * @return
   */
  public static Response getResponse(final Object result, Status status) {
    return Response.status(status).entity(result).build();
  }

  /**
   * Call another adapter an return the result
   * 
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/callAdapter")
  public Response getUsers() {
    HttpUriRequest request = new HttpGet(EXAMPLE_USERS_GETALL);
    try {
      HttpResponse result = api.getAdaptersAPI().executeAdapterRequest(request);
      return getResponse(api.getAdaptersAPI().getResponseAsJSON(result), Status.OK);
    } catch (Exception exception) {
      return getResponse(new JSONObject(), Status.INTERNAL_SERVER_ERROR);
    }
  }

}
