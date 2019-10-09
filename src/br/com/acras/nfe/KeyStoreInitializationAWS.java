package nfe;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.PrintStream;

import java.util.Map;
import java.util.Enumeration;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.lambda.runtime.Context;

import utils.*;

class KeyStoreInitializationAWS {
  String baseDirectory;
  String s3Bucket;
  Map<String, KeyEntryReference> keyEntryMap;

  public KeyStoreInitializationAWS(String s3Bucket, String baseDirectory,
      Map<String, KeyEntryReference> keyEntryMap) {
    this.s3Bucket = s3Bucket;
    this.baseDirectory = baseDirectory;
    this.keyEntryMap = keyEntryMap;
  }

  public String handle(HelperServerRequest resquest) throws Exception
  {
    String paramKSType = resquest.getKeystoretype();
    String paramKSFile = resquest.getKeystorefile();
    String paramKSPassword = resquest.getKeystorepassword();
    String paramKEAlias = resquest.getKeyentryalias();
    String paramKEPassword = resquest.getKeyentrypassword();

    if (paramKSFile != null && !paramKSFile.isEmpty())
      paramKSFile = baseDirectory + File.separator + paramKSFile;

    KeyStoreManager ksm = KeyStoreManager.createKeyStoreManager(
        s3Bucket, paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);

    return initKeyStore(ksm);
  }

  protected String getAllowedMethod()
  {
    return "GET";
  }

  private synchronized String initKeyStore(KeyStoreManager ksm) throws Exception
  {
    String effectiveAlias = ksm.getEffectiveAlias();

    String mapEntry = getMapEntry(ksm.getKSType(), ksm.getKSFile(), effectiveAlias);
    keyEntryMap.remove(mapEntry);

    KeyStore.PrivateKeyEntry keyEntry = ksm.getEntry(effectiveAlias);

    keyEntryMap.put(mapEntry,
        new KeyEntryReference(ksm.getKeyStore(), effectiveAlias, keyEntry));

    return mapEntry;
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
  protected String s3Bucket;

  private KeyStore keyStore;

  public static KeyStoreManager createKeyStoreManager(String s3Bucket, String paramKSType,
      String paramKSFile, String paramKSPassword, String paramKEAlias,
      String paramKEPassword) throws Exception
  {
    String ksType = paramKSType != null ? paramKSType : "<null>";

    if (ksType.compareTo("JKS") == 0)
      return new JKSManager(
        s3Bucket, paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);
    else if (ksType.compareTo("PKCS11") == 0)
      return new PKCS11Manager(
          paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);
    else if (ksType.compareTo("PKCS12") == 0)
      return new PKCS12Manager(
        s3Bucket, paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);
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

    validateParams();

    keyStore = loadKeyStore();
  }

  public String getKSType()
  {
    return paramKSType;
  }

  public String getKSFile()
  {
    return paramKSFile;
  }

  abstract public void validateParams() throws BadRequestException;

  public KeyStore getKeyStore()
  {
    return keyStore;
  }

  public String getEffectiveAlias() throws NotFoundException, KeyStoreException
  {
    String result = paramKEAlias;
    // Se o alias não for informado usa a primeira entrada que encontrar
    if (result == null)
    {
      Enumeration aliases = keyStore.aliases();
      while (aliases.hasMoreElements())
      {
        String s = (String) aliases.nextElement();
        if (keyStore.isKeyEntry(s))
        {
          result = s;
          break;
        }
      }
    }
    if (result == null)
      throw new NotFoundException("No key entry found in key store");

    return result;
  }

  public KeyStore.PrivateKeyEntry getEntry(String alias) throws Exception
  {
    KeyStore.PrivateKeyEntry result;
    KeyStore.PasswordProtection keyEntryPassword = getKeyEntryPassword();

    try
    {
      result = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, keyEntryPassword);
    }
    catch(UnrecoverableKeyException e)
    {
      throw new ForbiddenException("Incorrect password for key " + alias);
    }

    if (result == null)
      throw new NotFoundException("Alias " + alias + " not found in key store");

    return result;
  }

  private KeyStore loadKeyStore() throws Exception
  {
    KeyStore result = null;

    InputStream keyStream = getKeyStream();
    String keyStorePassword;

    try
    {
      keyStorePassword = GenericEncryption.decryptString(paramKSPassword);
    }
    catch(GenericEncryptionException e)
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

  protected KeyStore.PasswordProtection getKeyEntryPassword() throws GenericEncryptionException
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

abstract class FileKeyStoreManager extends KeyStoreManager {
  private String s3Bucket;
  public FileKeyStoreManager(String s3Bucket, String paramKSType, String paramKSFile, String paramKSPassword,
      String paramKEAlias, String paramKEPassword) throws Exception
  {
    super(paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);
    this.s3Bucket = s3Bucket;
  }

  protected InputStream getKeyStream() {
    AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    S3Object object = s3Client.getObject(s3Bucket, paramKSFile);
    return object.getObjectContent();
  }

  protected KeyStore.PasswordProtection getKeyEntryPassword() throws GenericEncryptionException
  {
    // Se a senha da chave estiver ausente usa a mesma do key store
    String password;
    if (paramKEPassword != null)
      password = paramKEPassword;
    else
      password = paramKSPassword;

    return new KeyStore.PasswordProtection(
        GenericEncryption.decryptString(password).toCharArray());
  }
}

class PKCS12Manager extends FileKeyStoreManager
{
  public PKCS12Manager(String s3Bucket, String paramKSType, String paramKSFile, String paramKSPassword,
      String paramKEAlias, String paramKEPassword) throws Exception
  {
    super(s3Bucket, paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);
  }

  public void validateParams() throws BadRequestException
  {
    if (paramKSFile == null || paramKSPassword == null)
      throw new BadRequestException(
          "PKCS12 key store requires all of the following params: " +
          "keystorefile, keystorepassword");
  }
}

class JKSManager extends FileKeyStoreManager
{
  public JKSManager(String s3Bucket, String paramKSType, String paramKSFile, String paramKSPassword,
      String paramKEAlias, String paramKEPassword) throws Exception
  {
    super(s3Bucket, paramKSType, paramKSFile, paramKSPassword, paramKEAlias, paramKEPassword);
  }

  public void validateParams() throws BadRequestException
  {
    if (paramKSFile == null || paramKSPassword == null)
      throw new BadRequestException(
          "JKS key store requires all of the following params: " +
          "keystorefile, keystorepassword");
  }
}

