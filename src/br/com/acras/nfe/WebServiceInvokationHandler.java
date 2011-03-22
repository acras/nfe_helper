package br.com.acras.nfe;

import java.net.SocketTimeoutException;

import java.io.InputStream;
import java.io.OutputStream;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;

import br.com.acras.utils.*;

class WebServiceInvokationHandler extends CustomHttpHandler
{
  Map<String, KeyEntryReference> keyEntryMap;
  
  public WebServiceInvokationHandler(Map<String, KeyEntryReference> keyEntryMap)
  {
    this.keyEntryMap = keyEntryMap;
  }
  
  protected void handle(CustomHttpExchange exchange) throws Exception
  {
    SSLContext sslContext = getSSLContext(exchange.getParameter("keystoreid"));

    String endpointURL = exchange.getParameter("endpointurl");
    String namespace = exchange.getParameter("namespace");
    String serviceName = exchange.getParameter("servicename");
    String operationName = exchange.getParameter("operationname");
    String soapVersion = exchange.tryParameter("soapversion");

    QName serviceQN = new QName(namespace, serviceName);
    QName portQN = new QName(namespace, serviceName + "Port");
    
    String soapBinding = SOAPBinding.SOAP11HTTP_BINDING;
    String soapProtocol = SOAPConstants.SOAP_1_1_PROTOCOL;
    String contentType = "text/xml; charset=utf-8";
    if ("12".equals(soapVersion))
    {
      soapBinding = SOAPBinding.SOAP12HTTP_BINDING;
      soapProtocol = SOAPConstants.SOAP_1_2_PROTOCOL;
      contentType = "application/soap+xml; charset=utf-8";
    }

    Service service = Service.create(serviceQN);
    service.addPort(portQN, soapBinding, endpointURL);
    
    Dispatch<SOAPMessage> dispatch =
        service.createDispatch(portQN, SOAPMessage.class, Service.Mode.MESSAGE);

    Map<String, Object> ctxt = ((BindingProvider) dispatch).getRequestContext();
    
    ctxt.put(BindingProvider.SOAPACTION_USE_PROPERTY,
        true);
    ctxt.put(BindingProvider.SOAPACTION_URI_PROPERTY,
        includeTrailingPathDelimiter(namespace) + operationName);
    ctxt.put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory",
        sslContext.getSocketFactory());

    invokeService(dispatch, soapProtocol, contentType,
        exchange.getInputStream(), exchange.getPrintStream());
  }
  
  private String includeTrailingPathDelimiter(String p)
  {
    String result = p;
    int len = p.length();
    
    if (len > 0 && p.charAt(len - 1) != '/')
      result += "/";
    
    return result;
  }
  
  protected String getAllowedMethod()
  {
    return "POST";
  }
  
  private void invokeService(Dispatch<SOAPMessage> dispatch, String protocol,
      String contentType, InputStream inputBody, OutputStream outputBody) throws Exception
  {
    MessageFactory mf = MessageFactory.newInstance(protocol);

    MimeHeaders headers = new MimeHeaders();
    headers.addHeader("Content-Type", contentType);
    
    SOAPMessage request = mf.createMessage(headers, inputBody);
    SOAPMessage response;

    try
    {
      response = dispatch.invoke(request);
    }
    catch(WebServiceException e)
    {
      Throwable cause = e;
      while (null != (cause = cause.getCause()))
      {
        if (cause instanceof SocketTimeoutException)
          throw new GatewayTimeoutException(e);
      }

      throw new BadGatewayException(e);
    }
    
    response.writeTo(outputBody);
  }
  
  private SSLContext getSSLContext(String keyStoreId) throws NotFoundException,
      KeyManagementException, NoSuchAlgorithmException
  {
    KeyEntryReference keRef = keyEntryMap.get(keyStoreId);
    if (keRef == null)
      throw new NotFoundException("No key entry with id " + keyStoreId +
          " was found (not initialized?)");
      
    SSLContext context = SSLContext.getInstance("SSL");
    context.init(createKeyManagers(keRef), null, null);
   
    return context;
  }

  private KeyManager[] createKeyManagers(KeyEntryReference keRef)
  {
    CustomKeyManager keyManager =
        new CustomKeyManager(null, keRef.getKeyStore(), keRef.getAlias(), keRef.getKeyEntry());
    return new KeyManager[] { keyManager };
  }
}
