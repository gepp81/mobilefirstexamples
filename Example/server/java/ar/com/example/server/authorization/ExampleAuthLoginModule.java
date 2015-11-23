package ar.com.example.server.authorization;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import com.worklight.common.log.WorklightLogger.MessagesBundles;
import com.worklight.common.log.WorklightServerLogger;
import com.worklight.core.auth.ext.DeviceSSO;
import com.worklight.extension.api.Messages;
import com.worklight.server.auth.api.MissingConfigurationOptionException;
import com.worklight.server.auth.api.UserIdentity;
import com.worklight.server.auth.api.UserNamePasswordLoginModule;
import com.worklight.server.auth.api.WorkLightAuthLoginModule;

/**
 * 
 * @author Guillermo Pi Dote - guillermo.pidote@globant.com
 *
 */
@DeviceSSO(supported = true) 
public class ExampleAuthLoginModule extends UserNamePasswordLoginModule {

  private static final long serialVersionUID = -2293374159804792717L;
  private static final WorklightServerLogger logger = new WorklightServerLogger(ExampleAuthLoginModule.class,
      MessagesBundles.EXTENSION_API);

  private static enum VALIDATIONTYPE {
    EXISTS("exists"), SEARCHPATTERN("searchPattern"), CUSTOM("custom");

    private final String value;

    private VALIDATIONTYPE(String value) {
      this.value = value;
    }

    public static boolean contains(String value) {
      for (VALIDATIONTYPE t : values()) {
        if (t.value.equals(value))
          return true;
      }
      return false;
    }
  }

  // Configuration options names
  private static final String LDAP_PROVIDER_URL_OPTION_NAME = "ldapProviderUrl";
  private static final String LDAP_TIMEOUT_MS_OPTION_NAME = "ldapTimeoutMs";
  private static final String LDAP_SECURITY_AUTHENTICATION_OPTION_NAME = "ldapSecurityAuthentication";
  private static final String VALIDATION_TYPE_OPTION_NAME = "validationType";
  private static final String LDAP_SECURITY_PRINCIPAL_PATTERN_OPTION_NAME = "ldapSecurityPrincipalPattern";
  private static final String LDAP_SEARCH_FILTER_PATTERN_OPTION_NAME = "ldapSearchFilterPattern";
  private static final String LDAP_SEARCH_BASE_OPTION_NAME = "ldapSearchBase";
  private static final String LDAP_REFERRAL = "ldapReferral";

  private static final String COM_SUN_JNDI_LDAP_LDAP_CTX_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
  private static final String COM_SUN_JNDI_LDAP_CONNECT_TIMEOUT = "com.sun.jndi.ldap.connect.timeout";
  private static final String USERNAME_PLACEHOLDER = "{username}";

  // Configuration options values to be populated on login module initialization
  private String ldapProviderUrl;
  private String ldapTimeoutMs;
  private String ldapSecurityAuthentication;
  private String ldapSecurityPrincipalPattern;
  private String validationType;
  private String ldapSearchFilterPattern;
  private String ldapSearchBase;
  private String ldapReferral;

  @Override
  public void init(Map<String, String> options) throws MissingConfigurationOptionException, RuntimeException {
    ldapProviderUrl = getConfigurationOption(LDAP_PROVIDER_URL_OPTION_NAME, options, true);
    ldapTimeoutMs = getConfigurationOption(LDAP_TIMEOUT_MS_OPTION_NAME, options, true);
    ldapSecurityAuthentication = getConfigurationOption(LDAP_SECURITY_AUTHENTICATION_OPTION_NAME, options, true);
    validationType = getConfigurationOption(VALIDATION_TYPE_OPTION_NAME, options, true);
    ldapSecurityPrincipalPattern = getConfigurationOption(LDAP_SECURITY_PRINCIPAL_PATTERN_OPTION_NAME, options, true);

    ldapSearchFilterPattern = getConfigurationOption(LDAP_SEARCH_FILTER_PATTERN_OPTION_NAME, options, false);
    ldapSearchBase = getConfigurationOption(LDAP_SEARCH_BASE_OPTION_NAME, options, false);
    ldapReferral = getConfigurationOption(LDAP_REFERRAL, options, false);

    if (!VALIDATIONTYPE.contains(validationType)) {
      String errorMessage = logger.getFormatter().format(Messages.logger.ldapInvalidValidationType,
          VALIDATION_TYPE_OPTION_NAME);
      throw new RuntimeException(errorMessage);
    }

    if (validationType.equals(VALIDATIONTYPE.SEARCHPATTERN.value)) {
      if (null == ldapSearchBase)
        throw new MissingConfigurationOptionException(LDAP_SEARCH_BASE_OPTION_NAME);
      if (null == ldapSearchFilterPattern)
        throw new MissingConfigurationOptionException(LDAP_SEARCH_FILTER_PATTERN_OPTION_NAME);
    }
  }

  @Override
  public boolean login(Map<String, Object> authenticationData) {
    populateCache(authenticationData);

    Hashtable<String, String> env = new Hashtable<String, String>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, COM_SUN_JNDI_LDAP_LDAP_CTX_FACTORY);
    env.put(COM_SUN_JNDI_LDAP_CONNECT_TIMEOUT, ldapTimeoutMs);
    env.put(Context.PROVIDER_URL, ldapProviderUrl);
    env.put(Context.SECURITY_AUTHENTICATION, ldapSecurityAuthentication);

    env.put(Context.SECURITY_PRINCIPAL, ldapSecurityPrincipalPattern.replace(USERNAME_PLACEHOLDER, username));
    env.put(Context.SECURITY_CREDENTIALS, password);
    if (ldapReferral != null) {
      env.put(Context.REFERRAL, ldapReferral);
    }

    LdapContext ldapCtx = null;
    try {
      ldapCtx = new InitialLdapContext(env, null);

      boolean authSuccess = true;
      if (validationType.equals(VALIDATIONTYPE.SEARCHPATTERN.value)) {
        authSuccess = doSearchPatternValidation(ldapCtx, username);
      } else if (validationType.equals(VALIDATIONTYPE.CUSTOM.value)) {
        authSuccess = doCustomValidation(ldapCtx, username, password);
      }

      if (authSuccess) {
        return true;
      } else {
        throw new Exception(validationType);
      }
    } catch (Exception e) {
      StringWriter stack = new StringWriter();
      e.printStackTrace(new PrintWriter(stack));
      logger.warn(e, "login", Messages.logger.ldapAuthenticationFailed, stack.toString());
      throw new RuntimeException(stack.toString());
    }
  }

  private String getConfigurationOption(String key, Map<String, String> options, boolean isMandatory)
      throws MissingConfigurationOptionException {
    String value = options.remove(key);

    if (null == value) {
      if (isMandatory) {
        throw new MissingConfigurationOptionException(key);
      } else {
        return value;
      }
    }

    return value.trim();
  }

  private boolean doSearchPatternValidation(LdapContext ldapCtx, String username) throws Exception {
    ldapCtx.setRequestControls(null);
    SearchControls searchControls = new SearchControls();
    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    searchControls.setTimeLimit(Integer.parseInt(ldapTimeoutMs));

    String seachFilter = ldapSearchFilterPattern.replace(USERNAME_PLACEHOLDER, username);
    NamingEnumeration<?> searchResults = ldapCtx.search(ldapSearchBase, seachFilter, searchControls);

    if (searchResults.hasMoreElements()) {
      return true;
    } else {
      return false;
    }
  }

  public boolean doCustomValidation(LdapContext ldapCtx, String username, String password) {
    return true;
  }

  @Override
  public UserIdentity createIdentity(String realm) {
    return new UserIdentity(realm, username, username, null, null, password);
  }

  @Override
  public void logout() {
    username = null;
  }

  @Override
  public void abort() {
    username = null;
  }

  @Override
  public WorkLightAuthLoginModule clone() throws CloneNotSupportedException {
    return (WorkLightAuthLoginModule) super.clone();
  }
}
