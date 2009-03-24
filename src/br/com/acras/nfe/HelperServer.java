package br.com.acras.nfe;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

// Como usar uma instância de chave já existente com o Axis2:
// http://markmail.org/message/zfseoekxmvijjmiq?q=axis2+sslcontext

public class HelperServer
{
  public static void main(String[] args)
  {
    try
    {
      int port = 0;

      if (args.length == 0)
        port = 9990;
      else if (args.length == 2 && args[0].compareTo("-p") == 0)
      {
        try
        {
          port = Integer.parseInt(args[1]);          
        }
        catch(NumberFormatException e)
        {
          showUsage();
        }
      }
      else
        showUsage();
      
      startServer(port);
    }
    catch(Exception e)
    {
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  private static void showUsage()
  {
    System.err.println("Usage: java HelperServer [-p <port>]");
    System.exit(2);
  }
  
  private static void startServer(int port) throws IOException
  {
    Map<String, KeyEntryReference> keyEntryMap = new HashMap<String, KeyEntryReference>();
    
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    
    server.createContext(
        "/validate",
        new SchemaValidatorHandler());
    server.createContext(
        "/initkeystore",
        new KeyStoreInitializationHandler(keyEntryMap));
    server.createContext(
        "/sign",
        new SignHandler(keyEntryMap));
    server.createContext(
        "/initwsclient",
        new WebServiceClientInitializationHandler(keyEntryMap));
    server.createContext(
        "/invokews",
        new WebServiceInvokationHandler());
    server.createContext(
        "/exit",
        new ExitHandler(server));
    
    server.setExecutor(null);
    server.start();
  }
}

