package com.evops.aqua.staff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

/**
 * 岗位可操作范围配置：兽医/采样员/捕捞员/设备员四类岗位各一行。
 * menuCodes 菜单编码逗号分隔；apiScopes 接口 Ant 路径逗号分隔（现场人员实时访问时校验）。
 * 调整配置只影响之后授权/收权所冻结的快照，历史授权快照不变。
 */
@TableName("t_staff_post_scope")
public class StaffPostScope extends BaseEntity {

    public static final String POST_VET = "VET";
    public static final String POST_SAMPLER = "SAMPLER";
    public static final String POST_FISHER = "FISHER";
    public static final String POST_EQUIPMENT = "EQUIPMENT";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private String postCode;
    private String postName;
    private String menuCodes;
    private String apiScopes;
    private String status;
    private String changedReason;
    private String operator;

    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }
    public String getMenuCodes() { return menuCodes; }
    public void setMenuCodes(String menuCodes) { this.menuCodes = menuCodes; }
    public String getApiScopes() { return apiScopes; }
    public void setApiScopes(String apiScopes) { this.apiScopes = apiScopes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getChangedReason() { return changedReason; }
    public void setChangedReason(String changedReason) { this.changedReason = changedReason; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
