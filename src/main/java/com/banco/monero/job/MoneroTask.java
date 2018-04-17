package com.banco.monero.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.banco.monero.dao.JobRepository;
import com.banco.monero.service.APIServiceImpl;

public class MoneroTask {
	
	@Autowired
	APIServiceImpl service;
	
	@Autowired
	JobRepository dao;

	private static final Logger logger = LoggerFactory.getLogger(MoneroTask.class);
	
	final static int givenBlockCountUnit = 100; //한번에 조회할 block 수
	
	public void updatePayments () {
		
		try{
			//동기화 진행할 blockheight 정보 가져오기
			int lastSyncBlockHeight = getLastSyncInfo(); //마지막 동기화 블록
			int maxSyncBlockHeight = lastSyncBlockHeight + givenBlockCountUnit;
			
			if(lastSyncBlockHeight < 0) {
				logger.info("blockheight less than 0 (" + lastSyncBlockHeight + ")"); 
				return;
			}
			
			if(service.mon_getblockcount(null)<=maxSyncBlockHeight){
				logger.info("no more transfer list"); return;
			}
			
			//monero rpc 로 transfer 정보 가져오기
			List<Map<String, Object>> payments = getTransfers(lastSyncBlockHeight, maxSyncBlockHeight);
			
			if(payments.size() <= 0){
				logger.info("transfer invalid");
				return;
			}
			
			if(payments.get(0).containsKey("status"))
			
			if(payments.get(0).containsKey("status")){
				logger.error(payments.get(0).get("error_message").toString());
			}
			
			int result = injectionToDB(payments);
			
			logger.info("Last Sync BlockHeight : " + lastSyncBlockHeight
					+ ", total payments count : " + payments.size()
					+ ", update payments count : " + result
					+ ", failed payments count : " + "??");

			updateBlockHeight(maxSyncBlockHeight);
			
		}catch(Exception e){
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private int getLastSyncInfo(){
		return dao.getLastBlockHeight();
	}
	
	private List<Map<String, Object>> getTransfers(int lastSyncBlockHeight, int maxSyncBlockHeight){
		
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

		//params setting
		JSONObject params = new JSONObject();
		JSONObject data = new JSONObject();
		
		data.put("in", true);
		data.put("out", true);
		data.put("pending", true);
		data.put("failed", true);
		data.put("pool", true);
		
		data.put("min_height", lastSyncBlockHeight);
		data.put("max_height", maxSyncBlockHeight);
		
		params.put("id", "banco");
		params.put("method", "get_transfers");
		params.put("params", data);
		
		JSONObject transfers = service.monero_request(params, "rpc");
		
		//error 가 있으면 
		if(transfers.has("status")){
			if("error".equals(transfers.getString("status"))){
				result.add(transfers.toMap());
				return result;
			}
		}

		if(!(transfers.length() > 0)){
			result.add(new JSONObject().put("status", "error").put("error_message", "transfer data is empty from server").toMap());
			return result;
		}
		
		result = getTransferList(transfers);
		
		return result;
	}
	
	private List<Map<String, Object>> getTransferList(JSONObject transferdata){
		
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		JSONObject data = new JSONObject(transferdata.toString());
		
		if(data.has("id")){
			if(!"banco".equals(data.getString("id"))){
				result.add(new JSONObject()
						.put("status", "error")
						.put("error_message", "transfer data from rpc had wrong id : "+data.getString("id")).toMap());
				return result;
			}
		}
		
		JSONObject list = data.getJSONObject("result");
		
		if(list.has("in")){
			for(int i = 0; i < list.getJSONArray("in").length(); i++){
				result.add(list.getJSONArray("in").getJSONObject(i).put("type", "in").toMap());
			}
		}
		
		if(list.has("out")){
			for(int i = 0; i < list.getJSONArray("out").length(); i++){
				result.add(list.getJSONArray("out").getJSONObject(i).put("type", "out").toMap());
			}		
		}
		
		if(list.has("pending")){
			for(int i = 0; i < list.getJSONArray("pending").length(); i++){
				result.add(list.getJSONArray("pending").getJSONObject(i).put("type", "pending").toMap());
			}
		}
		
		if(list.has("failed")){
			for(int i = 0; i < list.getJSONArray("failed").length(); i++){
				result.add(list.getJSONArray("failed").getJSONObject(i).put("type", "failed").toMap());
			}
		}
		
		if(list.has("pool")){
			for(int i = 0; i < list.getJSONArray("pool").length(); i++){
				result.add(list.getJSONArray("pool").getJSONObject(i).put("type", "pool").toMap());
			}
		}
		
		return result;
	}
	
	private int injectionToDB(List<Map<String, Object>> payments){
		return dao.injectionToDB(payments);
	}
	
	private int updateBlockHeight(int maxSyncBlockHeight){
		return dao.updateBlockHeight(maxSyncBlockHeight);
	}
}
