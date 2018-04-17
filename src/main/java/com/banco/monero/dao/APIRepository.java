package com.banco.monero.dao;

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
}
