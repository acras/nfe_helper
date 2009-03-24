class GatewayTimeoutException extends HttpException
{
  public GatewayTimeoutException(String message)
  {
    super(message);
  }

  public GatewayTimeoutException(Exception exception)
  {
    super(exception);
  }

  protected int getResponseCode()
  {
    return 504;
  }
}

