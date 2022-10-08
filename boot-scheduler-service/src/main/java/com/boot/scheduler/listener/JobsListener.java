package com.boot.scheduler.listener;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.springframework.stereotype.Component;

@Component
public class JobsListener implements JobListener {
    @Override
    public String getName() {
        return "globalJob";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        JobKey jobKey = context.getJobDetail().getKey();
        System.out.println("jobToBeExecuted :: jobKey : " + jobKey);
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        JobKey jobKey = context.getJobDetail().getKey();
        System.out.println("jobExecutionVetoed :: jobKey : " + jobKey);
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        JobKey jobKey = context.getJobDetail().getKey();
        System.out.println("jobWasExecuted :: jobKey : " + jobKey);
    }
}
