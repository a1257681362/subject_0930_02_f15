package com.evops.aqua.staff.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 在办任务登记：把现场人员承担的跨模块在办事项登记进来，完成后销项。
 * 批量冻结影响清单据此列出未完成任务。
 */
public class OpenTaskRequest {

    @NotBlank(message = "人员工号不能为空")
    private String staffNo;
    @NotBlank(message = "任务类型不能为空（WQ_TASK/DISEASE_FOLLOWUP/FISHING_RESERVATION/OTHER）")
    private String taskType;
    @NotBlank(message = "来源业务单号不能为空")
    private String sourceBizNo;
    private Long sourceBizId;
    private String taskSummary;
    @NotBlank(message = "登记人不能为空")
    private String registeredBy;
    @NotBlank(message = "请求幂等号不能为空")
    private String requestId;

    public String getStaffNo() { return staffNo; }
    public void setStaffNo(String staffNo) { this.staffNo = staffNo; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getSourceBizNo() { return sourceBizNo; }
    public void setSourceBizNo(String sourceBizNo) { this.sourceBizNo = sourceBizNo; }
    public Long getSourceBizId() { return sourceBizId; }
    public void setSourceBizId(Long sourceBizId) { this.sourceBizId = sourceBizId; }
    public String getTaskSummary() { return taskSummary; }
    public void setTaskSummary(String taskSummary) { this.taskSummary = taskSummary; }
    public String getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
