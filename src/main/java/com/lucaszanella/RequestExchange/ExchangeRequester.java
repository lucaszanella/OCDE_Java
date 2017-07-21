package com.lucaszanella.RequestExchange;


import com.lucaszanella.JsonUtilities.JsonNavigator;
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
    private String[] Pairs;
    //private ExchangeJsonModel Meta;
    private java.io.Reader Reader;
    private JsonObject exchangeObject;
    private JsonObject exchangeMetadata;
    private OkHttpClient httpsClient;

    /*
        Creates a new Exchange Requester based on the name of the exchange, the coin you want and the currency
     */
    public ExchangeRequester(String Exchange) {
        try {
            Reader = new FileReader("exchanges.json");
            JsonReader jsonReader = Json.createReader(Reader);
            JsonObject exchangesList = jsonReader.readObject();
            this.exchangeObject = exchangesList.getJsonObject(Exchange);
            this.exchangeMetadata = exchangeObject.getJsonObject("meta");
            this.ExchangeName = exchangeMetadata.getString("name");
            this.Pairs = (String[]) exchangeMetadata.getJsonArray("pairs").toArray();
        } catch (Exception e) {
            System.out.println("error... " + e); //Detail errors here
        }
    }
    /*
        Replaces the string "{n}" from url by the n-th string of stringsToInsert
     */
    public String ReplaceApiUrl(String url, String[] stringsToInsert) {
        return url;
    }

    /*
        Does the actual price request, parses it and returns in the format "ExchangeInfo"
     */
    public Map<String, Number> Request(String Coin1, String Coin2) throws Exception {
        this.httpsClient = new OkHttpClient.Builder()
                //.addNetworkInterceptor(new UserAgentInterceptor(userAgent))
                .build();

        String path;

        if (this.Pairs.length>0) {
            path = this.exchangeMetadata.getJsonObject("api").getString("pair");
            path = ReplaceApiUrl(path, new String[]{Coin1, Coin2});
        } else {
            path = this.exchangeMetadata.getJsonObject("api").getJ;
        }

        Request okhttpApiRequester = new Request.Builder().
                url(protocol + "://" + path).
                build();

        Response r = httpsClient.newCall(okhttpApiRequester).execute();
        String json = r.body().string();
        System.out.println(json);

        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonObject = jsonReader.readObject();

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
            if (!key.equals("meta")) { //Ignore the key 'meta', we're interested in the high,low,... keys
                JsonString value = this.exchangeObject.getJsonString(key);
                if (!"".equals(value.getString())) {
                    JsonValue jsonValue = JsonNavigator.Navigate(value.getString(), jsonObject);
                    if (jsonValue!=null && jsonValue.getValueType().equals(JsonValue.ValueType.NUMBER)) {
                        Number n = ((JsonNumber) jsonValue).doubleValue();
                        exchangeInfo.put(key, n);
                    } else if (jsonValue==null){
                        //Null returned, couldn't navigate to this on json
                    } else {
                        //Throw error, no number returned or can't be converted to number
                    }
                }
            }
            //System.out.println("key: " + key + ", value: " + value);
        }
        return exchangeInfo;
    }
}