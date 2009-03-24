import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

abstract class CustomHttpHandler implements HttpHandler
{
  public void handle(HttpExchange t) throws IOException
  {
    try
    {
      int responseCode = 500;
      
      CustomHttpExchange exchange = new CustomHttpExchange(t);
      try
      {    
        try
        {
          exchange.checkMethod(getAllowedMethod());
          handle(exchange);
          responseCode = 200;
        }
        catch(HttpException e)
        {
          responseCode = e.getResponseCode();
          exchange.getPrintStream().println(e.getMessage());
        }
        catch(Exception e)
        {
          responseCode = 500;
          e.printStackTrace(exchange.getPrintStream());
        }
      }
      finally
      {
        exchange.finish(responseCode);
      }
    }
    catch(IOException e)
    {
      e.printStackTrace();
      throw e;
    }
  }

  abstract protected void handle(CustomHttpExchange exchange) throws Exception;

  abstract protected String getAllowedMethod();
}

