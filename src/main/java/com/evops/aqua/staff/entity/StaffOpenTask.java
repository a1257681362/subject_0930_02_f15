package com.evops.aqua.staff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * 在办任务登记：现场人员当前承担的跨模块在办事项（采样/疫病复查/捕捞/其他），完成销项。
 * 批量冻结时据此列出"未完成任务"；设备员在途工单另实时反查保养工单表。
 */
@TableName("t_staff_open_task")
public class StaffOpenTask extends BaseEntity {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_FINISHED = "FINISHED";

    public static final String TYPE_WQ_TASK = "WQ_TASK";
    public static final String TYPE_DISEASE_FOLLOWUP = "DISEASE_FOLLOWUP";
    public static final String TYPE_FISHING_RESERVATION = "FISHING_RESERVATION";
    public static final String TYPE_OTHER = "OTHER";

    private Long staffId;
    private String postCode;
    private String taskType;
    private String sourceBizNo;
    private Long sourceBizId;
    private String taskSummary;
    private String status;
    private String registeredBy;
    private LocalDateTime registeredTime;
    private String finishedBy;
    private LocalDateTime finishedTime;
    private String requestId;

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getSourceBizNo() { return sourceBizNo; }
    public void setSourceBizNo(String sourceBizNo) { this.sourceBizNo = sourceBizNo; }
    public Long getSourceBizId() { return sourceBizId; }
    public void setSourceBizId(Long sourceBizId) { this.sourceBizId = sourceBizId; }
    public String getTaskSummary() { return taskSummary; }
    public void setTaskSummary(String taskSummary) { this.taskSummary = taskSummary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
    public LocalDateTime getRegisteredTime() { return registeredTime; }
    public void setRegisteredTime(LocalDateTime registeredTime) { this.registeredTime = registeredTime; }
    public String getFinishedBy() { return finishedBy; }
    public void setFinishedBy(String finishedBy) { this.finishedBy = finishedBy; }
    public LocalDateTime getFinishedTime() { return finishedTime; }
    public void setFinishedTime(LocalDateTime finishedTime) { this.finishedTime = finishedTime; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
