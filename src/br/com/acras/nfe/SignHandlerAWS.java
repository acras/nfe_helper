package nfe;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.security.cert.X509Certificate;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;

import org.w3c.dom.Document;

import utils.*;

class SignHandlerAWS {
  Map<String, KeyEntryReference> keyEntryMap;

  public SignHandlerAWS(Map<String, KeyEntryReference> keyEntryMap)
  {
    this.keyEntryMap = keyEntryMap;
  }

  protected void handle(HelperServerRequest resquest, HelperServerResponse response, String keyStoreId) throws Exception
  {
    XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

    String uri = resquest.getUri();
    SignedInfo si = getSignedInfo(fac, uri);

    KeyEntryReference keRef = keyEntryMap.get(keyStoreId);
    if (keRef == null)
      throw new NotFoundException("No key entry with id " + keyStoreId +
          " was found (not initialized?)");
    KeyInfo ki = getKeyInfo(fac, keRef.getKeyEntry());

    Document doc = readDocument(resquest.getInputStream());

    DOMSignContext dsc = new DOMSignContext(
                             keRef.getKeyEntry().getPrivateKey(),
                             doc.getDocumentElement());

    XMLSignature signature = fac.newXMLSignature(si, ki);
    try
    {
      signature.sign(dsc);
    }
    catch(XMLSignatureException e)
    {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof URIReferenceException)
        throw new NotFoundException(
            "Element with id " + uri + " cannot be found inside document");
      else
        throw e;
    }

    writeDocument(doc, response);
  }

  protected String getAllowedMethod()
  {
    return "POST";
  }

  private SignedInfo getSignedInfo(XMLSignatureFactory fac, String uri)
      throws NoSuchAlgorithmException, InvalidAlgorithmParameterException
  {
    Transform algEnveloped = fac.newTransform(
        "http://www.w3.org/2000/09/xmldsig#enveloped-signature",
        (TransformParameterSpec) null);
    Transform algC14n = fac.newTransform(
        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315",
        (TransformParameterSpec) null);
    List<Transform> transforms = new ArrayList<Transform>();
    transforms.add(algEnveloped);
    transforms.add(algC14n);

    Reference ref = fac.newReference(
                        uri,
                        fac.newDigestMethod(DigestMethod.SHA1, null),
                        transforms,
                        null,
                        null);

    CanonicalizationMethod c14nMethod = fac.newCanonicalizationMethod(
                                            CanonicalizationMethod.INCLUSIVE,
                                            (C14NMethodParameterSpec) null);
    return fac.newSignedInfo(
        c14nMethod,
        fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
        Collections.singletonList(ref));
  }

  private KeyInfo getKeyInfo(XMLSignatureFactory fac,
      KeyStore.PrivateKeyEntry keyEntry)
  {
    X509Certificate cert = (X509Certificate) keyEntry.getCertificate();

    KeyInfoFactory kif = fac.getKeyInfoFactory();
    return kif.newKeyInfo(Collections.singletonList(
        kif.newX509Data(Collections.singletonList(cert))));
  }

  // TODO: readDocument e writeDocument estão parcialmente duplicados em
  // WebServiceInvokationHandler. Deve-se encontrar um meio de reutilizá-los
  private Document readDocument(InputStream input)
      throws ParserConfigurationException, SAXException, IOException
  {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    return dbf.newDocumentBuilder().parse(input);
  }

  private void writeDocument(Document doc, HelperServerResponse response)
      throws TransformerConfigurationException, TransformerException
  {
    TransformerFactory tf = TransformerFactory.newInstance();
    tf.newTransformer().transform(
        new DOMSource(doc),
        new StreamResult(response.getPrintStream()));
    response.loadDocument();
  }
}

