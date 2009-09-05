package br.com.acras.nfe;

import java.io.IOException;

import java.util.Date;

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
        long timerBefore = System.nanoTime();
        
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

        long timerAfter = System.nanoTime();
        
        System.err.print("=> ");
        System.err.print(new Date());
        System.err.print(" | ");
        System.err.print(t.getRequestURI());
        System.err.print(" | ");
        System.err.printf("%.5f s", (timerAfter - timerBefore) * 1e-9);
        System.err.print(" | ");
        System.err.print(responseCode);        
        System.err.print("\n");
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

