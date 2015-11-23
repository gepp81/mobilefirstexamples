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

/server/conf/worklight.properties
  Values of properties for simple search in OpenLDAP server.

/server/conf/authenticatiConfig.xml
  Defines Custom Login Module, Realm and Security Test to connect with LDAP.
  
It's possible extend the class create our own security type. But in 7.1 the class that we extends only return a boolean type. The only posibility is throw a new RuntimeExeption. This has a TODO by IBM dev team. They want add a custom Object for a boolean.
