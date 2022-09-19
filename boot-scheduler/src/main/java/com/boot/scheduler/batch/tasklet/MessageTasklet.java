package com.boot.scheduler.batch.tasklet;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@RequiredArgsConstructor
public class MessageTasklet implements Tasklet {

    private final String message;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        System.out.println("Message: " + message); // 메세지 출력
        return RepeatStatus.FINISHED; // 처리가 완료된 것을 나타내는 수치 반환
    }
}
