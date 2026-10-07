package com.evops.aqua.staff.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evops.aqua.staff.entity.StaffCertificate;
import com.evops.aqua.staff.entity.StaffCertReminder;
import com.evops.aqua.staff.entity.StaffPersonnel;
import com.evops.aqua.staff.entity.StaffPositionGrant;
import com.evops.aqua.staff.entity.StaffTempGrant;
import com.evops.aqua.staff.mapper.StaffCertificateMapper;
import com.evops.aqua.staff.mapper.StaffCertReminderMapper;
import com.evops.aqua.staff.mapper.StaffPersonnelMapper;
import com.evops.aqua.staff.mapper.StaffPositionGrantMapper;
import com.evops.aqua.staff.mapper.StaffTempGrantMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 人员资质授权查询出口：列表、详情、CSV 导出共用 StaffAccessService.toRow 同一权限口径，
 * 保证"列表看到的、详情展示的、导出落列的、日志可勾稽的"四者一致。
 */
@Service
public class StaffQueryService {

    private final StaffPersonnelMapper personnelMapper;
    private final StaffCertificateMapper certificateMapper;
    private final StaffCertReminderMapper reminderMapper;
    private final StaffPositionGrantMapper grantMapper;
    private final StaffTempGrantMapper tempGrantMapper;
    private final StaffAccessService accessService;

    public StaffQueryService(StaffPersonnelMapper personnelMapper,
                             StaffCertificateMapper certificateMapper,
                             StaffCertReminderMapper reminderMapper,
                             StaffPositionGrantMapper grantMapper,
                             StaffTempGrantMapper tempGrantMapper,
                             StaffAccessService accessService) {
        this.personnelMapper = personnelMapper;
        this.certificateMapper = certificateMapper;
        this.reminderMapper = reminderMapper;
        this.grantMapper = grantMapper;
        this.tempGrantMapper = tempGrantMapper;
        this.accessService = accessService;
    }

    /** 列表行（含有效权限剖面），可按状态/岗位/仅看有效过滤。 */
    public List<Map<String, Object>> listRows(String status, String postCode, Boolean accessOnly,
                                              LocalDateTime now) {
        QueryWrapper<StaffPersonnel> wrapper = new QueryWrapper<StaffPersonnel>().orderByAsc("id");
        wrapper.eq(status != null && !status.trim().isEmpty(), "status", status);
        String post = postCode == null ? null : postCode.trim().toUpperCase();
        wrapper.eq(post != null && !post.isEmpty(), "current_post_code", post);
        List<StaffPersonnel> all = personnelMapper.selectList(wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StaffPersonnel staff : all) {
            StaffAccessService.AccessProfile profile = accessService.profileOf(staff, now);
            if (Boolean.TRUE.equals(accessOnly) && !profile.isAccessActive()) {
                continue;
            }
            rows.add(accessService.toRow(staff, profile));
        }
        return rows;
    }

    /** 人员资质授权详情：档案 + 有效权限 + 证书 + 任职历史 + 临时授权 + 近期提醒。 */
    public Map<String, Object> detail(Long staffId, LocalDateTime now) {
        StaffPersonnel staff = personnelMapper.selectById(staffId);
        if (staff == null) {
            throw new IllegalArgumentException("养殖人员不存在，id=" + staffId);
        }
        StaffAccessService.AccessProfile profile = accessService.profileOf(staff, now);
        Map<String, Object> detail = new LinkedHashMap<>(accessService.toRow(staff, profile));

        List<StaffCertificate> certs = certificateMapper.selectList(new QueryWrapper<StaffCertificate>()
                .eq("staff_id", staffId).orderByDesc("expiry_date"));
        List<Map<String, Object>> certViews = new ArrayList<>();
        for (StaffCertificate c : certs) {
            Map<String, Object> cv = new LinkedHashMap<>();
            cv.put("certId", c.getId());
            cv.put("certNo", c.getCertNo());
            cv.put("certName", c.getCertName());
            cv.put("postCode", c.getPostCode());
            cv.put("issueDate", c.getIssueDate());
            cv.put("expiryDate", c.getExpiryDate());
            cv.put("remindBeforeDays", c.getRemindBeforeDays());
            cv.put("status", c.getStatus());
            cv.put("revokeReason", c.getRevokeReason());
            certViews.add(cv);
        }
        detail.put("certificates", certViews);

        List<StaffPositionGrant> grants = grantMapper.selectList(new QueryWrapper<StaffPositionGrant>()
                .eq("staff_id", staffId).orderByDesc("id"));
        detail.put("positionGrants", grants);

        List<StaffTempGrant> temps = tempGrantMapper.selectList(new QueryWrapper<StaffTempGrant>()
                .eq("staff_id", staffId).orderByDesc("id"));
        detail.put("tempGrants", temps);

        List<StaffCertReminder> reminders = reminderMapper.selectList(new QueryWrapper<StaffCertReminder>()
                .eq("staff_id", staffId).orderByDesc("id"));
        detail.put("certReminders", reminders);
        return detail;
    }

    /**
     * CSV 导出：列与列表行字段一一对应，同一 loadRows 口径；冻结/离场/过期人员同样导出但权限列为否，
     * 便于验收时把导出与列表、详情逐行核对。
     */
    public String exportCsv(String status, String postCode, Boolean accessOnly, LocalDateTime now) {
        List<Map<String, Object>> rows = listRows(status, postCode, accessOnly, now);
        StringBuilder sb = new StringBuilder("﻿");
        List<String> headers = new ArrayList<>();
        headers.add("工号");
        headers.add("姓名");
        headers.add("电话");
        headers.add("人员状态");
        headers.add("当前岗位");
        headers.add("权限有效");
        headers.add("有效岗位");
        headers.add("有效菜单");
        headers.add("拒绝/收权原因");
        sb.append(String.join(",", headers)).append("\r\n");
        for (Map<String, Object> row : rows) {
            List<String> cols = new ArrayList<>();
            cols.add(csv(row.get("staffNo")));
            cols.add(csv(row.get("staffName")));
            cols.add(csv(row.get("phone")));
            cols.add(csv(row.get("status")));
            cols.add(csv(row.get("currentPostName")));
            cols.add(Boolean.TRUE.equals(row.get("accessActive")) ? "是" : "否");
            cols.add(csv(join(row.get("activePostCodes"))));
            cols.add(csv(join(row.get("activeMenus"))));
            cols.add(csv(row.get("denyReason")));
            sb.append(String.join(",", cols)).append("\r\n");
        }
        return sb.toString();
    }

    private String join(Object value) {
        if (!(value instanceof List)) {
            return "";
        }
        List<?> list = (List<?>) value;
        List<String> strs = new ArrayList<>();
        for (Object o : list) {
            strs.add(String.valueOf(o));
        }
        return String.join("|", strs);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
