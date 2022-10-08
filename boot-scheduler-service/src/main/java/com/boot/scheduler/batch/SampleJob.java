package com.boot.scheduler.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableBatchProcessing
public class SampleJob {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean("SampleJob")
    public Job SampleJob() {
        System.out.println("SampleJob 메서드 실행");
        return jobBuilderFactory.get("SampleJob")
                .flow(helloStep())
                .end()
                .build();
    }

    @Bean
    public Step helloStep() {
        System.out.println("helloStep 메서드 실행");
        return stepBuilderFactory.get("helloStep").tasklet(helloWorldTasklet(null)).build();
    }

    @Bean
    @StepScope
    public Tasklet helloWorldTasklet(@Value("#{jobParameters['message']}") String message) {
        return (stepContribution, chukContext) -> {
            System.out.println(message);
            return RepeatStatus.FINISHED;
        };
    }
}
