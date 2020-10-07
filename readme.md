# GemFire Kerberos Authentication

This project introduces a GemFire `SecurityManager` implementation which integrates GemFire with Kerberos for secure token based authentication.   

# Terms
1. KDC -  key distribution center
2. JAAS - Java Authentication and Authorization Service

# Design

GemFire has a very simple plugin mechanism for integrating with various security frameworks.    The plugin point for doing the authentication step is called `SecurityManager`.   The plugin point for components to present their credentials or tokens GemFire that is called `AuthInitialize`.

GemFire uses persistent connections.   So while the connection is valid the connection will not be re-authenticated. 

## SecurityManager

The kerberos `SeciurityManager` implementation called `KerberosSecurityManager`.   When implementing tbe `SeciurityManager` interface there are a couple of key methods.   

1. `SeciurityManager.authenticate()` - This method does the work of accepting the token passed in from the `AuthInitialize` implementation.   It will interact with the connection to the KDC established in the `init` method to perform the actual acceptance of the token. 
2. `SecurityManager.init()` - This method logs the security manager into the KDC.

## AuthInitialize

The Kerbros `AuthInitialize` implementation is called `KerberosAuthInitializer`.    The important methods are:

1. `AuthInitialize.getCredentials()`  This method initializes the connection to the KDC  if its not already connected.   Once connected, `getCredentials` gets the token to send to the `SeciurityManager`.    The `SecurityManager` will then to accept the token by verifying it with the KDC.

# Install

This project uses JAAS for Kerberos and GemFire so if you need more information on a particular aspect of something please use those communities as well.    I will try to do my best to document the install as best as I can.

## Java Properties

We are using JAAS to connect to Kerberos.   This means we need to configure JAAS Kerberos details.   These JAAS properties are needed for the startup of the various components.   This would include applications, GemFire Locators and GemFire Servers.

* `--J=-Djava.security.krb5.realm=<realm>` - The Kerberos Realm that we are authenticating with
* `--J=-Djava.security.krb5.kdc=<host>` - The host of the Kerberos KDC
* `--J=-Djava.security.auth.login.config=<jaas.conf>` - The JAAS Configuration
* `--J=-Djavax.security.auth.useSubjectCredsOnly=false` - Allow kerberos to decide how to get credentials

## Add Library to Java Classpath

The library needs to be added to the classpath of all the various components.   This would be applications, GemFire Locators and GemFire Servers.

* `--classpath=/path/to/vmware-gemfire-kerberos-0.0.1-SNAPSHOT.jar` 

## Sample JAAS Configuration

This JAAS example configuration assumes that there is a keytab which includes the private key.
   
```
gemfire_cluster {
com.sun.security.auth.module.Krb5LoginModule required
useKeyTab=true
storeKey=true
keyTab="../../gemfire_cluster.keytab"
useTicketCache=false
principal="gemfire/demo-virtual-machine@GEMFIRE";
};
```
## GemFire Security Properties

### security-client-auth-init

This is property configures GemFire `AuthInitialize` component for the client.   To enable the Kerberos token to be presented to the `SecurityManager` use the `com.vmware.gemfire.kerberos.KerberosAuthInitializer` implementation.

```
security-client-auth-init=com.vmware.gemfire.kerberos.KerberosAuthInitializer
```
### security-peer-auth-init
This is property configures GemFire `AuthInitialize` component for servers or peers.   To enable the Kerberos token to be presented to the `SecurityManager` use the `com.vmware.gemfire.kerberos.KerberosAuthInitializer` implementation.
```
security-peer-auth-init=com.vmware.gemfire.kerberos.KerberosAuthInitializer
```

### security-manager
The way we configure the `SecurityManager` for a given server component.   Only GemFire Locators and Server will instantiate the `SecurityManager`.   With Kerberos the   `KerberosSecurityManager` implementation is what will check the validity of the token with the KDC.
```
security-manager=com.vmware.gemfire.kerberos.KerberosSecurityManager
```
### security-kerberos-auth-jaas-name
This property configures the `KerberosAuthInitializer` to use the externalized JAAS security configuration.
```
security-kerberos-auth-jaas-name=gemfire_cluster
```
### security-kerberos-auth-service-name
Specify the service principal name that we are going to authenticate with.   
```
security-kerberos-auth-service-name=gemfire
```
### security-kerberos-auth-context-lifetime
The lifetime of the token in seconds.
```
# GSSContext.INDEFINITE_LIFETIME = Max Integer or 2147483647
# GSSContext.DEFAULT_LIFETIME = 0
security-kerberos-auth-context-lifetime=2147483647
```
### security-kerberos-auth-login-context-username
Enable the user to login into the KDC using user name and password.   I recommend using the key tab and not providing a username and password.

```
#security-kerberos-auth-login-context-username=cblack
#security-kerberos-auth-login-context-password=TrustNo1
```
### security-kerberos-security-manager-login-context
Allow the user to enable the `KerberosSecurityManager` to login to the KDC using username and password.   I recommend using the key tab and not providing a username and password.
```
#security-kerberos-security-manager-login-context-username=
#security-kerberos-security-manager-login-context-password=TrustNo1
```
### security-kerberos-security-manager-jaas-name
The JAAS configuration name to use.
```
security-kerberos-security-manager-jaas-name=gemfire_security_manager
```

# Connect GFSH
In Apache Geode version 1.13 and on we have enabled a token based login.   This means we need the token in a form that we can enter it.   The library has a helper program to generate a token which can be interpreted by the `KerberosSecurityManager`.   

## Sample Execution
The  `com.vmware.gemfire.kerberos.GetToken` application uses the `KerberosAuthInitializer` to login to Kerberos we need GemFire classes as well as the `vmware-gemfire-kerberos` library.   That gives us the classpath arguments to the java application.   Feel free to CLASSPATH environment variable, or the java cp argument as illustrated below.

In addition to the classpath we are going to need to configure the Java Kerberos implementation to point to the right Kerberos.   That is all of the `-D` properties in the example below.

Finally, we need to location of the gemfire security file to finish the configuration `KerberosAuthInitializer`.

The output is the token to use with gfsh.
```
$ java -cp /home/demo/dev/projects/vmware-gemfire-kerberos/build/libs/vmware-gemfire-kerberos-0.0.1-SNAPSHOT.jar:/home/demo/dev/gemfire/apache-geode-1.13.0/lib/* -Djava.security.krb5.realm=GEMFIRE -Djava.security.krb5.kdc=localhost -Djava.security.auth.login.config=jaas.conf -Djavax.security.auth.useSubjectCredsOnly=false com.vmware.gemfire.kerberos.GetToken --security-properties-file=/home/demo/dev/projects/vmware-gemfire-kerberos/etc/gfsecurity-app.properties
YIICeAYJKoZIhvcSAQICAQBuggJnMIICY6ADAgEFoQMCAQ6iBwMFAAAAAACjggFsYYIBaDCCAWSgAwIBBaEJGwdHRU1GSVJFoiowKKADAgEDoSEwHxsHZ2VtZmlyZRsUZGVtby12aXJ0dWFsLW1hY2hpbmWjggEkMIIBIKADAgESoQMCAQaiggESBIIBDk3GGn9uvxQutNbM+X6bQ+UokG+0jvSNDA5CLF1Bhk+V469hkuNxOO/LfRglELZNSua0EYtUjXIMGV+3QdIv0r1Hx8p4LtkYuhvFBNVRnypSfl9WUxYSDW++NEGXkMJD1YlEx830rIVDMnTuP1bpqwddGd3t3LSSWpu+Oq9IFDao+niztg+FJWNckLDzyqKzPfEiJJ0TWSdPviMrwCq2Kpb3RmXhm36yRZO5bpCchDzYJKC4lQPSrwPwMnGMQzcW2Wpif409VxyWwLX3HHVn8KBh6KIbP8wCrLipyqT0vuiM6r2nNerO8U4262AFmHVnVPpwnCzkmouB/+lfiYy63kFray7sM7Sp2UhbJt801KSB3TCB2qADAgESooHSBIHPuCI1GNYrFDvAKmT9oYXtbd3mSNUhhS7Gg0k380YIWQMjC9WigdFOJUxQoyjl192vAbdhYGUvuhN3dJvVt+DFkYj/J0VAm+U/cw3mSvkhh9c7WKa3+QmofTaugGhJlay98LqxOIgZMHErJ+qfcZ1FItOlA6OTfaNxfXb2G909ILjhxv9weGZGB7/huuqQ0CKysPwZu3HEiHXr/d8G2XqkV1rm39fCeq1EF1PTz/aGVRBgs8K8AyuCil8y0uW7IYrs1Z44qTISvnDK9U7kzf3Y

```

Then use that token and connect to GemFire Cluster.

```
$ gfsh
    _________________________     __
   / _____/ ______/ ______/ /____/ /
  / /  __/ /___  /_____  / _____  / 
 / /__/ / ____/  _____/ / /    / /  
/______/_/      /______/_/    /_/    1.13.0

Monitor and Manage Apache Geode
gfsh>connect --token=YIICeAYJKoZIhvcSAQICAQBuggJnMIICY6ADAgEFoQMCAQ6iBwMFAAAAAACjggFsYYIBaDCCAWSgAwIBBaEJGwdHRU1GSVJFoiowKKADAgEDoSEwHxsHZ2VtZmlyZRsUZGVtby12aXJ0dWFsLW1hY2hpbmWjggEkMIIBIKADAgESoQMCAQaiggESBIIBDk3GGn9uvxQutNbM+X6bQ+UokG+0jvSNDA5CLF1Bhk+V469hkuNxOO/LfRglELZNSua0EYtUjXIMGV+3QdIv0r1Hx8p4LtkYuhvFBNVRnypSfl9WUxYSDW++NEGXkMJD1YlEx830rIVDMnTuP1bpqwddGd3t3LSSWpu+Oq9IFDao+niztg+FJWNckLDzyqKzPfEiJJ0TWSdPviMrwCq2Kpb3RmXhm36yRZO5bpCchDzYJKC4lQPSrwPwMnGMQzcW2Wpif409VxyWwLX3HHVn8KBh6KIbP8wCrLipyqT0vuiM6r2nNerO8U4262AFmHVnVPpwnCzkmouB/+lfiYy63kFray7sM7Sp2UhbJt801KSB3TCB2qADAgESooHSBIHPuCI1GNYrFDvAKmT9oYXtbd3mSNUhhS7Gg0k380YIWQMjC9WigdFOJUxQoyjl192vAbdhYGUvuhN3dJvVt+DFkYj/J0VAm+U/cw3mSvkhh9c7WKa3+QmofTaugGhJlay98LqxOIgZMHErJ+qfcZ1FItOlA6OTfaNxfXb2G909ILjhxv9weGZGB7/huuqQ0CKysPwZu3HEiHXr/d8G2XqkV1rm39fCeq1EF1PTz/aGVRBgs8K8AyuCil8y0uW7IYrs1Z44qTISvnDK9U7kzf3Y
Connecting to Locator at [host=localhost, port=10334] ..
Connecting to Manager at [host=172.16.142.166, port=1099] ..
Successfully connected to: [host=172.16.142.166, port=1099]

You are connected to a cluster of version: 1.13.0

gfsh>
```
NOTE: The token generated by `com.vmware.gemfire.kerberos.GetToken` is only valid for one login. 

# Extra

Creating the application principal and keytab in kerberos:
```
kadmin.local:  addprinc -randkey app/demo-virtual-machine
WARNING: no policy specified for app/demo-virtual-machine@GEMFIRE; defaulting to no policy
Principal "app/demo-virtual-machine@GEMFIRE" created.
kadmin.local:  ktadd -k gemfire_app.keytab app/demo-virtual-machine
Entry for principal app/demo-virtual-machine with kvno 2, encryption type aes256-cts-hmac-sha1-96 added to keytab WRFILE:gemfire_app.keytab.
Entry for principal app/demo-virtual-machine with kvno 2, encryption type aes128-cts-hmac-sha1-96 added to keytab WRFILE:gemfire_app.keytab.
kadmin.local:  exit

```
The -randkey option of addprinc specifies that the encryption key should be chosen at random instead of being derived from a password. Services normally authenticate using a keytab, so have no need for a password.