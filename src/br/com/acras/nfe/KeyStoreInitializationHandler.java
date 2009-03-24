import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.Map;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;

import com.sun.net.httpserver.HttpExchange;

class KeyStoreInitializationHandler extends CustomHttpHandler
{
  Map<String, KeyEntryReference> keyEntryMap;
  
  final String mapEntryHeader = "X-Key-Store-Id";
  
  public KeyStoreInitializationHandler(Map<String, KeyEntryReference> keyEntryMap)
  {
    this.keyEntryMap = keyEntryMap;
  }
  
  protected void handle(CustomHttpExchange exchange) throws Exception
  {
    String paramKSType = exchange.tryParameter("keystoretype");
    String paramKSFile = exchange.tryParameter("keystorefile");
    String paramKSPassword = exchange.tryParameter("keystorepassword");
    String paramKEAlias = exchange.tryParameter("keyentryalias");
    String paramKEPassword = exchange.tryParameter("keyentrypassword");
    
    KeyStoreManager ksm = KeyStoreManager.createKeyStoreManager(
        paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);
    
    ksm.validateParams();
    
    String mapEntry = getMapEntry(paramKSType, paramKSFile, paramKEAlias);
    keyEntryMap.remove(mapEntry);
    
    KeyStore.PrivateKeyEntry keyEntry = ksm.getEntry();
    
    keyEntryMap.put(mapEntry, new KeyEntryReference(ksm.getKeyStore(), paramKEAlias, keyEntry));
    exchange.addHeader(mapEntryHeader, mapEntry);
  }

  protected String getAllowedMethod()
  {
    return "GET"; 
  }
  
  private String getMapEntry(String paramKSType, String paramKSFile,
      String paramKEAlias)
  {
    String result = paramKSType + ":";
    result += paramKSFile == null ? "null" : paramKSFile;
    result += ":" + paramKEAlias;
    
    return result;
  }
  
}

abstract class KeyStoreManager
{
  protected String paramKSType;
  protected String paramKSFile;
  protected String paramKSPassword;
  protected String paramKEAlias;
  protected String paramKEPassword;
  
  private KeyStore keyStore;
  
  public static KeyStoreManager createKeyStoreManager(String paramKSType,
      String paramKSFile, String paramKSPassword, String paramKEAlias,
      String paramKEPassword) throws Exception
  {
    String ksType = paramKSType != null ? paramKSType : "<null>";
    
    if (ksType.compareTo("JKS") == 0)
      return new JKSManager(
          paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);
    else if (ksType.compareTo("PKCS11") == 0)
      return new PKCS11Manager(
          paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);
    else
      throw new NotFoundException("Invalid key store type: " + ksType);
  }
  
  public KeyStoreManager(String paramKSType, String paramKSFile, String paramKSPassword,
      String paramKEAlias, String paramKEPassword) throws Exception
  {
    this.paramKSType = paramKSType;
    this.paramKSFile = paramKSFile;
    this.paramKSPassword = paramKSPassword;
    this.paramKEAlias = paramKEAlias;
    this.paramKEPassword = paramKEPassword;
    
    keyStore = loadKeyStore();
  }
  
  abstract public void validateParams() throws BadRequestException;
  
  public KeyStore getKeyStore()
  {
    return keyStore;
  }
  
  public KeyStore.PrivateKeyEntry getEntry() throws Exception
  {
    KeyStore.PrivateKeyEntry result;
    KeyStore.PasswordProtection keyEntryPassword = getKeyEntryPassword();
    
    try
    {
      result = (KeyStore.PrivateKeyEntry) keyStore.getEntry(paramKEAlias, keyEntryPassword);
    }
    catch(UnrecoverableKeyException e)
    {
      throw new ForbiddenException(
          "Incorrect password for key " + paramKEAlias);
    }
    
    if (result == null)
      throw new NotFoundException(
          "Alias " + paramKEAlias + " not found in key store"); 
    
    return result;
  }
  
  private KeyStore loadKeyStore() throws Exception
  {
    KeyStore result = null;
    
    InputStream keyStream = getKeyStream();
    String keyStorePassword;
    
    try
    {
      keyStorePassword = AcrasEncryption.decryptString(paramKSPassword);
    }
    catch(AcrasEncryptionException e)
    {
      throw new BadRequestException("Key store password not properly encoded");
    }

    result = KeyStore.getInstance(paramKSType);
    
    try
    {
      result.load(keyStream, keyStorePassword.toCharArray());
    }
    catch(Exception e)
    {
      throw new ForbiddenException(e);
    }

    return result;    
  }
  
  protected InputStream getKeyStream() throws HttpException
  {
    return null;
  }
  
  protected KeyStore.PasswordProtection getKeyEntryPassword() throws AcrasEncryptionException
  {
    return null;
  }
}

class PKCS11Manager extends KeyStoreManager
{
  public PKCS11Manager(String paramKSType, String paramKSFile, String paramKSPassword,
      String paramKEAlias, String paramKEPassword) throws Exception
  {
    super(paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);
  }
  
  public void validateParams() throws BadRequestException
  {
    if (paramKSPassword == null || paramKEAlias == null)
      throw new BadRequestException(
          "PKCS11 key store requires all of the following params: " +
          "keystorepassword, keyentryalias");
  }
}

class JKSManager extends KeyStoreManager
{
  public JKSManager(String paramKSType, String paramKSFile, String paramKSPassword,
      String paramKEAlias, String paramKEPassword) throws Exception
  {
    super(paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);
  }
  
  public void validateParams() throws BadRequestException
  {
    if (paramKSFile == null || paramKSPassword == null ||
        paramKEAlias == null || paramKEPassword == null)
      throw new BadRequestException(
          "JKS key store requires all of the following params: " +
          "keystorefile, keystorepassword, keyentryalias, " +
          "keyentrypassword");
  }
  
  protected InputStream getKeyStream() throws HttpException
  {
    try
    {
      return new java.io.FileInputStream(paramKSFile);
    }
    catch(FileNotFoundException e)
    {
      throw new NotFoundException(
          "Key store " + paramKSFile + " does not exist");
    }
  }
  
  protected KeyStore.PasswordProtection getKeyEntryPassword() throws AcrasEncryptionException
  {
    return new KeyStore.PasswordProtection(
        AcrasEncryption.decryptString(paramKEPassword).toCharArray());
  }
}

