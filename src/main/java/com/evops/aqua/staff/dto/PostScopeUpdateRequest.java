package com.evops.aqua.staff.dto;

import javax.validation.constraints.NotBlank;

/**
 * 岗位可操作范围调整：菜单编码与接口 Ant 路径逗号分隔。
 * 调整只影响之后授权/收权所冻结的快照；历史授权快照不回改。
 */
public class PostScopeUpdateRequest {

    @NotBlank(message = "岗位编码不能为空")
    private String postCode;
    @NotBlank(message = "可操作菜单不能为空")
    private String menuCodes;
    @NotBlank(message = "可操作接口范围不能为空")
    private String apiScopes;
    @NotBlank(message = "调整原因不能为空")
    private String changedReason;
    @NotBlank(message = "操作人不能为空")
    private String operator;

    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getMenuCodes() { return menuCodes; }
    public void setMenuCodes(String menuCodes) { this.menuCodes = menuCodes; }
    public String getApiScopes() { return apiScopes; }
    public void setApiScopes(String apiScopes) { this.apiScopes = apiScopes; }
    public String getChangedReason() { return changedReason; }
    public void setChangedReason(String changedReason) { this.changedReason = changedReason; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
