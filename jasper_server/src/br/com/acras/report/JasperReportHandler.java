package br.com.acras.report;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRXmlDataSource;

import br.com.acras.utils.*;

public class JasperReportHandler extends CustomHttpHandler
{
  protected void handle(CustomHttpExchange exchange) throws Exception
  {
    String compiledDesign = null;
    String selectCriteria = null;
    Map<String, String> reportParams = new HashMap<String, String>();
    
    Iterator it = exchange.getParameters().entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry entry = (Map.Entry) it.next();
      String k = (String) entry.getKey();
      String v = (String) entry.getValue();
      
      if (k.equals("compileddesign"))
        compiledDesign = v;
      else if (k.equals("selectcriteria"))
        selectCriteria = v;
      else
        reportParams.put(k, v);
    }

    if (compiledDesign == null)
      throw new BadRequestException("Missing parameter: compileddesign");
    if (selectCriteria == null)
      throw new BadRequestException("Missing parameter: selectcriteria");
    
    JRXmlDataSource dataSource = new JRXmlDataSource(exchange.getInputStream(), selectCriteria);
    
    dataSource.setLocale(Locale.US);
    dataSource.setNumberPattern("0");
    dataSource.setDatePattern("yyyy-MM-dd");
    
    JasperPrint jasperPrint = JasperFillManager.fillReport(compiledDesign, reportParams, dataSource);
    JasperExportManager.exportReportToPdfStream(jasperPrint, exchange.getPrintStream());
  }

  protected String getAllowedMethod()
  {
    return "POST"; 
  }
}
