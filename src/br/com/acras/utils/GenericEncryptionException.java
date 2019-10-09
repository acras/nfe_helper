package utils;

public class GenericEncryptionException extends Exception
{
  public GenericEncryptionException(String message)
  {
    super(message);
  }

  public GenericEncryptionException(Exception e)
  {
    super(e);
  }
}
