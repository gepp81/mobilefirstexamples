/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package ar.com.example;

import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import ar.com.example.dao.ExampleDAO;
import ar.com.example.dao.ExampleDAOImpl;

import com.ibm.json.java.JSONObject;
import com.worklight.adapters.rest.api.WLServerAPI;
import com.worklight.adapters.rest.api.WLServerAPIProvider;

@Path("/users")
public class ExampleAdapterResource {

  static Logger logger = Logger.getLogger(ExampleAdapterResource.class.getName());
  private static final String DEFAULT_LANGUAGE = "en_us";
  private static final String JDBC_MOBILEFIRST_TRAINING = "jdbc/mobilefirst_training";

  WLServerAPI api = WLServerAPIProvider.getWLServerAPI();

  private static DataSource ds = null;
  private static Context ctx = null;
  private static ExampleDAO dao = null;

  public static void init() throws NamingException {
    ctx = new InitialContext();
    ds = (DataSource) ctx.lookup(JDBC_MOBILEFIRST_TRAINING);
    dao = new ExampleDAOImpl(ds);
  }

  public static Response getResponse(final Object result, Status status) {
    return Response.status(status).entity(result).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/getAll")
  public Response getUserByDAO(@QueryParam(value = "language") String language) {
    try {
      language = StringUtils.isEmpty(language) ? DEFAULT_LANGUAGE : language;
      Response res = getResponse(dao.getAllUser(language), Status.OK);
      return res;
    } catch (Exception exception) {
      System.out.println(exception.getMessage());
      return getResponse(new JSONObject(), Status.INTERNAL_SERVER_ERROR);
    }
  }
}
