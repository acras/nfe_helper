package br.com.acras.nfe;

import java.net.SocketTimeoutException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

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
    ctxt.put(BindingProvider.SOAPACTION_URI_PROPERTY, namespace + "/" + operationName);

    String response = invokeService(
        dispatch, namespace, operationName, exchange.getInputStream());
    
    exchange.getPrintStream().println(response);
  }
  
  protected String getAllowedMethod()
  {
    return "POST";
  }
  
  private String invokeService(Dispatch<SOAPMessage> dispatch, String namespace,
      String operationName, InputStream httpBody) throws Exception
  {
    MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
    
    SOAPMessage request = mf.createMessage();
    SOAPBody requestBody = request.getSOAPPart().getEnvelope().getBody();
    
    String[] params = readParameters(httpBody);
    String header = params[0];
    String data = params[1];

    SOAPElement operation = requestBody.addChildElement(operationName, "", namespace);
    SOAPElement headerNode = operation.addChildElement("nfeCabecMsg");
    headerNode.addTextNode(header);
    SOAPElement dataNode = operation.addChildElement("nfeDadosMsg");
    dataNode.addTextNode(data);
    request.saveChanges();
    
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
    if (responseNode == null ||
        !responseNode.getNamespaceURI().equals(namespace) ||
        responseNode.getNodeType() != Node.ELEMENT_NODE)
      throwInvalidResponse(responseNode);

    Node resultNode = responseNode.getFirstChild();
    if (resultNode == null || resultNode.getNodeType() != Node.ELEMENT_NODE)
      throwInvalidResponse(responseNode);
 
    String result = getNodeText(resultNode);
    
    if (result.isEmpty())
      throwInvalidResponse(responseNode);
    
    return result;
  }
  
  private String[] readParameters(InputStream parameters) throws Exception
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
      checkCondition(false);
    }

    Node rootNode = doc.getDocumentElement();
    checkCondition(rootNode.getNodeType() == Node.ELEMENT_NODE);
    Element rootElement = (Element) rootNode;
    
    NodeList headerParam = rootElement.getElementsByTagName("headerParam");
    checkCondition(headerParam.getLength() == 1);
    NodeList dataParam = rootElement.getElementsByTagName("dataParam");
    checkCondition(dataParam.getLength() == 1);
    
    String headerParamText = getNodeText(headerParam.item(0));
    checkCondition(!headerParamText.isEmpty());
    String dataParamText = getNodeText(dataParam.item(0));
    checkCondition(!dataParamText.isEmpty());
    
    return new String[] { headerParamText, dataParamText };
  }
  
  private void checkCondition(boolean val) throws BadRequestException
  {
    if (!val)
      throw new BadRequestException("Parameters in message body are not properly encoded");
  }
  
  private void throwInvalidResponse(Node responseNode) throws Exception
  {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    if (responseNode != null)
    {
      TransformerFactory tf = TransformerFactory.newInstance();
      tf.newTransformer().transform(new DOMSource(responseNode), new StreamResult(os));
    }
    throw new BadGatewayException("Invalid response:\n" + os.toString());
  }
  
  private String getNodeText(Node node)
  {
    String result = "";
    
    Node textNode = node.getFirstChild();
    while (textNode != null)
    {
      if (textNode.getNodeType() == Node.TEXT_NODE)
        result += textNode.getNodeValue();
      textNode = textNode.getNextSibling();
    }
    
    return result;
  }
}
