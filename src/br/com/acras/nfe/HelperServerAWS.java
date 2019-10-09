package nfe;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.HashMap;
import java.util.Map;

public class HelperServerAWS implements RequestHandler<HelperServerRequest, HelperServerResponse> {

    private final String                BASE_DIRECTORY = "diretorio-da-pasta-lib";
    private final String                S3_BUCKET = "nome-bucket-s3";

    HelperServerResponse                response;
    Map<String, KeyEntryReference>      keyEntryMap;
    KeyStoreInitializationAWS           keyStoreInitializationAWS;
    SignHandlerAWS                      signHandlerAWS;
    private String                      keyStoreId;

    /*
    * TODO - precisa implementar HelperServer::configPKCS11
    */

    public HelperServerResponse handleRequest(HelperServerRequest request, Context context) {

      keyEntryMap = new HashMap<String, KeyEntryReference>();
      keyStoreInitializationAWS = new KeyStoreInitializationAWS(S3_BUCKET, BASE_DIRECTORY, keyEntryMap);
      try {
        keyStoreId = keyStoreInitializationAWS.handle(request);
        signHandlerAWS = new SignHandlerAWS(keyEntryMap);
        response = new HelperServerResponse();
        signHandlerAWS.handle(request, response, keyStoreId);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return response;
    }
}