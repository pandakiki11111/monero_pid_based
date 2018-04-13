package com.banco.monero.service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.banco.monero.properties.Properties;
import com.banco.monero.util.Util;

@Service("apiService")
public class APIServiceImpl implements APIService{

	private static final Logger logger = LoggerFactory.getLogger(APIServiceImpl.class);

	@Autowired(required=true)
	Properties info;
	
	@Autowired(required=true)
	Util util;
	
	@Override
	public JSONObject apiCall(JSONObject param) {
		return findMethod(param);
	}
	
	/**
	 * 신규 address 생성
	 * 
	 * @param paramMap
	 * @param info
	 * @return
	 */
	@SuppressWarnings({ "unused" })
	private JSONObject mon_getnewaccount(JSONObject param, Properties info){
		//DB 작업
		JSONObject jsonParams = new JSONObject();
		JSONObject data = new JSONObject();
		
		data.put("filename", param.has("account") ? param.get("account") : "");
		data.put("password", param.has("password") ? param.get("password") : "");
		data.put("language", param.has("language") ? param.get("language") : "English");
		
		jsonParams.put("method", "create_wallet");
		jsonParams.put("params", data);
		
		return monero_request(jsonParams, info, "rpc");
	}

	/**
	 * wallet 주소 가져오기
	 * 
	 * @param paramMap
	 * @param info
	 * @return
	 */
	@SuppressWarnings("unused")
	private JSONObject mon_getwalletaddress(JSONObject param, Properties info) {
		
		JSONObject jsonParams = new JSONObject();
		
		jsonParams.put("method", "getaddress");
		
		return monero_request(jsonParams, info, "rpc");
	}
	
	/**
	 * 밸런스 값 가져오기
	 * 
	 * @param paramMap
	 * @param info
	 * @return
	 * @throws InterruptedException 
	 */
	@SuppressWarnings({ "unused" })
	private JSONObject mon_getbalance(JSONObject param, Properties info) throws InterruptedException {
		//DB 조회로 변경
		JSONObject open = mon_openwallet(param, info);
		if(open.has("error")) return open;
		
		JSONObject sync = mon_checksync(param, info);
		if(sync.has("status")) return sync;
		
		param.put("method", "getbalance");
		
		JSONObject result = new JSONObject(monero_request(param, info, "rpc").toString());
		
		if(result.has("status")) return result;
		
		//setting result data
		if(!result.has("error")){
			if(result.getJSONObject("result").has("balance")) {
				Double balance = result.getJSONObject("result").getDouble("balance");
				result.getJSONObject("result").put("balance", new Util().toMonero(info.unit_piconero, balance));
			}
			if(result.getJSONObject("result").has("unlocked_balance")) {
				Double unlocked_balance = result.getJSONObject("result").getDouble("unlocked_balance");
				result.getJSONObject("result").put("unlocked_balance", new Util().toMonero(info.unit_piconero, unlocked_balance));
			}
		}
		
		return result;
	}
	
	
	/**
	 * 송금
	 * 
	 * @param paramMap
	 * @param info
	 * @return
	 */
	@SuppressWarnings({ "unused" })
	private JSONObject mon_withdrawalcoin(JSONObject param, Properties info) {
		//이제 payment_id 도 받아야 함
		//DB 에서 잔액 차감
		
		JSONObject open = mon_openwallet(param, info);
		if(open.has("error")) return open;
		
		JSONObject sync = mon_checksync(param, info);
		if(sync.has("status")) return sync;
		
		//주소와 금액 destination Setting
		JSONArray destinations = new JSONArray();
		
		JSONObject params = new JSONObject(param.toString());
		JSONArray dataArray =  new JSONArray(params.get("data").toString());
		
		for(int i = 0; i < dataArray.length(); i++){
			JSONObject destination = new JSONObject();
			
			destination.put("amount", dataArray.getJSONObject(i).getDouble("amount"));
			destination.put("address", dataArray.getJSONObject(i).getString("toaddress"));
			
			destinations.put(destination);
		}
		
		//params setting
		JSONObject dataParams = new JSONObject();
		
		dataParams.put("mixin", 4); // total 5 signatures (checkout monero ring signature)
		dataParams.put("get_tx_key", true); //Return the transaction key after sending
		dataParams.put("destinations", destinations);
		
		JSONObject jsonParams = new JSONObject();
		
		jsonParams.put("method", "transfer");
		jsonParams.put("params", dataParams);
		
		System.out.println("jsonParams : "+jsonParams);
		
		return monero_request(jsonParams, info, "rpc");
	}
	
	/**
	 * 송금 내역 확인
	 * @param param
	 * @param info
	 * @return
	 */
	@SuppressWarnings("unused")
	private JSONObject mon_gettransaction(JSONObject param, Properties info) {
		//db 에서 조회
		JSONObject open = mon_openwallet(param, info);
		if(open.has("error")) return open;
		
		JSONObject sync = mon_checksync(param, info);
		if(sync.has("status")) return sync;
		
		//params setting
		JSONObject dataParams = new JSONObject();
		dataParams.put("txid", (param.has("txid") ? param.get("txid") : ""));
		
		JSONObject jsonParams = new JSONObject();
		
		jsonParams.put("method", "get_transfer_by_txid");
		jsonParams.put("params", dataParams);
		
		JSONObject result = new JSONObject(monero_request(jsonParams, info, "rpc").toString());
		
		//setting result data
		if(!result.has("error")){
			if(result.getJSONObject("result").getJSONObject("transfer").has("amount")){
				double amount = result.getJSONObject("result").getJSONObject("transfer").getDouble("amount");
				result.getJSONObject("result").getJSONObject("transfer").put("amount",  new Util().toMonero(info.unit_piconero, amount));
			}
		}
		
		return result;
	}
	
	
	/**
	 * ※ parameter 체크 후 method 명 생성하여 method 실행 [coninname / command 필수]
	 *   
	 * @param paramMap
	 * @return
	 */
	private JSONObject findMethod(JSONObject param) {
		
		//coin name check
		if("error".equals(util.coinNameCheck(param, info).get("status"))){
			return util.coinNameCheck(param, info);
		}
		
		//command check
		if(!param.has("command")){ //대소문자 구분함 (<>ignore case)
			
			param.put("status", "error");
			param.put("error_message", "command is missing");
			
			return param;
		}

		//get coinname and command from parameter
		String coinname = param.get("coinname").toString(); 
		String command = param.get("command").toString();
		
		//generate method name
		String methodName = (coinname+"_"+command).toLowerCase();
		
		logger.info("coin_name : " + coinname + ", command : " + command + ", method_name : " + methodName);

		//check method exist in class
		Method[] methods = this.getClass().getDeclaredMethods(); // getMethods => returns only public methods
		boolean hasMethod = false;

		for (Method m : methods) {
		  if (m.getName().equals(methodName)) {
			  hasMethod = true;
		  }
		}
		
		if(hasMethod == false){
			param.put("status", "error");
			param.put("error_message", "method undefind (check command) <"+command+">");
			
			return param;
		}
		
		//excute method
		Method method = null;
		Object result = null;
		
		try {
			
			Class<?> c = Class.forName(this.getClass().getName());
			Object obj = c.newInstance();
			
			method = c.getDeclaredMethod(methodName, JSONObject.class, Properties.class); // (methodname, parameter type1, parameter type2...)
			method.setAccessible(true);
			
			result = method.invoke(obj, param, info); //properties를 보내줘야함.. new instance 라서 생성이 안됨
			
		} catch (Exception e){
			e.printStackTrace();
			
			param.put("status", "error");
			param.put("error_message", e.getMessage());
			
			return param;
		}finally{
			method.setAccessible(false); // it works after catch statement
		}
		
		return (JSONObject) result;
	}
	
	/*
	 * 모네로
	 */
	
	/**
	 * 모네로 코어 wallet rpc api 요청
	 * 
	 * @param jsonParams
	 * @param info
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private JSONObject monero_request(JSONObject jsonParams, Properties info, String serverType){
		Util util = new Util();
		JSONObject result = new JSONObject();
		
		String url = info.getMonero_url(serverType);
		
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json; charset=UTF-8");
		
		jsonParams.put("jsonrpc", "2.0");
		jsonParams.put("id", "0"); // json rpc response bring this id
		
		String params = jsonParams.toString();

		logger.info("params : " + params);
		
		try{
			String resultDecode = util.request(url, params, headers);
			
			if (!resultDecode.startsWith("error")) {
				Map<String, String> resultMap;
		    	resultMap = new ObjectMapper().readValue(resultDecode, HashMap.class);
		    	result = util.mapToJsonObject(resultMap);
			}else{
				result.put("status", "error");
				result.put("error_message", resultDecode.split("@#")[1]);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		logger.info(result.toString());
		
		return result;
	}
	
	/**
	 * monero open wallet
	 * API 사용전 wallet을 open 해야한다
	 * @param param
	 * @param info
	 * @return
	 */
	private JSONObject mon_openwallet(JSONObject param, Properties info){
		
		JSONObject jsonParams = new JSONObject();
		JSONObject data = new JSONObject();
		
		data.put("filename", param.has("account") ? param.get("account") : "");
		data.put("password", param.has("password") ? param.get("password") : "");
		
		jsonParams.put("method", "open_wallet");
		jsonParams.put("params", data);
		
		return monero_request(jsonParams, info, "rpc");
		
	}
	
	/**
	 * 접속한 rpc 서버의 block height 정보 정보 가져오기 실패시 -1 돌려줌
	 * @param param
	 * @param info
	 * @return
	 */
	private Integer mon_getheight(JSONObject param, Properties info){
		
		int returnValue = -1;
		
		try{
			JSONObject result = new JSONObject(monero_request(new JSONObject().put("method", "getheight"), info, "rpc").toString());
			
			if(!result.has("error") && result.has("result")){
				if(result.getJSONObject("result").has("height")){
					returnValue = result.getJSONObject("result").getInt("height");
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			returnValue = -1;
		}
		
		return returnValue;
	}
	
	private Integer mon_getblockcount(JSONObject param, Properties info){
		int returnValue = -1;
		
		try{
			JSONObject result = new JSONObject(monero_request(new JSONObject().put("method", "getblockcount"), info, "daemon").toString());
			
			if((!result.has("error")) && result.has("result")){
				if(result.getJSONObject("result").has("count")){
					returnValue = result.getJSONObject("result").getInt("count");
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			returnValue = -1;
		}
		
		return returnValue;
	}	
	
	private JSONObject mon_checksync(JSONObject param, Properties info) {
		boolean bool = false;
		int time = 0;
		
		JSONObject result = new JSONObject();
		
		try{
			while(bool == false){
				int rpcHeight = mon_getheight(param, info);
				int daemonHeight = mon_getblockcount(param, info);
				
				if(rpcHeight == daemonHeight){
					bool = true;
				}else{
					if(time <= 30){
						Thread.sleep(3000); //5초
						time += 3;
						continue;
					}else{
						bool = true;
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			result.put("status", "error");
			result.put("error_message","check sync between rpc and daemon ("+e.getMessage()+")");
		}
		
		if(time > 30){
			result.put("status", "error");
			result.put("error_message", "sync time over 30");
		}
		
		return result;
	}
}
