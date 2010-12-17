package br.com.acras.utils;

public class BadRequestException extends HttpException
{
  public BadRequestException(String message)
  {
    super(message);
  }

  public BadRequestException(Exception exception)
  {
    super(exception);
  }

  protected int getResponseCode()
  {
    return 400;
  }
}

