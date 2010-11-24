package br.com.acras.nfe;

import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;

class WebServiceClientInitializationHandler extends CustomHttpHandler
{
  String baseDirectory;
  Map<String, KeyEntryReference> keyEntryMap;
  
  public WebServiceClientInitializationHandler(String baseDirectory,
      Map<String, KeyEntryReference> keyEntryMap)
  {
    this.baseDirectory = baseDirectory;
    this.keyEntryMap = keyEntryMap;
  }
  
  protected void handle(CustomHttpExchange exchange) throws Exception
  {
    String keyStoreId = exchange.getParameter("keystoreid");

    KeyEntryReference keRef = keyEntryMap.get(keyStoreId);
    if (keRef == null)
      throw new NotFoundException("No key entry with id " + keyStoreId +
          " was found (not initialized?)");

    initSSLSocketFactory(keRef);
  }
  
  protected String getAllowedMethod()
  {
    return "GET";
  }
  
  private synchronized void initSSLSocketFactory(KeyEntryReference keRef) throws Exception
  {
    SSLContext context = SSLContext.getInstance("SSL");
    context.init(
        createKeyManagers(keRef),
        null,
        null);
    
    HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
  }
  
  private KeyManager[] createKeyManagers(KeyEntryReference keRef)
  {
    CustomKeyManager keyManager =
        new CustomKeyManager(null, keRef.getKeyStore(), keRef.getAlias(), keRef.getKeyEntry());
    return new KeyManager[] { keyManager };
  }
}

