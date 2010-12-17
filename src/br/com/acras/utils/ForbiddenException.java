package br.com.acras.utils;

public class ForbiddenException extends HttpException
{
  public ForbiddenException(String message)
  {
    super(message);
  }

  public ForbiddenException(Exception exception)
  {
    super(exception);
  }

  protected int getResponseCode()
  {
    return 403;
  }
}

