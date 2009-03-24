class NotFoundException extends HttpException
{
  public NotFoundException(String message)
  {
    super(message);
  }
  
  public NotFoundException(Exception exception)
  {
    super(exception);
  }
  
  protected int getResponseCode()
  {
    return 404;
  }
}

