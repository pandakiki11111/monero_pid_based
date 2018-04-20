package com.banco.monero.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class APIRepository {
	
	@Autowired(required=true)
	private SqlSession sqlSession;
	
	private static final String namespace = "com.banco.monero.dao.mapper.";
	
	public int createAccount(JSONObject param) {
		return sqlSession.insert(namespace + "createAccount", param.toMap());
	}

	public JSONObject getbalance(String account) {
		return sqlSession.selectOne(namespace + "getbalance", account);
	}

	public List<Map<String, Object>> gettransaction(Map<String, String> paramMap) {
		
		return sqlSession.selectList(namespace + "gettransaction", paramMap);
	}

	public int insertTransfer(List<Map<String, Object>> list) {
		return sqlSession.insert(namespace + "insertTransfer", list);
	}
}
