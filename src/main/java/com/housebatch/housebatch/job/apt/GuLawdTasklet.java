package com.housebatch.housebatch.job.apt;

import com.housebatch.housebatch.core.repository.LawdRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.StringUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * 아파트 주소지인 법정동 코드 조회처리를 위한 Tasklet
 *
 * ExecutionContext에 저장할 데이터
 * 1. guLawdCd : 법정동 구 코드로 다음 스텝에서 사용할 값
 * 2. guLawdCdList : 법정동 구 코드 배열
 * 3. itemCount : 남아있는 아이템(구 코드)의 개수
 */

@Slf4j
@RequiredArgsConstructor
public class GuLawdTasklet implements Tasklet {

    private final LawdRepository lawdRepository;
    private List<String> guLawdCdList;
    private int itemCount;
    private String guLawdCd;

    private static final String KEY_ITEM_COUNT = "itemCount";
    private static final String KEY_GU_LAWD_CD_LIST = "guLawdCdList";
    private static final String KEY_GU_LAWD_CD = "guLawdCd";

    public GuLawdTasklet(LawdRepository lawdRepository, String guLawdCd) {
        this.lawdRepository = lawdRepository;
        this.guLawdCd = guLawdCd;
    }

    /**
     *
     * @param contribution mutable state to be passed back to update the current step execution
     * @param chunkContext attributes shared between invocations but not between restarts
     * @return
     * @throws Exception
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        ExecutionContext executionContext = getExecutionContext(chunkContext);  // BATCH_STEP_EXECUTION_CONTEXT 내에 저장된 메타데이터 조회
        initList(executionContext);                                             // 법정동 코드 초기화
        initItemCount(executionContext);                                        // 법정동 코드 개수 초기화

        if (itemCount == 0) {
            contribution.setExitStatus(ExitStatus.COMPLETED);
            return RepeatStatus.FINISHED;
        }

        /**
         * ExecutionContext에 데이터 추가 시 문제가 될 수 있는 코드
         * 아래 코드와 같이 작성하게 되면 'java.lang.UnsupportedOperationException: null' 오류가 발생하게 된다.
         * 원인은 getJobExecutionContext()의 메소드에서 MAP은 읽기만 가능하고 쓰기는 불가능한 형태이기 때문에 발생하게 된다.
         * - chunkContext.getStepContext().getJobExecutionContext().put(KEY_GU_LAWD_CD, guLawdCdList.get(itemCount - 1));
         */
        executionContext.putString(KEY_GU_LAWD_CD, guLawdCdList.get(itemCount - 1));
        executionContext.putInt(KEY_ITEM_COUNT, itemCount - 1);

        contribution.setExitStatus(new ExitStatus("CONTINUABLE"));
        return RepeatStatus.FINISHED;
    }

    /**
     * ExecutionContext에 존재하는 데이터를 조회하는 메소드
     * @param chunkContext
     * @return ExecutionContext - 법정동에서 구 코드 추출하여 제공
     */
    private ExecutionContext getExecutionContext(
            ChunkContext chunkContext
    ) {
        /*
         * ExecutionContext 사용
         * 사용하는 경우
         *   1. 중단된 배치 작업을 이어서 진행하기 위한 경우
         *   2. Step 간 데이터를 전달이 필요한 경우
         *
         * 동작 방식
         *   - Spring Batch 실행내역을 Batch 관련 테이블에 메타데이터로 저장
         *     (데이터베이스 테이블 명 : BATCH_JOB_EXECUTION_CONTEXT)
         *   - Spring Batch 메타데이터 테이블을 참조하여 필요 데이터 조회
         */
        StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
        return stepExecution.getJobExecution().getExecutionContext();
    }

    /**
     * ExecutionContext 초기화 기능 제공<br>
     * - ExecutionContext 저장된 키 중 guLawdCdList가 존재하지 않을 경우 : 데이터베이스에서 법정동 코드 조회하여 ExecutionContext에 guLawdCdList와 itemCount 저장
     * - ExecutionContext 저장된 키 중 guLawdCdList가 존재하는 경우 : ExecutionContext룰 사용하여 법정동 코드 guLawdCdList에 저장
     * @param executionContext
     */
    private void initList(ExecutionContext executionContext) {

        /*
         * 데이터가 존재하면 다음 스텝을 실행, 없으면 종료되도록 진행
         *   - 데이터가 있는 경우 RepeatStatus.CONTINUABLE 로 동작
         *
         * ExecutionContext에 저장할 데이터
         * 1. guLawdCd : 법정동 구 코드로 다음 스텝에서 사용할 값
         * 2. guLawdCdList : 법정동 구 코드 배열
         * 3. itemCount : 남아있는 아이템(구 코드)의 개수
         *
         * [고민해 볼 사항]
         * ExecutionContext에 데이터베이스에서 불러온 데이터 전체를 저장하는 방식으로 동작
         * - 매번 데이터베이스 조회하는 방식으로 작성
         *   - 단점 : 데이터베이스 조회하는 반복 동작으로 시스템 부하 발생
         *   - 장점 : 대용량의 데이터를 분할 조회하여 자원을 안정적/효율적으로 사용 가능
         *           (내부에서 제한적으로 사용하는 경우인지, 다수의 고객으로 접근이 많이 발생하는 데이터인 경우인지 고려 필요)
         */
        if (executionContext.containsKey(KEY_GU_LAWD_CD_LIST)) {
            guLawdCdList = (List<String>) executionContext.get(KEY_GU_LAWD_CD_LIST);
        } else if(StringUtils.hasText(guLawdCd)) {
            guLawdCdList = Arrays.asList(guLawdCd);
            executionContext.put(KEY_GU_LAWD_CD_LIST, guLawdCdList);
            executionContext.putInt(KEY_ITEM_COUNT, guLawdCdList.size());
        } else {
            guLawdCdList = lawdRepository.findDistinctGuLawdCd();
            executionContext.put(KEY_GU_LAWD_CD_LIST, guLawdCdList);
            executionContext.putInt(KEY_ITEM_COUNT, guLawdCdList.size());
        }
    }

    /**
     * ExecutionContext 데이터 개수 초기화 기능 제공<br>
     * - ExecutionContext에 저장된 키 중 itemCount가 존재하지 않을 경우 : guLawdCdList를 사용하여 itemCount 초기화
     * - ExecutionContext에 저장된 키 중 itemCount가 존재하는 경우 : ExecutionContext룰 사용하여 법정동 개수를 itemCount에 저장
     * @param executionContext
     */
    private void initItemCount(ExecutionContext executionContext) {
        if (executionContext.containsKey(KEY_ITEM_COUNT)) {
            itemCount = executionContext.getInt(KEY_ITEM_COUNT);
        } else {
            itemCount = guLawdCdList.size();
        }
    }
}
