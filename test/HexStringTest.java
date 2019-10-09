import static org.junit.Assert.*;
import org.junit.Test;

import utils.HexString;
import utils.HexStringException;

public class HexStringTest
{
  @Test
  public void testDecodeSingleByte() throws HexStringException
  {
    byte[] b = HexString.decode("A0");
    assertEquals(1, b.length);
    assertEquals((byte) 0xA0, b[0]);
  }

  @Test
  public void testDecodeMultipleBytes() throws HexStringException
  {
    byte[] b = HexString.decode("F0E1D2C3B4A5");
    assertEquals(6, b.length);
    assertEquals((byte) 0xF0, b[0]);
    assertEquals((byte) 0xE1, b[1]);
    assertEquals((byte) 0xD2, b[2]);
    assertEquals((byte) 0xC3, b[3]);
    assertEquals((byte) 0xB4, b[4]);
    assertEquals((byte) 0xA5, b[5]);
  }

  @Test
  public void testDecodeLowerCase() throws HexStringException
  {
    byte[] b = HexString.decode("fedc");
    assertEquals(2, b.length);
    assertEquals((byte) 0xFE, b[0]);
    assertEquals((byte) 0xDC, b[1]);
  }

  @Test(expected = HexStringException.class)
  public void testDecodeBadChar()  throws HexStringException
  {
    byte[] b = HexString.decode("fedc..");
  }

  @Test(expected = HexStringException.class)
  public void testDecodeBadStringLength()  throws HexStringException
  {
    byte[] b = HexString.decode("12345");
  }

  @Test
  public void testEncodeSingleByte()
  {
    byte[] b = new byte[] { (byte) 0xF0 };

    String s = HexString.encode(b);
    assertEquals("f0", s);
  }

  @Test
  public void testEncodeMultipleBytes()
  {
    byte[] b = new byte[] { (byte) 0xF0, (byte) 0xE1, (byte) 0xD2 };

    String s = HexString.encode(b);
    assertEquals("f0e1d2", s);
  }
}