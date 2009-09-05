package br.com.acras.nfe;

abstract class HttpException extends Exception
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
