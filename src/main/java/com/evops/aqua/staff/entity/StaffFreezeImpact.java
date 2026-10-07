package com.evops.aqua.staff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

/**
 * 冻结影响清单：逐人逐项列出受影响工单与未完成任务，随冻结单只追加。
 * impactType: WORK_ORDER 受影响工单 / OPEN_TASK 未完成任务。
 */
@TableName("t_staff_freeze_impact")
public class StaffFreezeImpact extends BaseEntity {

    public static final String TYPE_WORK_ORDER = "WORK_ORDER";
    public static final String TYPE_OPEN_TASK = "OPEN_TASK";

    private Long freezeBatchId;
    private Long staffId;
    private String staffNo;
    private String staffName;
    private String impactType;
    private String sourceModule;
    private String bizRef;
    private String bizStatus;
    private String detail;

    public Long getFreezeBatchId() { return freezeBatchId; }
    public void setFreezeBatchId(Long freezeBatchId) { this.freezeBatchId = freezeBatchId; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public String getStaffNo() { return staffNo; }
    public void setStaffNo(String staffNo) { this.staffNo = staffNo; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public String getImpactType() { return impactType; }
    public void setImpactType(String impactType) { this.impactType = impactType; }
    public String getSourceModule() { return sourceModule; }
    public void setSourceModule(String sourceModule) { this.sourceModule = sourceModule; }
    public String getBizRef() { return bizRef; }
    public void setBizRef(String bizRef) { this.bizRef = bizRef; }
    public String getBizStatus() { return bizStatus; }
    public void setBizStatus(String bizStatus) { this.bizStatus = bizStatus; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
