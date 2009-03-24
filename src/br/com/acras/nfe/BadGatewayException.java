package br.com.acras.nfe;

class BadGatewayException extends HttpException
{
  public BadGatewayException(String message)
  {
    super(message);
  }

  public BadGatewayException(Exception exception)
  {
    super(exception);
  }

  protected int getResponseCode()
  {
    return 502;
  }
}

