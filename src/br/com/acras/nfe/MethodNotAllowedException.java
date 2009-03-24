package br.com.acras.nfe;

class MethodNotAllowedException extends HttpException
{
  public MethodNotAllowedException(String message)
  {
    super(message);
  }

  public MethodNotAllowedException(Exception exception)
  {
    super(exception);
  }

  protected int getResponseCode()
  {
    return 405;
  }
}

