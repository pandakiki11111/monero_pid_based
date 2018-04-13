package com.banco.monero.properties;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class Properties {
	
	@Value("#{properties['monero.ip']}")
	private String MONERO_IP;
	
	@Value("#{properties['monero.port']}")
	private String MONERO_PORT;
	
	@Value("#{properties['monero.daemon.ip']}")
	private String MONERO_DAEMON_IP;
	
	@Value("#{properties['monero.daemon.port']}")
	private String MONERO_DAEMON_PORT;
	
	@Value("#{properties['coinnames']}")
	private String COINNAMES;
	
	//command list (확인용)
	public String METHOD_NEW_ACCOUT = "getnewaccount";
	public String METHOD_GET_WALLET_ADDRESS = "getwalletaddress";
	public String METHOD_GET_BALANCE = "getbalance";
	public String METHOD_WITHDRAWAL_COIN = "withdrawalcoin";
	public String METHOD_GET_TRANSACTION = "gettransaction";
	public String METHOD_GET_TRANSACTION_LIST = "gettransactionlist";
	public String METHOD_OPEN_WALLET = "openwallet";
	
	public String getMONERO_IP() {
		return MONERO_IP;
	}

	public String getMONERO_PORT() {
		return MONERO_PORT;
	}
	
	public String getMONERO_DAEMON_IP() {
		return MONERO_DAEMON_IP;
	}

	public String getMONERO_DAEMON_PORT() {
		return MONERO_DAEMON_PORT;
	}
	
	/**
	 * return monero url (default : rpc)
	 * @param type
	 * @return
	 */
	public String getMonero_url(String type) {
		String url = MONERO_IP + ":" + MONERO_PORT;
		if("daemon".equals(type)) url = MONERO_DAEMON_IP + ":" + MONERO_DAEMON_PORT;
		return url + "/json_rpc";
	}
	
	public List<String> getCoinNames(){
		List<String> list = Arrays.asList(COINNAMES.toUpperCase().split(":"));
		return list;
	}
	
	//unit list
	public int unit_piconero = -12;
	public int unit_nanonero = -9;
	public int unit_micronero = -6;
	public int unit_millinero = -3;
	public int unit_centinero = -2;
	public int unit_decinero = -1;
	public int unit_monero = 0;
	public int unit_decanero = 1;
	public int unit_hectonero = 2;
	public int unit_kilonero = 3;
	public int unit_meganero = 6;
	
	public Map<String, Integer> getNeros(){
		
		Map<String, Integer> map = new HashMap<>();
		
		/*
	    Denominations of Monero
		Name	   Base 10	Amount
		piconero	10^-12	0.000000000001
		nanonero	10^-9	0.000000001
		micronero	10^-6	0.000001
		millinero	10^-3	0.001
		centinero	10^-2	0.01
		decinero	10^-1	0.1
		monero		10^0	1
		decanero	10^1	10
		hectonero	10^2	100
		kilonero	10^3	1,000
		
		meganero	10^6	1,000,000
	 */
		map.put("piconero", unit_piconero);
		map.put("nanonero", unit_nanonero);
		map.put("micronero", unit_micronero);
		map.put("millinero", unit_millinero);
		map.put("centinero", unit_centinero);
		map.put("decinero", unit_decinero);
		map.put("monero", unit_monero);
		map.put("decanero", unit_decanero);
		map.put("hectonero", unit_hectonero);
		map.put("kilonero", unit_kilonero);
		map.put("meganero", unit_meganero);
		
		return map;
	}
}
