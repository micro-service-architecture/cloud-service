package com.boot.scheduler.controller;

import com.boot.scheduler.dto.ApiResponse;
import com.boot.scheduler.dto.JobRequest;
import com.boot.scheduler.dto.JobStatusResponse;
import com.boot.scheduler.job.CronJob;
import com.boot.scheduler.job.SimpleJob;
import com.boot.scheduler.service.ScheduleService;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scheduler")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @RequestMapping(value = "/job", method = RequestMethod.GET)
    public JobStatusResponse getAllJobs() {
        return scheduleService.getAllJobs();
    }

    @RequestMapping(value = "/job", method = RequestMethod.POST)
    public ResponseEntity<?> addScheduleJob(@ModelAttribute JobRequest jobRequest) {
        System.err.println("add schedule job :: jobRequest : " + jobRequest);
        if (jobRequest.getJobName() == null) {
            return new ResponseEntity<>(new ApiResponse(false, "Require jobName"),
                    HttpStatus.BAD_REQUEST);
        }

        JobKey jobKey = new JobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        if (!scheduleService.isJobExists(jobKey)) {
            if (jobRequest.getCronExpression() == null) {
                scheduleService.addJob(jobRequest, SimpleJob.class);
            } else {
                scheduleService.addJob(jobRequest, CronJob.class);
            }
        } else {
            return new ResponseEntity<>(new ApiResponse(false, "Job already exits"),
                    HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ApiResponse(true, "Job created successfully"), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/job", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteScheduleJob(@ModelAttribute JobRequest jobRequest) {
        JobKey jobKey = new JobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        if (scheduleService.isJobExists(jobKey)) {
            if (!scheduleService.isJobRunning(jobKey)) {
                scheduleService.deleteJob(jobKey);
            } else {
                return new ResponseEntity<>(new ApiResponse(false, "Job already in running state"), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(new ApiResponse(false, "Job does not exits"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ApiResponse(true, "Job deleted successfully"), HttpStatus.OK);
    }
}
