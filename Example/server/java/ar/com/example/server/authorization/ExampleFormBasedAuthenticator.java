package ar.com.example.server.authorization;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.ibm.json.java.JSONObject;
import com.worklight.common.util.FileTemplate;
import com.worklight.core.auth.ext.DeviceSSO;
import com.worklight.core.auth.ext.FormBasedAuthenticator;
import com.worklight.server.auth.api.AuthenticationResult;
import com.worklight.server.auth.api.AuthenticationStatus;
import com.worklight.server.auth.api.BadConfigurationOptionException;
import com.worklight.server.auth.api.MissingConfigurationOptionException;
import com.worklight.server.auth.api.UserIdentity;
import com.worklight.server.auth.api.UsernamePasswordAuthenticator;
import com.worklight.server.bundle.api.WorklightBundles;
import com.worklight.server.util.JSONUtils;

@DeviceSSO(supported = true)
public class ExampleFormBasedAuthenticator extends UsernamePasswordAuthenticator {

  private static final long serialVersionUID = 3184985432942202494L;
  private static final String DEFAULT_LOGIN_HTML_TEMPLATE = "login.html.template";
  private static final String PARAM_LOGIN_PAGE_PATH = "login-page";
  private static final String PARAM_AUTH_REDIRECT_URL = "auth-redirect";
  private static final String SUBMIT_PATH = "submit-path";

  private static final String ERROR_PLACEHOLDER = "${errorMessage}";
  private static final String J_SECURITY_CHECK = "j_security_check";
  private static final String J_USERNAME = "j_username";
  private static final String J_PASSWORD = "j_password";

  private static final Map<String, String> loginPageTemplateCache = new HashMap<String, String>();

  private static enum Status {
    NOT_STARTED, FORWARDED_TO_LOGIN, RESPONSE_RECEIVED, SUCCESS
  }

  /**
   * stored the value of AUTH_REDIRECT_URL. it will contain a fully qualified
   * URL to some external auth page
   */
  private String redirectUrl = null;
  private String loginPageTemplate = null;
  private Status status = Status.NOT_STARTED;
  private String submitPath = null;

  @Override
  public void init(Map<String, String> options) throws MissingConfigurationOptionException {
    redirectUrl = getOption(PARAM_AUTH_REDIRECT_URL, options, false);
    String customustomLoginPageResourceName = getOption(PARAM_LOGIN_PAGE_PATH, options, false);
    if (customustomLoginPageResourceName != null && redirectUrl != null) {
      String errMsg = " conflicts with property '" + PARAM_AUTH_REDIRECT_URL + "'. Remove one of them.";
      throw new BadConfigurationOptionException(PARAM_LOGIN_PAGE_PATH, errMsg);
    }

    submitPath = getOption(SUBMIT_PATH, options, false);
    if (null == submitPath) {
      submitPath = J_SECURITY_CHECK;
    }

    loginPageTemplate = getLoginPageTemplate(customustomLoginPageResourceName);
  }

  /**
   * Returns the cached version of a resource
   * 
   * @param projectResourceName
   * @return the template from the project, or the default template if null was
   *         passed as a parameter
   * @throws BadConfigurationOptionException
   */
  private static String getLoginPageTemplate(String projectResourceName) throws BadConfigurationOptionException {
    String resourceContent = loginPageTemplateCache.get(projectResourceName);
    if (resourceContent == null) {
      InputStream loginPageStream = null;
      try {
        if (projectResourceName != null) {
          String resourceLocation = "conf/" + projectResourceName;
          loginPageStream = WorklightBundles.getInstance().getProjectClassLoader()
              .getResourceAsStream(resourceLocation);
          if (loginPageStream == null) {
            String errMsg = " has a problem. " + projectResourceName + " can't be found under server/conf/ directory.";
            throw new BadConfigurationOptionException(PARAM_LOGIN_PAGE_PATH, errMsg);
          }
        }

        if (loginPageStream == null) {
          // use the login template that we have inside the wl-server-core.jar
          // this is also important for backward compatibility.
          loginPageStream = FormBasedAuthenticator.class.getResourceAsStream(DEFAULT_LOGIN_HTML_TEMPLATE);
        }
        try {
          resourceContent = IOUtils.toString(loginPageStream);
          loginPageTemplateCache.put(projectResourceName, resourceContent);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } finally {
        IOUtils.closeQuietly(loginPageStream);
      }
    }

    return resourceContent;
  }

  protected String getOption(String name, Map<String, String> options, boolean isMandatory)
      throws MissingConfigurationOptionException {
    String res = options.remove(name);
    if (res != null) {
      res = res.trim();
    }
    if (isMandatory && (res == null || res.isEmpty())) {
      throw new MissingConfigurationOptionException(name);
    }
    return res;
  }

  @Override
  public AuthenticationResult processRequest(HttpServletRequest request, HttpServletResponse response,
      boolean isAccessToProtectedResource) throws IOException, ServletException {
    switch (status) {
    case NOT_STARTED:
      sendLoginPage(response, "");
      status = Status.FORWARDED_TO_LOGIN;
      return AuthenticationResult.createFrom(AuthenticationStatus.CLIENT_INTERACTION_REQUIRED);

    case FORWARDED_TO_LOGIN:
      if (isRequestToSubmitUrl(request)) {
        status = Status.RESPONSE_RECEIVED;
        userName = request.getParameter(J_USERNAME);
        password = request.getParameter(J_PASSWORD);
        return AuthenticationResult.createFrom(AuthenticationStatus.SUCCESS);
      } else {
        if (isAccessToProtectedResource) {
          sendLoginPage(response, "");
          return AuthenticationResult.createFrom(AuthenticationStatus.CLIENT_INTERACTION_REQUIRED);
        }
        return AuthenticationResult.createFrom(AuthenticationStatus.REQUEST_NOT_RECOGNIZED);
      }

    default:
      throw new IllegalStateException("The form authenticator doesn't expect any requests in state " + status);
    }
  }

  private void sendLoginPage(HttpServletResponse response, String errorMessage) throws IOException {
    if (redirectUrl != null) {
      response.sendRedirect(redirectUrl);
    } else {
      response.setHeader("Expires", "-1");

      String replacedLoginPageTemplate = FileTemplate.replaceToken(ERROR_PLACEHOLDER, errorMessage, loginPageTemplate);
      PrintWriter writer = response.getWriter();
      writer.print(replacedLoginPageTemplate);
    }
  }

  @Override
  public AuthenticationResult processRequestAlreadyAuthenticated(HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {
    return AuthenticationResult.createFrom(AuthenticationStatus.REQUEST_NOT_RECOGNIZED);
  }

  @Override
  public AuthenticationResult processAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      String errorMessage) throws IOException, ServletException {
    response.setHeader("Expires", "-1");
    if (errorMessage == null) {
      errorMessage = "Please check the credentials";
    }
    sendLoginPage(response, errorMessage);
    status = Status.FORWARDED_TO_LOGIN;
    return AuthenticationResult.createFrom(AuthenticationStatus.CLIENT_INTERACTION_REQUIRED);
  }

  @Override
  public HttpServletRequest getRequestToProceed(HttpServletRequest currentRequest, HttpServletResponse currentResponse,
      UserIdentity user) throws IOException {
    return null;
  }

  @Override
  public boolean changeResponseOnSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (status == Status.RESPONSE_RECEIVED && isRequestToSubmitUrl(request)) {
      response.setStatus(HttpServletResponse.SC_OK);
      JSONUtils.sendJSONObject(response, new JSONObject());
      status = Status.SUCCESS;
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((loginPageTemplate == null) ? 0 : loginPageTemplate.hashCode());
    result = prime * result + ((redirectUrl == null) ? 0 : redirectUrl.hashCode());
    result = prime * result + ((status == null) ? 0 : status.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    ExampleFormBasedAuthenticator other = (ExampleFormBasedAuthenticator) obj;
    if (loginPageTemplate == null) {
      if (other.loginPageTemplate != null)
        return false;
    } else if (!loginPageTemplate.equals(other.loginPageTemplate))
      return false;
    if (redirectUrl == null) {
      if (other.redirectUrl != null)
        return false;
    } else if (!redirectUrl.equals(other.redirectUrl))
      return false;
    if (status != other.status)
      return false;
    return true;
  }

  @Override
  public Map<String, Object> getAuthenticationData() {
    Map<String, Object> userNameAndPassword = new HashMap<String, Object>(4);
    userNameAndPassword.put(USER_NAME, userName);
    userNameAndPassword.put(USER_PASSWORD, password);
    return userNameAndPassword;
  }

  private boolean isRequestToSubmitUrl(HttpServletRequest request) {
    return request.getRequestURI().indexOf(submitPath) != -1;
  }
}
