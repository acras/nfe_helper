package br.com.acras.utils;

public class HexString
{
  private static int getCharValue(char[] strChars, int pos) throws HexStringException
  {
    int result = Character.digit(strChars[pos], 16);
    if (result == -1)
      throw new HexStringException("Invalid character at position " + Integer.toString(pos));
    return result;
  }

  public static byte[] decode(String str) throws HexStringException
  {
    char[] strChars = str.toCharArray();
    
    int len = strChars.length;
    if (len % 2 != 0)
      throw new HexStringException("String length is not multiple of 2");
    
    byte[] result = new byte[len / 2];
    
    for (int i = 0; i < result.length; i++)
    {
      int d1 = getCharValue(strChars, i * 2);
      int d2 = getCharValue(strChars, i * 2 + 1);
      result[i] = (byte) (d1 * 16 + d2);
    }
    
    return result;
  }
  
  public static String encode(byte[] bytes)
  {
    StringBuffer strBuf = new StringBuffer();
    
    for (int i = 0; i < bytes.length; i++)
    {
      strBuf.append(Character.forDigit((bytes[i] >> 4) & 0x0F, 16));
      strBuf.append(Character.forDigit((bytes[i]) & 0x0F, 16));
    }
    
    return strBuf.toString();
  }
}