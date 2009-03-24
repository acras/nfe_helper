import java.net.Socket;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;

// Descaradamente copiado de http://objectmix.com/java/76407-a.html
public class CustomKeyManager implements X509KeyManager
{
  private final X509KeyManager keyManager;
  private final KeyStore keyStore;
  private final String alias;
  private final KeyStore.PrivateKeyEntry privateKeyEntry;

  public CustomKeyManager(X509KeyManager keyManager, KeyStore keyStore, String alias,
      KeyStore.PrivateKeyEntry privateKeyEntry)
  {
    this.keyManager = keyManager;
    this.keyStore = keyStore;
    this.alias = alias;
    this.privateKeyEntry = privateKeyEntry;
  }

  public String[] getClientAliases(String keyType, Principal[] issuers)
  {
    return new String[] { alias };
  }

  public String[] getServerAliases(String keyType, Principal[] issuers)
  {
    return keyManager.getServerAliases(keyType, issuers);
  }

  public X509Certificate[] getCertificateChain(String alias)
  {
    assertAlias(alias);
    
    Certificate[] chain = null;
    try
    {
      chain = keyStore.getCertificateChain(alias);
    }
    catch (KeyStoreException e)
    {
      throwError(e.getClass().getName() + ": " + e.getMessage());
    }
    
    final X509Certificate[] certChain = new X509Certificate[chain.length];
    for (int i = 0; i < chain.length; i++)
    {
      certChain[i] = (X509Certificate) chain[i];
    }
    return certChain;
  }

  public PrivateKey getPrivateKey(String alias)
  {
    assertAlias(alias);
    
    return privateKeyEntry.getPrivateKey();
  }

  public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket)
  {
    return alias;
  }

  public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket)
  {
    return keyManager.chooseServerAlias(keyType, issuers, socket);
  }

  private void assertAlias (String alias)
  {
    if (!alias.equals(this.alias))
      throwError("Unexpected alias " + alias);
  }
  
  private void throwError(String message)
  {
    throw new CustomKeyManagerRuntimeException(message);
  }
}

