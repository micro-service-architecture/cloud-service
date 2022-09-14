package com.boot.scheduler.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class SimpleJob extends QuartzJobBean {

    private int MAX_SLEEP_IN_SECONDS = 5;
    private volatile Thread currThread;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getJobDetail().getKey();
        currThread = Thread.currentThread();
        System.out.println("============================================================================");
        System.out.println("SimpleJob started :: jobKey : " + jobKey + " - " + currThread.getName());

        IntStream.range(0, 5).forEach(i -> {
            System.out.println("SimpleJob Counting - " + i);
            try {
                TimeUnit.SECONDS.sleep(MAX_SLEEP_IN_SECONDS);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        });

        System.out.println("SimpleJob ended :: jobKey : " + jobKey + " - " + currThread.getName());
        System.out.println("============================================================================");
    }
}
