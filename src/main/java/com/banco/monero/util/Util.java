package com.banco.monero.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.banco.monero.properties.Properties;

public class Util {
	
	private static final Logger logger = LoggerFactory.getLogger(Util.class);
	
	public String request(String string_url, String params,  HashMap<String, String> headers){
		
		 String response = "";
		 HttpURLConnection http = null;
		 InputStream in = null;
		
		try {
			
			URL url = new URL(string_url);
			http = (HttpURLConnection) url.openConnection();
			http.setRequestMethod("POST");
			http.setDoOutput(true);
			
			byte[] out = params.getBytes(StandardCharsets.UTF_8);
			int length = out.length;

			http.setFixedLengthStreamingMode(length);
			http.setRequestProperty("Content-Type", "application/json-rpc; charset=UTF-8");
			
			try(OutputStream os = http.getOutputStream()) {
				http.connect();
			    os.write(out);
			}catch(Exception e){
				return "error@#rpc server: "+e.getMessage();
			}
			
			in = http.getInputStream();
			
			StringBuilder textBuilder = new StringBuilder();
		    try (Reader reader = new BufferedReader(new InputStreamReader(in, Charset.forName(StandardCharsets.UTF_8.name())))) {
		        int c = 0;
		        while ((c = reader.read()) != -1) {
		            textBuilder.append((char) c);
		        }
		    }catch(Exception e){
		    	
		    }finally{
		    	in.close();
		    }
		    
		    response = textBuilder.toString();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return response;
	}
	
	/**
	 * map 을 query 문자로 변경
	 */
	public String mapToQueryString(Map<String, Object> map) {
		StringBuilder string = new StringBuilder();
	
		if (map.size() > 0) {
		    string.append("?");
		}
	
		for (Entry<String, Object> entry : map.entrySet()) {
		    string.append(entry.getKey());
		    string.append("=");
		    string.append(entry.getValue());
		    string.append("&");
		}
	
		return string.toString();
    }
	
	/**
	 * map (string, string) 을 json 형태로 변경
	 * */
	public JSONObject mapToJsonObject(Map<String, String> map){
		JSONObject json = new JSONObject();
		for( Map.Entry<String, String> entry : map.entrySet() ) {
			String key = entry.getKey();
			Object value = entry.getValue();
			json.put(key, value);
		}
		return json;
	}
	
	/**
	 * map (string, object) 을 json 형태로 변경 
	 * */
	public JSONObject mapToJsonObject(Map<String, Object> map, int flag){
		JSONObject json = new JSONObject();
		for( Map.Entry<String, Object> entry : map.entrySet() ) {
			String key = entry.getKey();
			Object value = entry.getValue();
			json.put(key, value);
		}
		return json;
	}
	
	/**
	 * other units to monero (check if null)
	 * 
	 * @param decimal
	 * @param amount
	 * @return
	 */
	public String toMonero(int decimal, double amount){

		Map<String, Integer> map = new Properties().getNeros();
		
		System.out.println("d : "+ decimal+"/"+amount);
		
		if(map.values().contains(decimal)){
			Double d = new Double(amount * (Math.pow(10, decimal)));
			
			System.out.println("to monero : "+ BigDecimal.valueOf(d).toPlainString());
			System.out.println("to unit : "+ toUnit(decimal, d));
			
			return BigDecimal.valueOf(d).toPlainString();
		}
		
		return null;
	}
	
	/**
	 * monero to units (check if null)
	 * 
	 * @param decimal
	 * @param amount
	 * @return
	 */
	public String toUnit(int decimal, double amount){
		Map<String, Integer> map = new Properties().getNeros();
		
		System.out.println("d : "+ decimal+"/"+amount);
		
		if(map.values().contains(decimal)){
			Double d = new Double(amount * (Math.pow(10, (decimal*-1))));
			return BigDecimal.valueOf(d).toPlainString();
		}
		
		return null;
	}
	
	public JSONObject paramValidator(JSONObject param, String command) {
		JSONObject result = new JSONObject();
		
		return result;
	}
	
	public JSONObject coinNameCheck(JSONObject param, Properties info){
		
		JSONObject result = new JSONObject();
		
		if(param.length() == 0){
			result.put("status", "error");
			result.put("error_message", "parameter is empty");
		}else if(!param.has("coinname")){
			result.put("status", "error");
			result.put("error_message", "coinname parameter is missing");
		}else if(!info.getCoinNames().contains(param.get("coinname").toString().toUpperCase())){
			result.put("status", "error");
			result.put("error_message", "coinname is invalid");
		}else{
			result.put("status", "success");
		}
		
		return result;
	}
}
