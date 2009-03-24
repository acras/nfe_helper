package br.com.acras.nfe;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import br.com.acras.utils.GenericEncryption;
import br.com.acras.utils.GenericEncryptionException;

class WebServiceClientInitializationHandler extends CustomHttpHandler
{
  Map<String, KeyEntryReference> keyEntryMap;
  
  public WebServiceClientInitializationHandler(Map<String, KeyEntryReference> keyEntryMap)
  {
    this.keyEntryMap = keyEntryMap;    
  }
  
  protected void handle(CustomHttpExchange exchange) throws Exception
  {
    String keyStoreId = exchange.getParameter("keystoreid");
    String trustStoreFile = exchange.getParameter("truststorefile");
    String trustStorePassword = exchange.getParameter("truststorepassword");

    KeyEntryReference keRef = keyEntryMap.get(keyStoreId);
    if (keRef == null)
      throw new NotFoundException("No key entry with id " + keyStoreId +
          " was found (not initialized?)");
      
    SSLContext context = SSLContext.getInstance("SSL");
    context.init(
        createKeyManagers(keRef),
        createTrustManagers(trustStoreFile, trustStorePassword),
        null);
    
    HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
  }
  
  protected String getAllowedMethod()
  {
    return "GET";
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

