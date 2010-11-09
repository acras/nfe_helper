package br.com.acras.nfe;

import java.net.SocketTimeoutException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class WebServiceInvokationHandler extends CustomHttpHandler
{
  protected void handle(CustomHttpExchange exchange) throws Exception
  {
    String endpointURL = exchange.getParameter("endpointurl");
    String namespace = exchange.getParameter("namespace");
    String serviceName = exchange.getParameter("servicename");
    String operationName = exchange.getParameter("operationname");

    QName serviceQN = new QName(namespace, serviceName);
    QName portQN = new QName(namespace, serviceName + "Port");

    Service service = Service.create(serviceQN);
    service.addPort(portQN, SOAPBinding.SOAP11HTTP_BINDING, endpointURL);
    
    Dispatch<SOAPMessage> dispatch =
        service.createDispatch(portQN, SOAPMessage.class, Service.Mode.MESSAGE);
        
    Map<String, Object> ctxt = ((BindingProvider) dispatch).getRequestContext();
    ctxt.put(BindingProvider.SOAPACTION_USE_PROPERTY, true);
    ctxt.put(BindingProvider.SOAPACTION_URI_PROPERTY, namespace + operationName);

    invokeService(dispatch, exchange.getInputStream(), exchange.getPrintStream());
  }
  
  protected String getAllowedMethod()
  {
    return "POST";
  }
  
  private void invokeService(Dispatch<SOAPMessage> dispatch,
      InputStream inputBody, OutputStream outputBody) throws Exception
  {
    MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
    
    SOAPMessage request = mf.createMessage();
    SOAPBody requestBody = request.getSOAPPart().getEnvelope().getBody();
    
    Document doc = readDocument(inputBody);

    requestBody.addDocument(doc);
    request.saveChanges();
    
    // DEBUG
    request.writeTo(System.out);
    System.out.println("****");
    // /DEBUG
    
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
    
    Node responseBody = response.getSOAPBody();
    
    Node responseNode = responseBody.getFirstChild();
    writeDocument(responseNode, outputBody);
  }
  
  private Document readDocument(InputStream parameters) throws Exception
  {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = null;
    try
    {
      doc = db.parse(parameters);
    }
    catch(SAXException e)
    {
      throw new BadRequestException("Parameters in message body are not properly encoded");
    }

    return doc;
  }
  
  private void writeDocument(Node node, OutputStream output)
      throws TransformerConfigurationException, TransformerException
  {
    TransformerFactory tf = TransformerFactory.newInstance();
    tf.newTransformer().transform(
        new DOMSource(node),
        new StreamResult(output));
  }
}
