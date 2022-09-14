package com.boot.scheduler.dto;

import org.quartz.JobDataMap;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class JobRequest {

    private String jobGroup = "DEFAULT";
    private String jobName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDateAt;
    private long repeatIntervalInSeconds;
    private int repeatCount;

    private String cronExpression;
    private JobDataMap jobDataMap;

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public LocalDateTime getStartDateAt() {
        return startDateAt;
    }

    public void setStartDateAt(LocalDateTime startDateAt) {
        this.startDateAt = startDateAt;
    }

    public long getRepeatIntervalInSeconds() {
        return repeatIntervalInSeconds;
    }

    public void setRepeatIntervalInSeconds(long repeatIntervalInSeconds) {
        this.repeatIntervalInSeconds = repeatIntervalInSeconds;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public JobDataMap getJobDataMap() {
        return jobDataMap;
    }

    public void setJobDataMap(JobDataMap jobDataMap) {
        this.jobDataMap = jobDataMap;
    }
}
