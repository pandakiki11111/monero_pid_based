package com.banco.monero.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class JobDetailBean extends QuartzJobBean {

	private MoneroTask task;
	
	@Override
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		
		task.updatePayments();
	}

	public void setTask(MoneroTask task) {
		this.task = task;
	}
}
