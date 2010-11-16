package br.com.acras.nfe;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import br.com.acras.utils.GenericEncryption;
import br.com.acras.utils.GenericEncryptionException;

class WebServiceClientInitializationHandler extends CustomHttpHandler
{
  String baseDirectory;
  Map<String, KeyEntryReference> keyEntryMap;
  boolean enableSSLChecks;
  
  public WebServiceClientInitializationHandler(String baseDirectory,
      Map<String, KeyEntryReference> keyEntryMap, boolean enableSSLChecks)
  {
    this.baseDirectory = baseDirectory;
    this.keyEntryMap = keyEntryMap;
    this.enableSSLChecks = enableSSLChecks;
  }
  
  protected void handle(CustomHttpExchange exchange) throws Exception
  {
    String keyStoreId = exchange.getParameter("keystoreid");
    String trustStoreFile = baseDirectory + File.separator +
        exchange.getParameter("truststorefile");
    String trustStorePassword = exchange.getParameter("truststorepassword");

    KeyEntryReference keRef = keyEntryMap.get(keyStoreId);
    if (keRef == null)
      throw new NotFoundException("No key entry with id " + keyStoreId +
          " was found (not initialized?)");

    initSSLSocketFactory(keRef, trustStoreFile, trustStorePassword);      
  }
  
  protected String getAllowedMethod()
  {
    return "GET";
  }
  
  private synchronized void initSSLSocketFactory(KeyEntryReference keRef,
      String trustStoreFile, String trustStorePassword) throws Exception
  {
    SSLContext context = SSLContext.getInstance("SSL");
    context.init(
        createKeyManagers(keRef),
        createTrustManagers(trustStoreFile, trustStorePassword),
        null);
    
    HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
    
    if (!enableSSLChecks)
    {
      HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()  
          {        
            public boolean verify(String hostname, SSLSession session)  
            {
              System.err.printf("=> WARNING: skipping SSL validation for host %s.\n", hostname); 
              return true;  
            }  
          });
    }
  }
  
  private TrustManager[] createTrustManagers(String trustStoreFile, String trustStorePassword)
      throws Exception
  {
    FileInputStream inputStream;
    String password;
    
    try
    {
      inputStream = new FileInputStream(trustStoreFile);
    }
    catch(FileNotFoundException e)
    {
      throw new NotFoundException("Trust store " + trustStoreFile + " does not exist");
    }
    
    try
    {
      password = GenericEncryption.decryptString(trustStorePassword);
    }
    catch(GenericEncryptionException e)
    {
      throw new BadRequestException("Trust store password not properly encoded");
    }
    
    KeyStore ks = KeyStore.getInstance("JKS");
    
    try
    {
      ks.load(inputStream, password.toCharArray());
    }
    catch(Exception e)
    {
      throw new ForbiddenException(e);
    }
    
    TrustManagerFactory tmFactory;
    
    tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmFactory.init(ks);
    
    return tmFactory.getTrustManagers();
  }
  
  private KeyManager[] createKeyManagers(KeyEntryReference keRef)
  {
    CustomKeyManager keyManager =
        new CustomKeyManager(null, keRef.getKeyStore(), keRef.getAlias(), keRef.getKeyEntry());
    return new KeyManager[] { keyManager };
  }
}

