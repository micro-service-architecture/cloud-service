package com.boot.scheduler.service.impl;

import com.boot.scheduler.dto.JobRequest;
import com.boot.scheduler.dto.JobResponse;
import com.boot.scheduler.dto.JobStatusResponse;
import com.boot.scheduler.service.ScheduleService;
import com.boot.scheduler.util.DateTimeUtils;
import com.boot.scheduler.util.JobUtils;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;
    @Autowired
    private ApplicationContext context;

    @Override
    public JobStatusResponse getAllJobs() {
        JobResponse jobResponse;
        JobStatusResponse jobStatusResponse = new JobStatusResponse();
        List<JobResponse> jobs = new ArrayList<>();
        int numOfRunningJobs = 0;
        int numOfGroups = 0;
        int numOfAllJobs = 0;

        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            for (String groupName : scheduler.getJobGroupNames()) {
                numOfGroups++;
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);

                    jobResponse = new JobResponse.Builder()
                            .setJobName(jobKey.getName())
                            .setGroupName(jobKey.getGroup())
                            .setScheduleTime(DateTimeUtils.toString(triggers.get(0).getStartTime()))
                            .setLastFiredTime(DateTimeUtils.toString(triggers.get(0).getPreviousFireTime()))
                            .setNextFireTime(DateTimeUtils.toString(triggers.get(0).getNextFireTime()))
                            .build();

                    if (isJobRunning(jobKey)) {
                        jobResponse.setJobStatus("RUNNING");
                        numOfRunningJobs++;
                    } else {
                        String jobState = getJobState(jobKey);
                        jobResponse.setJobStatus(jobState);
                    }
                    numOfAllJobs++;
                    jobs.add(jobResponse);
                }
            }
        } catch (SchedulerException e) {
            System.err.println("[schedulerdebug] error while fetching all job info");
            e.printStackTrace();
        }

        jobStatusResponse.setNumOfAllJobs(numOfAllJobs);
        jobStatusResponse.setNumOfRunningJobs(numOfRunningJobs);
        jobStatusResponse.setNumOfGroups(numOfGroups);
        jobStatusResponse.setJobs(jobs);
        return jobStatusResponse;
    }

    @Override
    public boolean isJobRunning(JobKey jobKey) {
        try {
            List<JobExecutionContext> currentJobs = schedulerFactoryBean.getScheduler().getCurrentlyExecutingJobs();
            if (currentJobs != null) {
                for (JobExecutionContext jobCtx : currentJobs) {
                    if (jobKey.getName().equals(jobCtx.getJobDetail().getKey().getName())) {
                        return true;
                    }
                }
            }
        } catch (SchedulerException e) {
            System.err.println("[schedulerdebug] error occurred while checking job with jobKey : " + jobKey);
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isJobExists(JobKey jobKey) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            if (scheduler.checkExists(jobKey)) {
                return true;
            }
        } catch (SchedulerException e) {
            System.err.println("[schedulerdebug] error occurred while checking job exists :: jobKey : " + jobKey);
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean addJob(JobRequest jobRequest, Class<? extends Job> jobClass) {
        JobKey jobKey = null;
        JobDetail jobDetail;
        Trigger trigger;

        try {
            trigger = JobUtils.createTrigger(jobRequest);
            jobDetail = JobUtils.createJob(jobRequest, jobClass, context);
            jobKey = JobKey.jobKey(jobRequest.getJobName(), jobRequest.getJobGroup());

            Date dt = schedulerFactoryBean.getScheduler().scheduleJob(jobDetail, trigger);
            System.out.println("Job with jobKey : " + jobDetail.getKey() + " scheduled successfully at date : " + dt);
            return true;
        } catch (SchedulerException e) {
            System.err.println("error occurred while scheduling with jobKey : " + jobKey);
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deleteJob(JobKey jobKey) {
        System.err.println("[schedulerdebug] deleting job with jobKey : " + jobKey);
        try {
            return schedulerFactoryBean.getScheduler().deleteJob(jobKey);
        } catch (SchedulerException e) {
            System.err.println("[schedulerdebug] error occurred while deleting job with jobKey : " + jobKey);
        }
        return false;
    }

    @Override
    public boolean pauseJob(JobKey jobKey) {
        System.err.println("[schedulerdebug] pausing job with jobKey : " + jobKey);
        try {
            schedulerFactoryBean.getScheduler().pauseJob(jobKey);
            return true;
        } catch (SchedulerException e) {
            System.err.println("[schedulerdebug] error occurred while deleting job with jobKey : " + jobKey);
        }
        return false;
    }

    @Override
    public boolean resumeJob(JobKey jobKey) {
        System.err.println("[schedulerdebug] resuming job with jobKey : " + jobKey);
        try {
            schedulerFactoryBean.getScheduler().resumeJob(jobKey);
            return true;
        } catch (SchedulerException e) {
            System.err.println("[schedulerdebug] error occurred while resuming job with jobKey : " + jobKey);
        }
        return false;
    }

    @Override
    public String getJobState(JobKey jobKey) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);

            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());

            if (triggers != null && triggers.size() > 0) {
                for (Trigger trigger : triggers) {
                    Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                    if (Trigger.TriggerState.NORMAL.equals(triggerState)) {
                        return "SCHEDULED";
                    }
                    return triggerState.name().toUpperCase();
                }
            }
        } catch (SchedulerException e) {
            System.err.println("[schedulerdebug] Error occurred while getting job state with jobKey : " + jobKey);
        }
        return null;
    }
}
