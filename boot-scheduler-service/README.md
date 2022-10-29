# boot-scheduler
## 목차
* **[JOB 관리 페이지](#JOB-관리-페이지)**
* **[Cron Job](#Cron-Job)**
* **[Simple Job](#Simple-Job)**
* **[Batch Job](#Batch-Job)**

`boot-scheduler` 는 quartz 를 통해 스케줄을 생성하고 `스케줄 Name` 으로 해당하는 `batch Job` 을 만들어서 schedule-batch 설정하는 모듈이다.

## JOB 관리 페이지
![image](https://user-images.githubusercontent.com/31242766/190974118-7a736302-6561-4284-b499-c8ae5bac674f.png)

## Cron Job
![image](https://user-images.githubusercontent.com/31242766/190973740-dea281c7-202d-4932-ae5a-310bed8c51da.png)

## Simple Job
![image](https://user-images.githubusercontent.com/31242766/190975312-5d4e2930-f24a-4f25-ad3d-ec1335c34ae1.png)

## Batch Job
스케줄에 필요한 로직을 해당 `SampleJob` 처럼 batch 를 구성하여 schedule-batch 설정한다.
```java
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
```
