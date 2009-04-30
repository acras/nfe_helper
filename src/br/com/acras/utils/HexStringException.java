package br.com.acras.utils;

public class HexStringException extends Exception
{
  public HexStringException(String message)
  {
    super(message);
  }

  public HexStringException(Exception e)
  {
    super(e);
  }
}
