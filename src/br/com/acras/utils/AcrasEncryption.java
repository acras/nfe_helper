import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AcrasEncryption
{
  private static final byte[] initVector = new byte[] { -110, 4, -4, -52, -127,
      -91, 11, 67, -108, -80, -11, -97, 25, -65, 49, 6 };
  private static final byte[] key = new byte[] { 45, 35, 108, 75, 58, 34, -13,
      -66, 95, -71, -14, 69, 57, 23, 41, -118 };
  private static final String padString = "{pad}"; 
      
  private static Cipher getAESCipher()
      throws NoSuchAlgorithmException, NoSuchPaddingException,
      InvalidKeyException, InvalidAlgorithmParameterException
  {
    SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
    IvParameterSpec ivSpec = new IvParameterSpec(initVector);
      
    Cipher result = Cipher.getInstance("AES/CBC/PKCS5Padding");
    result.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
    
    return result;
  };
      
  private static byte[] packStringOfHexes(String stringOfHexes)
  {
    char[] strChars = stringOfHexes.toCharArray();
    byte[] result = new byte[strChars.length / 2];
    
    for (int i = 0; i < result.length; i++)
      result[i] = (byte) (Character.digit(strChars[i * 2], 16) * 16 +
                          Character.digit(strChars[i * 2 + 1], 16)); 
    
    return result;
  }
  
  private static void throwInvalidStringError() throws AcrasEncryptionException
  {
    throw new AcrasEncryptionException("Invalid encrypted string");
  }
  
  private static String decryptRawString(String str) throws AcrasEncryptionException
  {
    byte[] rawInput = packStringOfHexes(str);
    byte[] input = new byte[rawInput.length - 1];
    
    // O ruby-aes adiciona um byte que representa o número de caracteres usados
    // como "padding". Como o Java trata a string cifrada apenas como blocos 
    // devemos remover nós mesmos esses caracteres
    System.arraycopy(rawInput, 0, input, 0, input.length);
    int padCount = rawInput[rawInput.length - 1];
    
    byte[] output = new byte[input.length];
    
    try
    {
      Cipher cipher = getAESCipher();
      
      int outputLength = cipher.update(input, 0, input.length, output, 0);
      outputLength += cipher.doFinal(output, outputLength);
    }
    catch(Exception e)
    {
      throw new AcrasEncryptionException(e.getClass().getName() + ": " + e.getMessage());
    }
    
    String outputStr = new String(output);

    if (outputStr.length() < padCount)
      throwInvalidStringError();
    
    return outputStr.substring(0, outputStr.length() - padCount);
  }
  
  public static String decryptString(String encryptedStr) throws AcrasEncryptionException
  {
    String s = decryptRawString(encryptedStr);

    String pad = padString; 
    int c = padString.length();
    
    if (s.length() < 2 * c || !s.startsWith(pad) || !s.endsWith(pad))
      throwInvalidStringError();
    
    return s.substring(c, s.length() - c);
  }
}

