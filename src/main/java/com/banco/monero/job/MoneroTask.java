package com.banco.monero.job;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.banco.monero.service.APIServiceImpl;

public class MoneroTask {
	
	@Autowired
	APIServiceImpl service;

	private static final Logger logger = LoggerFactory.getLogger(MoneroTask.class);
	
	final static int givenBlockCountUnit = 100;
	
	public void updatePayments () {
		
		try{
			//동기화 진행할 blockheight 정보 가져오기
			int lastSyncBlockHeight = getLastSyncInfo();
			int maxSyncBlockHeight = lastSyncBlockHeight + givenBlockCountUnit;
			
			if(lastSyncBlockHeight < 0) {
				logger.info("blockheight less than 0 (" + lastSyncBlockHeight + ")"); 
				return;
			}
			
			//monero rpc 로 transfer 정보 가져오기
			JSONArray payments = getPayments(lastSyncBlockHeight, maxSyncBlockHeight);
			
			if(payments.length() < 0){
				logger.info("transfer invalid");
				return;
			}
			
			Map<String, String> result = new HashMap<String, String>();
			result = injectionToDB(payments);
			
			if("error".equals(result.get("status"))) {
				logger.info(result.get("error_message"));
				return;
			}
			
			logger.info("Last Sync BlockHeight : " + lastSyncBlockHeight 
					+ ", update payments count : " + result.get("success")
					+ ", failed payments count : " + result.get("failed"));
			
			updateBlockHeight(maxSyncBlockHeight);
			
		}catch(Exception e){
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private int getLastSyncInfo(){
		return 0;
	}
	
	private JSONArray getPayments(int lastSyncBlockHeight, int maxSyncBlockHeight){
		return null;
	}
	
	private Map<String, String> injectionToDB(JSONArray payments){
		return null;
	}
	
	private int updateBlockHeight(int maxSyncBlockHeight){
		return 0;
	}
}
