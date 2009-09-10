package br.com.acras.nfe;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class HelperServer
{
  private static final String connectTimeoutProp = "sun.net.client.defaultConnectTimeout";
  private static final String readTimeoutProp = "sun.net.client.defaultReadTimeout";
  
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

      // Se não for especificado vamos definir um timeout razoável para as
      // operações, para evitar que uma demora no acesso ao servidor da SEFA não
      // esgote o pool e impeça tarefas como validação e assinatura de xmls.
      //
      // No caso do timeout de conexão o valor pode ser bem baixo pois, se a
      // conexão não foi efetuada sabemos que nenhuma informação chegou à SEFA.
      // Para o timeout de leitura existe esse problema adicional: se o valor
      // for baixo demais aumentará a chance de que, durante uma conexão lenta,
      // um pedido chegue à SEFA mas esta aplicação aborte antes da conexão
      // terminar espontaneamente.
      //
      // De qualquer forma é importante que o timeout do cliente desta aplicação
      // seja grande o suficiente para não abortar a conexão antes que esta
      // aborte a conexão com a SEFA.
      //
      // (Foi escolhido um timeout de 30 segundos para leitura porque em
      // Set/2009 notou-se que em alguns períodos as requisições feitas à SEFA
      // chegavam a demorar 20 segundos.)
      if (null == System.getProperty(connectTimeoutProp))
        System.setProperty(connectTimeoutProp, "5000");
      if (null == System.getProperty(readTimeoutProp))
        System.setProperty(readTimeoutProp, "30000");
      
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
    
    server.setExecutor(Executors.newFixedThreadPool(10));
    server.start();
  }
}
