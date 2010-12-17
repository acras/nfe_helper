package br.com.acras.report;

import java.io.IOException;

import java.util.concurrent.Executors;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class JasperServer
{
  int port = 0;
  String baseDirectory = ".";
  HttpServer httpServer = null;
  
  /**
    * Os métodos init(), start(), stop(), destroy() constituem a interface que permite à esta
    * classe ser lançada pelo jsvc (http://commons.apache.org/daemon/jsvc.html). Estas e o
    * restante do código foram escritos de forma que o servidor possa ser chamado tanto como
    * daemon (Unix) quando como console. No Windows não é necessário código especial porque o
    * jsl.exe (http://92.51.133.148/jsl/howto.html) já fica feliz com o main().
    */
  public void init(String[] args) throws Exception
  {
    this.port = 9980;
    
    int k = 0;
    int cnt = args.length;
    
    while (k < cnt)
    {
      if (args[k].equals("-p") && k + 1 < cnt)
      {
        try
        {
          this.port = Integer.parseInt(args[k + 1]);
        }
        catch(NumberFormatException e)
        {
          showUsage();
        }
        k += 2;
      }
      else
        showUsage();
    }
  }
  
  public void start() throws Exception
  {
    this.httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);

    try
    {
      this.httpServer.createContext("/generate", new JasperReportHandler());
      
      this.httpServer.setExecutor(Executors.newFixedThreadPool(2));
      this.httpServer.start();
    }
    catch (Exception e)
    {
      this.httpServer = null;
      throw e;
    }
  }

  public void stop() throws Exception
  {
    try
    {
      this.httpServer.stop(3);
    }
    finally
    {
      this.httpServer = null;
    }
  }
  
  public void destroy() throws Exception
  {
    
  }
  
  public static void main(String[] args)
  {
    try
    {
      final JasperServer s = new JasperServer();
      s.init(args);
      s.start();
    }
    catch(Exception e)
    {
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  private static void showUsage()
  {
    System.err.println("Usage:");
    System.err.println("  java br.com.acras.JasperServer [-p <port>]");
    System.exit(2);
  }
}
