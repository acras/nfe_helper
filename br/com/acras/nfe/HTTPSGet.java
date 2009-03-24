import java.net.*;
import java.io.*;
import java.security.*;

import javax.net.ssl.*;

import javax.xml.namespace.*;
import javax.xml.soap.*;
import javax.xml.ws.*;
import javax.xml.ws.soap.*;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

// Exemplo construído com a ajuda de:
//   http://e-docs.bea.com/wls/docs81/security/SSL_client.html#1029618
//   http://markmail.org/message/zfseoekxmvijjmiq
//   http://hc.apache.org/httpclient-3.x/sslguide.html
//   http://www.angelfire.com/or/abhilash/site/articles/jsse-km/customKeyManager.html
//   http://publib.boulder.ibm.com/infocenter/wasinfo/v7r0/index.jsp?topic=
//     /com.ibm.websphere.express.iseries.doc/info/iseriesexp/ae/twbs_jaxwsdynclient.html

public class HTTPSGet
{
  public static void main(String[] args)
  {
    try
    {
      connect(args[0], args[1], args[2], args[3]);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  private static TrustManager[] createTrustManagers() throws Exception
  {
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(
        new FileInputStream("../soapclient/certificates/trusted.jks"),
        "acrasnfe".toCharArray());
    
    TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    tmfactory.init(ks);
    
    return tmfactory.getTrustManagers();
  }
  
  private static void connect(String endpointURL, String namespace, String serviceNameStr,
      String messageNodeName) throws Exception
  {
    SSLContext context = SSLContext.getInstance("SSL");
    
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(new FileInputStream("../medika.jks"), "123".toCharArray());
    
    KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(
        "nfe", new KeyStore.PasswordProtection("123".toCharArray()));
    
    CustomKeyManager km = new CustomKeyManager(null, ks, "nfe", keyEntry);
    KeyManager[] keyManagers = { km };

    context.init(keyManagers, createTrustManagers(), null);

    HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
    
    QName serviceName = new QName(namespace, serviceNameStr);
    QName portName = new QName(namespace, serviceNameStr + "Port");

    String endpointUrl = endpointURL;
    
    Service service = Service.create(serviceName);
    service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, endpointUrl);
    
    Dispatch<SOAPMessage> dispatch =
        service.createDispatch(portName, SOAPMessage.class, Service.Mode.MESSAGE);
    
    MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
    
    SOAPMessage request = mf.createMessage();
    SOAPPart part = request.getSOAPPart();
    
    SOAPEnvelope env = part.getEnvelope();
    SOAPHeader soapHeader = env.getHeader();
    SOAPBody soapBody = env.getBody();
    
    String[] params = getParams();
    String header = params[0];
    String data = params[1];

    SOAPElement operation = soapBody.addChildElement(messageNodeName, "", namespace);
    SOAPElement headerNode = operation.addChildElement("nfeCabecMsg");
    headerNode.addTextNode(header);
    SOAPElement dataNode = operation.addChildElement("nfeDadosMsg");
    dataNode.addTextNode(data);
    request.saveChanges();
    
    SOAPMessage response = dispatch.invoke(request);
    
    org.w3c.dom.Node responseBody = response.getSOAPBody().getFirstChild();
    org.w3c.dom.Node resultNode = responseBody.getFirstChild();
    org.w3c.dom.Node textNode = resultNode.getFirstChild();
    System.out.println(textNode.getNodeValue());
  }
  
  private static String[] getParams() throws Exception
  {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document doc = dbf.newDocumentBuilder().parse(System.in);

    org.w3c.dom.Element contextNode = (org.w3c.dom.Element) doc.getDocumentElement();
    org.w3c.dom.NodeList headerParam = contextNode.getElementsByTagName("headerParam");
    org.w3c.dom.NodeList dataParam = contextNode.getElementsByTagName("dataParam");
    
    return new String[] {
        headerParam.item(0).getFirstChild().getNodeValue(),
        dataParam.item(0).getFirstChild().getNodeValue() };
  }
}

