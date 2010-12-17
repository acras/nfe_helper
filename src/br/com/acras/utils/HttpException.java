package br.com.acras.utils;

abstract public class HttpException extends Exception
{
  public HttpException(String message)
  {
    super(message);
  }
  
  public HttpException(Throwable cause)
  {
    super(cause.getClass().getName() + ": " + cause.getMessage());
  }

  abstract protected int getResponseCode();
}
