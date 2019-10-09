package nfe;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayOutputStream;

/**
 * HelperServerResponse
 */
public class HelperServerResponse {

  String nfeDocument;
  PrintStream printStream;

  public HelperServerResponse() {

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

  /*
  * TODO - retornar PrintStream somente para se conformar ao SignHandler não parece ser a melhor opção
  */
  public PrintStream getPrintStream()  {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try
    {
      printStream = new PrintStream(byteArrayOutputStream, false, "UTF-8");
    }
    catch(UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }
    return printStream;
  }

  public void loadDocument() {
    this.nfeDocument = this.printStream.toString();
  }

}