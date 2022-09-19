package com.boot.scheduler.batch;

import com.boot.scheduler.batch.tasklet.MessageTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                .flow(helloStep(null))
                .end()
                .build();
    }

    @Bean
    @JobScope
    public Step helloStep(@Value("#{jobParameters[date]}") String date) {
        System.out.println("helloStep 메서드 실행");
        return stepBuilderFactory.get("myHelloStep") // 임의의 스탭 이름을 지정
                .tasklet(new MessageTasklet("Hello!")) // 실행하는 Tasklet을 지정
                .build();
    }
}
