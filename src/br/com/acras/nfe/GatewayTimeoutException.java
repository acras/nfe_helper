package br.com.acras.nfe;

class GatewayTimeoutException extends HttpException
{
  public GatewayTimeoutException(String message)
  {
    super(message);
  }

  public GatewayTimeoutException(Throwable cause)
  {
    super(cause);
  }

  protected int getResponseCode()
  {
    return 504;
  }
}

