package com.lucaszanella.RequestExchange;


import com.lucaszanella.JsonNavigator.JsonNavigator;
import okhttp3.*;

import javax.json.*;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ExchangeRequester {
    private static String protocol = "https"; //Always https, http not allowed >:)
    private String ExchangeApiDomain;
    private String ExchangeName;
    private String ExchangeCoin;
    private String ExchangeApiPath;
    private String ExchangeCurrency;
    //private ExchangeJsonModel Meta;
    private java.io.Reader Reader;
    private JsonObject exchangeObject;
    private static OkHttpClient httpsClient = new OkHttpClient.Builder()
            //.addNetworkInterceptor(new UserAgentInterceptor(userAgent))
            .build();
    private Request okhttpApiRequester;

    /*
        Creates a new Exchange Requester based on the name of the exchange, the coin you want and the currency
     */
    public ExchangeRequester(String Exchange, String Coin, String Currency) {
        try {
            Reader = new FileReader("exchanges.json");
            JsonReader jsonReader = Json.createReader(Reader);
            JsonObject exchangesList = jsonReader.readObject();
            this.exchangeObject = exchangesList.getJsonObject(Exchange);
            JsonObject exchangeMetadata = exchangeObject.getJsonObject("meta");
            this.ExchangeName = exchangeMetadata.getString("name");
            this.ExchangeCoin = Coin;
            this.ExchangeCurrency = Currency;
            this.ExchangeApiPath = exchangeMetadata.
                    getJsonObject("api").
                    getJsonObject(Coin).
                    getString(Currency);//POSSIBLE EXCEPTION HERE, TAKE CARE LATER
            this.okhttpApiRequester = new Request.Builder().
                    url(protocol + "://" + this.ExchangeApiPath).
                    build();
        } catch (Exception e) {
            System.out.println("error... " + e); //Detail errors here
        }
    }

    /*
        Does the actual price request, parses it and returns in the format "ExchangeInfo"
     */
    public Map<String, Number> Request() throws Exception {
        Response r = httpsClient.newCall(okhttpApiRequester).execute();
        String json = r.body().string();
        System.out.println(json);

        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonObject = jsonReader.readObject();
        //ExchangeInfo e = new ExchangeInfo();
        Map<String, Number> exchangeInfo = new HashMap<>();
        /*
            Let's iterate through the exchangeObject to see where to find
            each node in the api response from each exchange. For example,
            MercadoBitcoin's 'high' price is found at its json response by
            navigating in ticker and then high, so MercadoBitcoin's entry
            in exchanges.json says that 'high' is located at 'ticker.high'
         */
        for (Map.Entry<String, JsonValue> entry : this.exchangeObject.entrySet()) {
            String key = entry.getKey();
            JsonValue value = entry.getValue();
            if (!key.equals("meta")) { //Ignore the key 'meta', we're intersted in the high,low,... keys
                JsonValue jsonValue = JsonNavigator.Navigate(value.toString(), jsonObject);
                if (jsonValue!=null && jsonValue.getValueType().equals(JsonValue.ValueType.NUMBER)) {
                    Number n = ((Number) jsonValue);
                    exchangeInfo.put(key, n);
                } else if (jsonValue==null){
                    //Null returned, couldn't navigate to this on json
                } else {
                    //Throw error, no number returned or can't be converted to number
                }
                break;
            }
            //System.out.println("key: " + key + ", value: " + value);
        }
        return exchangeInfo;
    }
}