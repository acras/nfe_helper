package nfe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

public class HelperServerRequest {

  String uri;
  String keystoretype;
  String keystorefile;
  String keystorepassword;
  String keyentryalias;
  String keyentrypassword;
  String nfeDocument;

  public HelperServerRequest(String uri, String keystoretype, String keystorefile, String keystorepassword,
                              String keyentryalias, String keyentrypassword, String nfeDocument) {
    this.uri = uri;
    this.keystoretype = keystoretype;
    this.keystorefile = keystorefile;
    this.keystorepassword = keystorepassword;
    this.keyentryalias = keyentryalias;
    this.keyentrypassword = keyentrypassword;
    this.nfeDocument = nfeDocument;
  }

  public HelperServerRequest() {

  }

  /*
  * TODO - retornar novo stream a cada chamada não parece ser a melhor escolha
  */
  public InputStream getInputStream() throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    if (this.nfeDocument != null)
    {
      InputStream in = IOUtils.toInputStream(this.nfeDocument, "UTF-8");
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

    return new ByteArrayInputStream(out.toByteArray());
  }

  /**
   * @return the uri
   */
  public String getUri() {
    return uri;
  }

  /**
   * @param uri the uri to set
   */
  public void setUri(String uri) {
    this.uri = uri;
  }

  /**
   * @return the nfeDocument
   */
  public String getNfeDocument() {
    return nfeDocument;
  }

  /**
   * @param nfeDocument the nfeDocument to set
   */
  public void setNfeDocument(String nfeDocument) {
    this.nfeDocument = nfeDocument;
  }

  /**
   * @return the keystoretype
   */
  public String getKeystoretype() {
    return keystoretype;
  }

  /**
   * @param keystoretype the keystoretype to set
   */
  public void setKeystoretype(String keystoretype) {
    this.keystoretype = keystoretype;
  }

  /**
   * @return the keystorefile
   */
  public String getKeystorefile() {
    return keystorefile;
  }

  /**
   * @param keystorefile the keystorefile to set
   */
  public void setKeystorefile(String keystorefile) {
    this.keystorefile = keystorefile;
  }

  /**
   * @return the keystorepassword
   */
  public String getKeystorepassword() {
    return keystorepassword;
  }

  /**
   * @param keystorepassword the keystorepassword to set
   */
  public void setKeystorepassword(String keystorepassword) {
    this.keystorepassword = keystorepassword;
  }

  /**
   * @return the keyentryalias
   */
  public String getKeyentryalias() {
    return keyentryalias;
  }

  /**
   * @param keyentryalias the keyentryalias to set
   */
  public void setKeyentryalias(String keyentryalias) {
    this.keyentryalias = keyentryalias;
  }

  /**
   * @return the keyentrypassword
   */
  public String getKeyentrypassword() {
    return keyentrypassword;
  }

  /**
   * @param keyentrypassword the keyentrypassword to set
   */
  public void setKeyentrypassword(String keyentrypassword) {
    this.keyentrypassword = keyentrypassword;
  }

}