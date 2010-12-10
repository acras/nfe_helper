@java -cp %~dp0dist\acras.jar -Djavax.net.ssl.trustStore=lib\certificates\trusted.jks -Djavax.net.ssl.trustStorePassword=acrasnfe br.com.acras.nfe.HelperServer %*
