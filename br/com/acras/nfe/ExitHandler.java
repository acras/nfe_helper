import com.sun.net.httpserver.HttpServer;

/*
  Chuncho para parar o servidor no Windows. Se isso não for usado há duas alternativas:
    (a) Matar o processo com um SIG_KILL: acho bem feio fazer assim, mais feio do que o chuncho
        abaixo
    (b) Implementar o modo "elegante" de terminar um processo no Windows (o equivalente a
        mandar um Control-C para um processo de linha de comando), usando a API
        CreateRemoteThread para criar, no processo do servidor, uma thread que chama
        ExitProcess (http://www.ddj.com/dept/windows/documents/s=7285/wdj9907c/9907c.htm): o
        método ainda merece ser melhor investigado pois, num teste rápido, ele não pareceu 
        permitir a finalização ordenada do processo 
*/
class ExitHandler extends CustomHttpHandler
{
  HttpServer server;
  
  public ExitHandler(HttpServer server)
  {
    this.server = server;
  }
  
  protected void handle(CustomHttpExchange exchange) throws Exception
  {
    Thread p = new HttpServerStopperThread(server);
    p.start();
    exchange.getPrintStream().println("HTTP server termination scheduled successfully");
  }
  
  protected String getAllowedMethod()
  {
    return "GET";
  }
  
  private class HttpServerStopperThread extends Thread
  {
    HttpServer server;
    
    HttpServerStopperThread(HttpServer server)
    {
      this.server = server;
    }
    
    public void run()
    {
      server.stop(3);
    }
    
  }
}

