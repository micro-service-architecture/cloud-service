package com.boot.scheduler.dto;

import java.util.List;

public class JobStatusResponse {
    private int numOfAllJobs;
    private int numOfGroups;
    private int numOfRunningJobs;
    private List<JobResponse> jobs;

    public int getNumOfAllJobs() {
        return numOfAllJobs;
    }

    public void setNumOfAllJobs(int numOfAllJobs) {
        this.numOfAllJobs = numOfAllJobs;
    }

    public int getNumOfGroups() {
        return numOfGroups;
    }

    public void setNumOfGroups(int numOfGroups) {
        this.numOfGroups = numOfGroups;
    }

    public int getNumOfRunningJobs() {
        return numOfRunningJobs;
    }

    public void setNumOfRunningJobs(int numOfRunningJobs) {
        this.numOfRunningJobs = numOfRunningJobs;
    }

    public List<JobResponse> getJobs() {
        return jobs;
    }

    public void setJobs(List<JobResponse> jobs) {
        this.jobs = jobs;
    }
}
