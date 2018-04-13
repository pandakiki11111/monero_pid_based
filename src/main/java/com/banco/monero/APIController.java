package com.banco.monero;

import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.banco.monero.service.APIServiceImpl;
import com.banco.monero.util.Util;

@Controller
public class APIController {

	@Autowired
	APIServiceImpl service;
	
	private static final Logger logger = LoggerFactory.getLogger(APIController.class);
	
	@RequestMapping(value = "/api", method = RequestMethod.POST)
	public ModelAndView CoreAPI(Model model, @RequestBody Map<String, String> param){

		ModelAndView mv = new ModelAndView();
		mv.setViewName("jsonView");
		
		JSONObject result = service.apiCall(new Util().mapToJsonObject(param));
		if(!result.has("status")) result.put("status", "success");
		
		logger.info("status : " + result.get("status"));
		
		mv.addObject("data", result.toMap());

		return mv;
	}
}
