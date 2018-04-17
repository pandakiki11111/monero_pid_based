package com.banco.monero.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JobRepository {
	
	@Autowired(required=true)
	private SqlSession sqlSession;
	
	private static final String namespace = "com.banco.monero.dao.mapper.";
	
	public int getLastBlockHeight(){
		return sqlSession.selectOne(namespace + "getLastBlockHeight");
	}

	public int injectionToDB(List<Map<String, Object>> payments) {
		return sqlSession.insert(namespace + "insertData", payments);
	}

	public int updateBlockHeight(int maxSyncBlockHeight) {
		return sqlSession.insert(namespace + "updateBlockHeight", maxSyncBlockHeight);
	}
}
