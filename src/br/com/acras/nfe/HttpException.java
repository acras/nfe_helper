package br.com.acras.nfe;

abstract class HttpException extends Exception
{
  public HttpException(String message)
  {
    super(message);
  }
  
  public HttpException(Exception exception)
  {
    super(exception.getClass().getName() + ": " + exception.getMessage());
  }
  
  abstract protected int getResponseCode();
}
