package com.boot.scheduler.dto;

public class JobResponse {
    private String jobName;
    private String groupName;
    private String jobStatus;
    private String scheduleTime;
    private String lastFiredTime;
    private String nextFireTime;

    public JobResponse(String jobName, String groupName, String jobStatus, String scheduleTime, String lastFiredTime, String nextFireTime) {
        this.jobName = jobName;
        this.groupName = groupName;
        this.jobStatus = jobStatus;
        this.scheduleTime = scheduleTime;
        this.lastFiredTime = lastFiredTime;
        this.nextFireTime = nextFireTime;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(String scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public String getLastFiredTime() {
        return lastFiredTime;
    }

    public void setLastFiredTime(String lastFiredTime) {
        this.lastFiredTime = lastFiredTime;
    }

    public String getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(String nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    public static class Builder {
        private String jobName;
        private String groupName;
        private String jobStatus;
        private String scheduleTime;
        private String lastFiredTime;
        private String nextFireTime;

        public Builder setJobName(String jobName) {
            this.jobName = jobName;
            return this;
        }

        public Builder setGroupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder setJobStatus(String jobStatus) {
            this.jobStatus = jobStatus;
            return this;
        }

        public Builder setScheduleTime(String scheduleTime) {
            this.scheduleTime = scheduleTime;
            return this;
        }

        public Builder setLastFiredTime(String lastFiredTime) {
            this.lastFiredTime = lastFiredTime;
            return this;
        }

        public Builder setNextFireTime(String nextFireTime) {
            this.nextFireTime = nextFireTime;
            return this;
        }

        public JobResponse build() {
            return new JobResponse(jobName, groupName, jobStatus, scheduleTime, lastFiredTime, nextFireTime);
        }
    }
}
