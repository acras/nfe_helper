package br.com.acras.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.net.URI;
import java.net.URLDecoder;

import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class CustomHttpExchange
{
  private ByteArrayOutputStream byteArrayOutputStream;

  private HttpExchange httpExchange;
  private InputStream inputStream;
  private PrintStream printStream;
  private Headers headers;
  private Map<String, String> parameters;
  
  public CustomHttpExchange(HttpExchange httpExchange) throws IOException
  {
    this.httpExchange = httpExchange;
    // Lê toda a entrada caso necessário e armazena num buffer separado, tirando a
    // responsabilidade das classes derivadas do CustomHttpHandler de ler o RequestBody
    initInputStream();
    initPrintStream();
    
    headers = httpExchange.getResponseHeaders();
    parameters = decodeURIQuery(httpExchange.getRequestURI());
  }
  
  public void checkMethod(String expectedMethod) throws MethodNotAllowedException
  {
    String s = httpExchange.getRequestMethod();
    if (!expectedMethod.equals(s))
    {
      addHeader("Allow", expectedMethod);
      throw new MethodNotAllowedException("Method not allowed: " + s);
    }
  }
  
  public void finish(int responseCode) throws IOException
  {
    httpExchange.sendResponseHeaders(responseCode, byteArrayOutputStream.size());
    OutputStream responseBody = httpExchange.getResponseBody();
    byteArrayOutputStream.writeTo(responseBody);
    responseBody.close();

    httpExchange.close();
  }
  
  public String tryParameter(String name)
  {
    return parameters.get(name);
  }
  
  public String getParameter(String name) throws BadRequestException
  {
    String result = tryParameter(name);
    if (result == null)
      throw new BadRequestException("Missing parameter: " + name);
    return result;
  }
  
  public Map<String, String> getParameters()
  {
    return parameters;
  }
  
  public void addHeader(String key, String value)
  {
    headers.add(key, value);
  }
  
  public InputStream getInputStream()
  {
    return inputStream;
  }
  
  public PrintStream getPrintStream()
  {
    return printStream;
  }
  
  private void initInputStream() throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    if (httpExchange.getRequestMethod().equals("POST"))
    {
      InputStream in = httpExchange.getRequestBody();
      try
      {
        byte[] buffer = new byte[1024];
        while (true)
        {
          int c = in.read(buffer);
          if (c == -1)
            break;
          out.write(buffer, 0, c);
        }
      }
      finally
      {
        in.close();
      }
    }

    this.inputStream = new ByteArrayInputStream(out.toByteArray());   
  }

  private void initPrintStream()
  {
    byteArrayOutputStream = new ByteArrayOutputStream();
    try
    {
      printStream = new PrintStream(byteArrayOutputStream, false, "UTF-8");
    }
    catch(UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }
  }
  
  private Map<String, String> decodeURIQuery(URI uri)
  {
    Map<String, String> result = new HashMap<String, String>();
    
    String query = uri.getRawQuery();
    if (query != null)
    {
      String params[] = query.split("\\&");
      for (String item: params)
        if (!item.isEmpty())
        {
          String paramParts[] = item.split("\\=", 2);
  
          String key = paramParts[0];
          String value = "";
          if (paramParts.length > 1)
            value = paramParts[1]; 
  
          try
          {
            result.put(URLDecoder.decode(key, "UTF-8"), URLDecoder.decode(value, "UTF-8"));
          }
          catch(UnsupportedEncodingException e)
          {
            throw new RuntimeException(e);
          }
        }
    }
      
    return result;
  }
}
