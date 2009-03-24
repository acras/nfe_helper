import java.security.KeyStore;

public class KeyEntryReference
{
  private KeyStore keyStore;
  private String alias;
  private KeyStore.PrivateKeyEntry keyEntry;
  
  public KeyEntryReference(KeyStore keyStore, String alias, KeyStore.PrivateKeyEntry keyEntry)
  {
    this.keyStore = keyStore;
    this.alias = alias;
    this.keyEntry = keyEntry;
  }
  
  public KeyStore getKeyStore()
  {
    return keyStore;
  }
  
  public String getAlias()
  {
    return alias;
  }
  
  public KeyStore.PrivateKeyEntry getKeyEntry()
  {
    return keyEntry;
  }
}

