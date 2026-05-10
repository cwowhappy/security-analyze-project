package org.cwowhappy.securityanalyze.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 创建采集任务调度请求 DTO。
 */
@Data
public class CreateCollectionTaskScheduleRequest {

    @NotBlank(message = "调度名称不能为空")
    private String name;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @NotBlank(message = "Cron 表达式不能为空")
    private String cronExpression;

    private Map<String, Object> taskParams;

    private String dataSource;
}
