package org.cwowhappy.securityanalyze.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 创建采集任务请求 DTO。
 */
@Data
public class CreateCollectionTaskRequest {

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    private Map<String, Object> taskParams;

    private String dataSource;
}
