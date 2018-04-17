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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import com.banco.monero.dao.APIRepository;
import com.banco.monero.util.Info;
import com.banco.monero.util.Util;

@Service
@ComponentScan("com.banco.monero")
public class APIServiceImpl implements APIService{

	private static final Logger logger = LoggerFactory.getLogger(APIServiceImpl.class);
	
	@Autowired
	APIRepository dao;
	
	@Autowired
	Info info;
	
	@Autowired
	Util util;

	@Override
	public JSONObject apiCall(JSONObject param) {
		return findMethod(param);
	}
	
	/**
	 * 신규 address 생성
	 * 
	 * @param paramMap
	 * @return
	 */
	private JSONObject mon_getnewaccount(JSONObject param){

		JSONObject result = new JSONObject();
		
		if(!param.has("account")){
			result.put("status", "error");
			result.put("error_message", "account parameter is missing");
			return result;
		}
		
		if(!param.has("password")){
			result.put("status", "error");
			result.put("error_message", "password parameter is missing");
			return result;
		}
		
		int count = 0;
		
		try{
			count = dao.createAccount(param);
		}catch(Exception e){
			result.put("status", "error");
			result.put("error_message", e.getMessage());
			
			e.printStackTrace();
			
			return result;
		}
		
		if(count > 0){
			result.put("status", "success");
		}else{
			result.put("status", "error");
			result.put("error_message", "create new account failed");
		}
		
		return result;
	}

	/**
	 * wallet 주소 가져오기
	 * 
	 * @param paramMap
	 * @return
	 */
	private JSONObject mon_getwalletaddress(JSONObject param) {
		
		JSONObject jsonParams = new JSONObject();
		
		jsonParams.put("method", "getaddress");
		
		return monero_request(jsonParams, "rpc");
	}
	
	/**
	 * 밸런스 값 가져오기
	 * 
	 * @param paramMap
	 * @return
	 * @throws InterruptedException 
	 */
	private JSONObject mon_getbalance(JSONObject param){
		//DB 조회로 변경
		
		return dao.getbalance();
	}
	
	
	/**
	 * 송금
	 * 
	 * @param paramMap
	 * @return
	 */
	private JSONObject mon_withdrawalcoin(JSONObject param) {
		//이제 payment_id 도 받아야 함
		//DB 에서 잔액 차감
		
		JSONObject open = mon_openwallet(param);
		if(open.has("error")) return open;
		
		JSONObject sync = mon_checksync(param);
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
		
		return monero_request(jsonParams, "rpc");
	}
	
	/**
	 * 송금 내역 확인
	 * @param param
	 * @return
	 */
	private JSONObject mon_gettransaction(JSONObject param) {
		//db 에서 조회

		return dao.gettransaction();
	}
	
	
	
	/*
	 * 모네로
	 */
	
	/**
	 * 모네로 코어 wallet rpc api 요청
	 * 
	 * @param jsonParams
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject monero_request(JSONObject jsonParams, String serverType){
		JSONObject result = new JSONObject();
		
		String url = info.getMonero_url(serverType);
		
		System.out.println(url);
		
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json; charset=UTF-8");
		
		jsonParams.put("jsonrpc", "2.0");
		if(!jsonParams.has("id")) jsonParams.put("id", "0"); // json rpc response bring this id 
		
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
		
		logger.info(methodName);
		
		Object result = null;
		
		if("mon_getnewaccount".equals(methodName)) result = mon_getnewaccount(param);
		if("mon_getwalletaddress".equals(methodName)) result = mon_getwalletaddress(param);
		if("mon_getbalance".equals(methodName)) result = mon_getbalance(param);
		if("mon_withdrawalcoin".equals(methodName)) result = mon_withdrawalcoin(param);
		if("mon_gettransaction".equals(methodName)) result = mon_gettransaction(param);
		
		//excute method
//		Method method = null;
//		Object result = null;
//		
//		try {
//			
//			Class<?> c = Class.forName(this.getClass().getName());
//			Object obj = c.newInstance();
//			
//			method = c.getDeclaredMethod(methodName, JSONObject.class); // (methodname, parameter type1, parameter type2...)
//			method.setAccessible(true);
//			
//			result = method.invoke(obj, param); //properties를 보내줘야함.. new instance 라서 생성이 안됨 >>componenetscan 으로 회생가능
//			
//		} catch (Exception e){
//			e.printStackTrace();
//			
//			param.put("status", "error");
//			param.put("error_message", e.getMessage());
//			
//			return param;
//		}finally{
//			method.setAccessible(false); // it works after catch statement
//		}
		
		return (JSONObject) result;
	}
	
	/**
	 * monero open wallet
	 * API 사용전 wallet을 open 해야한다
	 * @param param
	 * @return
	 */
	private JSONObject mon_openwallet(JSONObject param){
		
		JSONObject jsonParams = new JSONObject();
		JSONObject data = new JSONObject();
		
		data.put("filename", param.has("account") ? param.get("account") : "");
		data.put("password", param.has("password") ? param.get("password") : "");
		
		jsonParams.put("method", "open_wallet");
		jsonParams.put("params", data);
		
		return monero_request(jsonParams, "rpc");
		
	}
	
	/**
	 * 접속한 rpc 서버의 block height 정보 정보 가져오기 실패시 -1 돌려줌
	 * @param param
	 * @return
	 */
	private Integer mon_getheight(JSONObject param){
		
		int returnValue = -1;
		
		try{
			JSONObject result = new JSONObject(monero_request(new JSONObject().put("method", "getheight"), "rpc").toString());
			
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
	
	public Integer mon_getblockcount(JSONObject param){
		int returnValue = -1;
		
		try{
			JSONObject result = new JSONObject(monero_request(new JSONObject().put("method", "getblockcount"), "daemon").toString());
			
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
	
	private JSONObject mon_checksync(JSONObject param) {
		boolean bool = false;
		int time = 0;
		
		JSONObject result = new JSONObject();
		
		try{
			while(bool == false){
				int rpcHeight = mon_getheight(param);
				int daemonHeight = mon_getblockcount(param);
				
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
