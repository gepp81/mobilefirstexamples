# mobilefirstexamples
Examples of mobilefirst platform adapter.
Has two adapter as simples examples to uses adapter.
Also describes a simple security where integrates LDAP server to security.

Example Adapter
---------------

It is a simple example. This adapter can connect to MySQL DB (can be antoher DB) through JDBC.
It explains how to connect to JDBC by configuring the server or configure the connection on the same adapter.

Invoker Adapter
---------------

Has a very simple example where one adapter can call another adapter method.

Other files
---------------

-/server/conf/worklight.properties
  Values of properties for simple search in OpenLDAP server.

-/server/conf/authenticatiConfig.xml
  Defines Custom Login Module, Realm and Security Test to connect with LDAP.
  
It's possible extend the class create our own security type. But in 7.1 the class that we extends only return a boolean type. The only posibility is throw a new RuntimeExeption. This has a TODO by IBM dev team. They want add a custom Object for a boolean. Add this example in server source java folder.

How to work.

  When is called method login in class ExampleAuthLoginModule

  public boolean login(Map<String, Object> authenticationData) {
    ...
    // This initialization can throw an exeption beacuse LDAP Invalid Credentials.
    ldapCtx = new InitialLdapContext(env, null);
    ...
    
    catch {
      // Here only can throw a RuntimeExeption beacuse the method override don't allow throw exception and only can allow return a boolean value.
    }
  }
  
  This runtimeException call the method processAuthenticationFailure in ExampleFormBasedAuthenticator (extends UsernamePasswordAuthenticator)
  
  Her we can get the error mensage of exception and return our custom response.
  
  Is's possible to use this with all type of login (not only LDAP)
 
