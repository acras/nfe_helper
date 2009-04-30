package br.com.acras.utils;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class GenericEncryption
{
  private static final byte[] initVector = new byte[] { -110, 4, -4, -52, -127,
      -91, 11, 67, -108, -80, -11, -97, 25, -65, 49, 6 };
  private static final byte[] key = new byte[] { 45, 35, 108, 75, 58, 34, -13,
      -66, 95, -71, -14, 69, 57, 23, 41, -118 };
  private static final String padString = "{pad}"; 
      
  private static Cipher getAESCipher(int opmode)
      throws NoSuchAlgorithmException, NoSuchPaddingException,
      InvalidKeyException, InvalidAlgorithmParameterException
  {
    SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
    IvParameterSpec ivSpec = new IvParameterSpec(initVector);
      
    Cipher result = Cipher.getInstance("AES/CBC/PKCS5Padding");
    result.init(opmode, keySpec, ivSpec);
    
    return result;
  };
      
  private static void throwInvalidStringError() throws GenericEncryptionException
  {
    throw new GenericEncryptionException("Invalid encrypted string");
  }
  
  public static String decryptString(String str) throws GenericEncryptionException
  {
    byte[] input = HexString.decode(str);
    byte[] output;
    
    try
    {
      Cipher cipher = getAESCipher(Cipher.DECRYPT_MODE);
      output = cipher.doFinal(input);
    }
    catch(Exception e)
    {
      throw new GenericEncryptionException(e);
    }
    
    String s = new String(output);
    int c = padString.length();
    
    if (s.length() < 2 * c || !s.startsWith(padString) || !s.endsWith(padString))
      throwInvalidStringError();
    
    return s.substring(c, s.length() - c);
  }
  
  public static String encryptString(String decryptedStr) throws GenericEncryptionException
  {
    byte output[];
    String s = padString + decryptedStr + padString;
    
    try
    {
      Cipher cipher = getAESCipher(Cipher.ENCRYPT_MODE);
      output = cipher.doFinal(s.getBytes("UTF-8"));
    }
    catch(Exception e)
    {
      throw new GenericEncryptionException(e);
    }
    
    for (int i = 0; i < output.length; i++)
    {
      int j = output[i] & 0xff;       
      System.out.print(Character.forDigit(j / 16, 16));
      System.out.print(Character.forDigit(j % 16, 16));
    }
    System.out.print("\n");
    
    return "";
  }
}
