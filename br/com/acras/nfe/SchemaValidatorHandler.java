import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.Map;
import java.util.HashMap;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

class SchemaValidatorHandler extends CustomHttpHandler
{
  Map<String, Schema> schemaGrammarMap;
  
  final String schemaStatusHeader = "X-Schema-File-Status";
  final String schemaCachedHeader = "X-Schema-Loaded-From-Cache";
  final String documentValidHeader = "X-Document-Valid";
      
  public SchemaValidatorHandler()
  {
    this.schemaGrammarMap = new HashMap<String, Schema>();
  }
  
  protected void handle(CustomHttpExchange exchange) throws Exception
  {
    String xsdpath = getSchemaFilePath(exchange);
    if (xsdpath == null)
      return;
    
    Validator schemaValidator = getSchemaValidator(exchange, xsdpath);
    if (schemaValidator == null)
      return;
    
    validateDocument(exchange, schemaValidator);
  }
  
  protected String getAllowedMethod()
  {
    return "POST";
  }

  private String getSchemaFilePath(CustomHttpExchange exchange)
  {
    String result = exchange.tryParameter("xsdpath");
    if (result == null)
    {
      exchange.addHeader(schemaStatusHeader, "not specified");
      exchange.getPrintStream().println("Schema file was not specified");
    }
    return result;
  }
  
  private Validator getSchemaValidator(CustomHttpExchange exchange, String schemaFileName)
  {
    Schema schemaGrammar = schemaGrammarMap.get(schemaFileName);
    if (schemaGrammar == null)
    {
      File schemaFile = new File(schemaFileName);
      if (!schemaFile.exists())
      {
        exchange.addHeader(schemaStatusHeader, "not found");
        exchange.getPrintStream().println("Schema file was not found on the server: " +
                                          schemaFileName);
        return null;
      }
      
      SchemaFactory schemaFactory = SchemaFactory.newInstance(
          "http://www.w3.org/2001/XMLSchema");
      try
      {
        schemaGrammar = schemaFactory.newSchema(schemaFile);
      }
      catch(SAXException e)
      {
        exchange.addHeader(schemaStatusHeader, "invalid");
        exchange.getPrintStream().println("Schema file is not valid: " + schemaFileName);
        return null;
      }
      
      schemaGrammarMap.put(schemaFileName, schemaGrammar);
      exchange.addHeader(schemaCachedHeader, "false");
    }
    else
      exchange.addHeader(schemaCachedHeader, "true");
    
    exchange.addHeader(schemaStatusHeader, "valid");
    return schemaGrammar.newValidator();
  }
  
  private void validateDocument(CustomHttpExchange exchange, Validator validator)
      throws IOException
  {
    String headerValue = "true";

    ErrorHandler errorHandler = new ErrorHandler(exchange.getPrintStream());
    validator.setErrorHandler(errorHandler);
    try
    {
      validator.validate(new StreamSource(exchange.getInputStream()));
      if (errorHandler.getErrorCount() > 0)
        headerValue = "false";
    }
    catch(SAXException e)
    {
      headerValue = "false";
    }
    
    exchange.addHeader(documentValidHeader, headerValue);
  }
  
  private class ErrorHandler extends DefaultHandler
  {
    PrintStream printStream;
    int errorCount = 0;
    
    public ErrorHandler(PrintStream printStream)
    {
      this.printStream = printStream;
    }
    
    public void error(SAXParseException parseException)
    {
      printException(parseException);
    }
    
    public void fatalError(SAXParseException parseException)
    {
      printException(parseException);
    }
    
    public void warning(SAXParseException parseException)
    {
      printException(parseException);
    }
    
    private void printException(SAXParseException exception)
    {
      errorCount += 1;
      printStream.println("lineNumber: " + exception.getLineNumber());
      printStream.println("columnNumber: " + exception.getColumnNumber());
      printStream.println("message: " + exception.getMessage());
    }
    
    public int getErrorCount()
    {
      return errorCount;
    }
  }
}
