package com.evops.aqua.staff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

/**
 * 人员资质授权模块变更证据：所有写操作留痕（对象、动作、操作者、前后 JSON 快照、幂等请求号），
 * 含定时提醒与过期收权，操作者固定为调度标识也不丢操作者，只追加。
 */
@TableName("t_staff_change_evidence")
public class StaffChangeEvidence extends BaseEntity {

    public static final String ENTITY_PERSONNEL = "PERSONNEL";
    public static final String ENTITY_CERTIFICATE = "CERTIFICATE";
    public static final String ENTITY_POST_SCOPE = "POST_SCOPE";
    public static final String ENTITY_GRANT = "POSITION_GRANT";
    public static final String ENTITY_TEMP_GRANT = "TEMP_GRANT";
    public static final String ENTITY_REVOKE = "PERMISSION_REVOKE";
    public static final String ENTITY_FREEZE = "FREEZE_BATCH";
    public static final String ENTITY_OPEN_TASK = "OPEN_TASK";

    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_ISSUE_CERT = "ISSUE_CERT";
    public static final String ACTION_REVOKE_CERT = "REVOKE_CERT";
    public static final String ACTION_CERT_REMIND = "CERT_REMIND";
    public static final String ACTION_CERT_EXPIRE = "CERT_EXPIRE";
    public static final String ACTION_UPDATE_SCOPE = "UPDATE_SCOPE";
    public static final String ACTION_GRANT = "GRANT";
    public static final String ACTION_POST_CHANGE = "POST_CHANGE";
    public static final String ACTION_TEMP_GRANT = "TEMP_GRANT";
    public static final String ACTION_TEMP_EXPIRE = "TEMP_EXPIRE";
    public static final String ACTION_TEMP_REVOKE = "TEMP_REVOKE";
    public static final String ACTION_PERMISSION_REVOKE = "PERMISSION_REVOKE";
    public static final String ACTION_FREEZE = "FREEZE";
    public static final String ACTION_UNFREEZE = "UNFREEZE";
    public static final String ACTION_LEAVE = "LEAVE";
    public static final String ACTION_OPEN_TASK_REGISTER = "OPEN_TASK_REGISTER";
    public static final String ACTION_OPEN_TASK_FINISH = "OPEN_TASK_FINISH";

    private String entityType;
    private Long entityId;
    private String action;
    private String requestId;
    private String operator;
    private String beforeJson;
    private String afterJson;
    private String detail;

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getBeforeJson() { return beforeJson; }
    public void setBeforeJson(String beforeJson) { this.beforeJson = beforeJson; }
    public String getAfterJson() { return afterJson; }
    public void setAfterJson(String afterJson) { this.afterJson = afterJson; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
