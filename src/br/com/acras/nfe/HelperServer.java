package nfe;

import java.io.File;
import java.io.IOException;

import java.security.Provider;
import java.security.Security;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HttpsURLConnection;

import com.sun.net.httpserver.HttpServer;

import utils.*;

public class HelperServer
{
  private static final String connectTimeoutProp = "sun.net.client.defaultConnectTimeout";
  private static final String readTimeoutProp = "sun.net.client.defaultReadTimeout";

  int port = 0;
  boolean enableSSLChecks = true;
  String baseDirectory = ".";
  String pkcs11Config = null;
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
    this.port = 9990;
    this.baseDirectory = ".";

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
      else if (args[k].equals("-w") && k + 1 < cnt)
      {
        this.baseDirectory = args[k + 1];
        k += 2;
      }
      else if (args[k].equals("--pkcs11") && k + 1 < cnt)
      {
        this.pkcs11Config = args[k + 1];
        k += 2;
      }
      else if (args[k].equals("--disable-ssl-checks"))
      {
        this.enableSSLChecks = false;
        k += 1;
      }
      else
        showUsage();
    }

    configTimeouts();
    configPKCS11();
    configSSLChecks();
  }

  public void start() throws Exception
  {
    Map<String, KeyEntryReference> keyEntryMap = new HashMap<String, KeyEntryReference>();

    this.httpServer = HttpServer.create(
        new InetSocketAddress("127.0.0.1", this.port), 0);

    try
    {
      this.httpServer.createContext(
          "/validate",
          new SchemaValidatorHandler(baseDirectory));
      this.httpServer.createContext(
          "/initkeystore",
          new KeyStoreInitializationHandler(baseDirectory, keyEntryMap));
      this.httpServer.createContext(
          "/sign",
          new SignHandler(keyEntryMap));
      this.httpServer.createContext(
          "/invokews",
          new WebServiceInvokationHandler(keyEntryMap));

      this.httpServer.setExecutor(Executors.newFixedThreadPool(10));
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
      final HelperServer s = new HelperServer();
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
    System.err.println("  java HelperServer [-p <port>] [-w <working dir>]");
    System.err.println("      [--pkcs11 <PKCS11 config file>] [--disable-ssl-checks]");
    System.exit(2);
  }

  private static void configTimeouts()
  {
    // Se não for especificado vamos definir um timeout razoável para as
    // operações, para evitar que uma demora no acesso ao servidor da SEFA não
    // esgote o pool e impeça tarefas como validação e assinatura de xmls.
    //
    // No caso do timeout de conexão o valor pode ser bem baixo pois, se a
    // conexão não foi efetuada, sabemos que nenhuma informação chegou à SEFA.
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
  }

  private void configPKCS11()
  {
    if (pkcs11Config != null)
    {
      Provider provider = new sun.security.pkcs11.SunPKCS11(pkcs11Config);
      Security.addProvider(provider);
    }
  }

  private void configSSLChecks()
  {
    if (!enableSSLChecks)
    {
      HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
          {
            public boolean verify(String hostname, SSLSession session)
            {
              System.err.printf("=> WARNING: skipping SSL validation for host %s.\n", hostname);
              return true;
            }
          });
    }
  }
}
