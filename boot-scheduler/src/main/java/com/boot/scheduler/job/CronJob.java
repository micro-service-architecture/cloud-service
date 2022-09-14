package com.boot.scheduler.job;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class CronJob extends QuartzJobBean {

    private int MAX_SLEEP_IN_SECONDS = 5;
    private volatile Thread currThread;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        int jobId = jobDataMap.getInt("jobId");
        JobKey jobKey = context.getJobDetail().getKey();

        currThread = Thread.currentThread();
        System.out.println("============================================================================");
        System.out.println("CronJob started :: sleep : " + MAX_SLEEP_IN_SECONDS + " jobId : " + jobId + " jobKey : " + jobKey + " - " + currThread.getName());

        IntStream.range(0, 10).forEach(i -> {
            System.out.println("CronJob Counting - " + i);
            try {
                TimeUnit.SECONDS.sleep(MAX_SLEEP_IN_SECONDS);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        });
        System.out.println("CronJob ended :: jobKey : " + jobKey + " - " + currThread.getName());
        System.out.println("============================================================================");
    }
}
