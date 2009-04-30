import static org.junit.Assert.*;
import org.junit.Test;

import br.com.acras.utils.GenericEncryption;
import br.com.acras.utils.GenericEncryptionException;

public class GenericEncryptionTest
{
  @Test
  public void testDecryptEmptyString() throws GenericEncryptionException
  {
    String s = GenericEncryption.decryptString("35db101b3f060b3fe6c79a94a367dd9a");
    assertEquals("", s);
  }
  
  @Test
  public void testDecryptSmallString() throws GenericEncryptionException
  {
    String s = GenericEncryption.decryptString("c42764600b0ec9b34e47e78fe0a61d41");
    assertEquals("abc", s);
  }
  
  @Test
  public void testDecryptLongString() throws GenericEncryptionException
  {
    String s = GenericEncryption.decryptString(
        "5b3fb590bac6dca8fa08e152ac9c7e0d7b2922e22cbaeedcd30b7eb608cb2a09" +
        "4fc2aaf15321da767bc8f9bccd7f5f0677b2b8bebabc30de85ec2ceeba2f4b4c");
    assertEquals("This is a long test string to be encrypted.", s);
  }
  
  @Test
  public void testDecryptStringWithLengthMultipleOf16() throws GenericEncryptionException
  {
    String s = GenericEncryption.decryptString(
        "13938cb27af12fabe8632b6ab6e844259f4469a6f42d589fafdf2f929548a426");
    assertEquals("string", s);
  }
  
  /* Testa se ocorre exceção quando o bloco é válido mas a string
     descriptografada não tem o prefixo e o sufixo esperados */
  @Test(expected = GenericEncryptionException.class)
  public void testDecryptStringWithBadPrefixOrSuffix() throws GenericEncryptionException
  {
    try
    {
      GenericEncryption.decryptString("a68bc118bd8d13644ea8c04c1ce6c39d");
    }
    catch (GenericEncryptionException e)
    {
      System.out.println(e.getMessage());
      throw e;
    }
  }

  /* Testa se ocorre exceção quando a string foi corrompida */
  @Test(expected = GenericEncryptionException.class)
  public void testDecryptCorruptedString() throws GenericEncryptionException
  {
    // String testada é a mesma do teste testDecryptSmallString, porém com o
    // último caractere trocado
    try
    {
      GenericEncryption.decryptString("c42764600b0ec9b34e47e78fe0a61d42");
    }
    catch (GenericEncryptionException e)
    {
      System.out.println(e.getMessage());
      throw e;
    }
  }

  /* Testa se ocorre exceção quando o tamanho não é múltiplo do tamanho do
     bloco */
  @Test(expected = GenericEncryptionException.class)
  public void testDecryptBadBlock() throws GenericEncryptionException
  {
    try
    {
      GenericEncryption.decryptString("c42764600b0ec9b3");
    }
    catch (GenericEncryptionException e)
    {
      System.out.println(e.getMessage());
      throw e;
    }
  }
}
