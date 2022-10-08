package com.boot.scheduler.listener;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.springframework.stereotype.Component;

@Component
public class TriggersListener implements TriggerListener {
    @Override
    public String getName() {
        return "globalTrigger";
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        JobKey jobKey = trigger.getJobKey();
        System.out.println("triggerFired at " + trigger.getStartTime() + " :: jobKey : " + jobKey);
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        JobKey jobKey = trigger.getJobKey();
        System.out.println("triggerMisfired at " + trigger.getStartTime() + " :: jobKey : " + jobKey);
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {
        JobKey jobKey = trigger.getJobKey();
        System.out.println("triggerComplete at " + trigger.getStartTime() + " :: jobKey : " + jobKey);
    }
}
