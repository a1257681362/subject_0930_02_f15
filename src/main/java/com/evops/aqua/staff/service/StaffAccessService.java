package com.evops.aqua.staff.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evops.aqua.staff.entity.StaffCertificate;
import com.evops.aqua.staff.entity.StaffPersonnel;
import com.evops.aqua.staff.entity.StaffPositionGrant;
import com.evops.aqua.staff.entity.StaffPostScope;
import com.evops.aqua.staff.entity.StaffTempGrant;
import com.evops.aqua.staff.mapper.StaffCertificateMapper;
import com.evops.aqua.staff.mapper.StaffPersonnelMapper;
import com.evops.aqua.staff.mapper.StaffPositionGrantMapper;
import com.evops.aqua.staff.mapper.StaffPostScopeMapper;
import com.evops.aqua.staff.mapper.StaffTempGrantMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 人员有效权限的唯一口径来源：列表、详情、CSV 导出、现场接口实时拦截都调用本类，
 * 杜绝"详情有权限、列表/导出却另算"的口径分叉。
 *
 * 有效权限 = 人员在职(ACTIVE，未冻结未离场)
 *          × 在任任职行(grant ACTIVE 且落在有效期)
 *          × 授权依据实时有效：正式岗凭有效证书（未过期未撤销），临时岗看临时授权时间窗。
 * 即使收权定时扫描尚未运行，只要证书已过期/临时授权已结束/人员已冻结，实时判定立即无权限——
 * 状态扫描（台账+证据）与实时判定双保险。
 */
@Service
public class StaffAccessService {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final StaffPersonnelMapper personnelMapper;
    private final StaffPositionGrantMapper grantMapper;
    private final StaffCertificateMapper certificateMapper;
    private final StaffTempGrantMapper tempGrantMapper;
    private final StaffPostScopeMapper postScopeMapper;

    public StaffAccessService(StaffPersonnelMapper personnelMapper,
                              StaffPositionGrantMapper grantMapper,
                              StaffCertificateMapper certificateMapper,
                              StaffTempGrantMapper tempGrantMapper,
                              StaffPostScopeMapper postScopeMapper) {
        this.personnelMapper = personnelMapper;
        this.grantMapper = grantMapper;
        this.certificateMapper = certificateMapper;
        this.tempGrantMapper = tempGrantMapper;
        this.postScopeMapper = postScopeMapper;
    }

    /** 单个岗位的有效操作范围快照。 */
    public static class PostAccess {
        private String postCode;
        private String postName;
        private String source;
        private String basisNo;
        private LocalDate certExpiry;
        private LocalDateTime grantEnd;
        private List<String> menus = new ArrayList<>();
        private List<String> apis = new ArrayList<>();

        public String getPostCode() { return postCode; }
        public void setPostCode(String postCode) { this.postCode = postCode; }
        public String getPostName() { return postName; }
        public void setPostName(String postName) { this.postName = postName; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getBasisNo() { return basisNo; }
        public void setBasisNo(String basisNo) { this.basisNo = basisNo; }
        public LocalDate getCertExpiry() { return certExpiry; }
        public void setCertExpiry(LocalDate certExpiry) { this.certExpiry = certExpiry; }
        public LocalDateTime getGrantEnd() { return grantEnd; }
        public void setGrantEnd(LocalDateTime grantEnd) { this.grantEnd = grantEnd; }
        public List<String> getMenus() { return menus; }
        public void setMenus(List<String> menus) { this.menus = menus; }
        public List<String> getApis() { return apis; }
        public void setApis(List<String> apis) { this.apis = apis; }
    }

    /** 人员有效权限剖面。 */
    public static class AccessProfile {
        private Long staffId;
        private String staffNo;
        private String staffName;
        private String staffStatus;
        private boolean accessActive;
        private String denyReason;
        private List<PostAccess> posts = new ArrayList<>();

        public Long getStaffId() { return staffId; }
        public void setStaffId(Long staffId) { this.staffId = staffId; }
        public String getStaffNo() { return staffNo; }
        public void setStaffNo(String staffNo) { this.staffNo = staffNo; }
        public String getStaffName() { return staffName; }
        public void setStaffName(String staffName) { this.staffName = staffName; }
        public String getStaffStatus() { return staffStatus; }
        public void setStaffStatus(String staffStatus) { this.staffStatus = staffStatus; }
        public boolean isAccessActive() { return accessActive; }
        public void setAccessActive(boolean accessActive) { this.accessActive = accessActive; }
        public String getDenyReason() { return denyReason; }
        public void setDenyReason(String denyReason) { this.denyReason = denyReason; }
        public List<PostAccess> getPosts() { return posts; }
        public void setPosts(List<PostAccess> posts) { this.posts = posts; }
    }

    public StaffPersonnel getByStaffNoOrThrow(String staffNo) {
        StaffPersonnel staff = personnelMapper.selectOne(
                new QueryWrapper<StaffPersonnel>().eq("staff_no", staffNo));
        if (staff == null) {
            throw new IllegalArgumentException("养殖人员不存在，工号=" + staffNo);
        }
        return staff;
    }

    public StaffPostScope getScopeOrThrow(String postCode) {
        StaffPostScope scope = postScopeMapper.selectOne(
                new QueryWrapper<StaffPostScope>().eq("post_code", normalizePost(postCode)));
        if (scope == null) {
            throw new IllegalArgumentException("岗位可操作范围未配置：" + postCode);
        }
        if (!StaffPostScope.STATUS_ACTIVE.equals(scope.getStatus())) {
            throw new IllegalArgumentException("岗位[" + scope.getPostName() + "]可操作范围已停用，暂不可授权");
        }
        return scope;
    }

    public static String normalizePost(String postCode) {
        if (postCode == null) {
            throw new IllegalArgumentException("岗位编码不能为空");
        }
        String p = postCode.trim().toUpperCase();
        if (!Arrays.asList(StaffPostScope.POST_VET, StaffPostScope.POST_SAMPLER,
                StaffPostScope.POST_FISHER, StaffPostScope.POST_EQUIPMENT).contains(p)) {
            throw new IllegalArgumentException(
                    "岗位编码非法（仅支持 VET/SAMPLER/FISHER/EQUIPMENT）：" + postCode);
        }
        return p;
    }

    /** 该人员该岗位当前是否持有效证书（状态 VALID 且到期日 >= today）。 */
    public StaffCertificate validCertificate(Long staffId, String postCode, LocalDate today) {
        List<StaffCertificate> certs = certificateMapper.selectList(new QueryWrapper<StaffCertificate>()
                .eq("staff_id", staffId)
                .eq("post_code", postCode)
                .eq("status", StaffCertificate.STATUS_VALID)
                .orderByAsc("expiry_date"));
        for (StaffCertificate cert : certs) {
            if (cert.getExpiryDate() != null && !cert.getExpiryDate().isBefore(today)) {
                return cert;
            }
        }
        return null;
    }

    /** 计算人员在指定时刻的有效权限剖面（唯一口径）。 */
    public AccessProfile profileOf(StaffPersonnel staff, LocalDateTime now) {
        AccessProfile profile = new AccessProfile();
        profile.setStaffId(staff.getId());
        profile.setStaffNo(staff.getStaffNo());
        profile.setStaffName(staff.getStaffName());
        profile.setStaffStatus(staff.getStatus());

        if (StaffPersonnel.STATUS_FROZEN.equals(staff.getStatus())) {
            profile.setAccessActive(false);
            profile.setDenyReason("人员已冻结，菜单与接口权限全部收回");
            return profile;
        }
        if (StaffPersonnel.STATUS_LEFT.equals(staff.getStatus())) {
            profile.setAccessActive(false);
            profile.setDenyReason("人员已离场，无任何操作权限");
            return profile;
        }

        LocalDate today = now.toLocalDate();
        List<StaffPositionGrant> grants = grantMapper.selectList(new QueryWrapper<StaffPositionGrant>()
                .eq("staff_id", staff.getId())
                .eq("status", StaffPositionGrant.STATUS_ACTIVE)
                .orderByAsc("id"));
        for (StaffPositionGrant grant : grants) {
            if (grant.getValidFrom() != null && grant.getValidFrom().isAfter(now)) {
                continue;
            }
            if (grant.getValidTo() != null && !grant.getValidTo().isAfter(now)) {
                continue; // [from, to) 半开区间
            }
            // 有效菜单/接口取岗位"当前生效"的范围配置：范围调整或调岗后列表/详情/导出/实时拦截即时同口径；
            // 授权与收权时另把当时范围冻结进 grant/revoke 台账，历史快照不受影响。
            StaffPostScope scope = postScopeMapper.selectOne(new QueryWrapper<StaffPostScope>()
                    .eq("post_code", grant.getPostCode())
                    .eq("status", StaffPostScope.STATUS_ACTIVE));
            if (scope == null) {
                continue; // 岗位范围已停用：该岗位暂无可操作菜单与接口
            }
            PostAccess access = new PostAccess();
            access.setPostCode(grant.getPostCode());
            access.setPostName(scope.getPostName());
            access.setSource(grant.getSource());
            access.setGrantEnd(grant.getValidTo());
            access.setMenus(splitCsv(scope.getMenuCodes()));
            access.setApis(splitCsv(scope.getApiScopes()));

            if (StaffPositionGrant.SOURCE_TEMP.equals(grant.getSource())) {
                StaffTempGrant temp = grant.getRefId() == null ? null
                        : tempGrantMapper.selectById(grant.getRefId());
                if (temp == null || !StaffTempGrant.STATUS_ACTIVE.equals(temp.getStatus())
                        || temp.getStartTime().isAfter(now) || !temp.getEndTime().isAfter(now)) {
                    continue; // 临时授权未开始/已结束/已撤销：实时无此岗位权限
                }
                access.setBasisNo(temp.getApprovalNo());
            } else {
                StaffCertificate cert = validCertificate(staff.getId(), grant.getPostCode(), today);
                if (cert == null) {
                    continue; // 证书已过期或撤销：实时立即收回该岗位菜单与接口
                }
                access.setBasisNo(cert.getCertNo());
                access.setCertExpiry(cert.getExpiryDate());
            }
            profile.getPosts().add(access);
        }

        profile.setAccessActive(!profile.getPosts().isEmpty());
        if (!profile.isAccessActive()) {
            profile.setDenyReason("无在任且资质有效的岗位授权（证书可能已过期或临时授权已结束）");
        }
        return profile;
    }

    public AccessProfile profileOf(String staffNo, LocalDateTime now) {
        return profileOf(getByStaffNoOrThrow(staffNo), now);
    }

    /** 有效菜单编码并集（列表/详情/导出口径一致）。 */
    public List<String> activeMenus(AccessProfile profile) {
        List<String> menus = new ArrayList<>();
        for (PostAccess post : profile.getPosts()) {
            for (String menu : post.getMenus()) {
                if (!menus.contains(menu)) {
                    menus.add(menu);
                }
            }
        }
        return menus;
    }

    /**
     * 现场人员接口实时校验：命中任一有效岗位的 Ant 接口范围即放行。
     * 冻结/离场/证书过期/临时到期立即拒绝，返回拒绝原因。
     */
    public AccessProfile checkApi(String staffNo, String requestUri, LocalDateTime now) {
        StaffPersonnel staff = getByStaffNoOrThrow(staffNo);
        AccessProfile profile = profileOf(staff, now);
        if (!profile.isAccessActive()) {
            return profile;
        }
        for (PostAccess post : profile.getPosts()) {
            for (String pattern : post.getApis()) {
                if (PATH_MATCHER.match(pattern, requestUri)) {
                    return profile;
                }
            }
        }
        profile.setAccessActive(false);
        profile.setDenyReason("人员[" + staffNo + "]当前岗位可操作范围不含接口：" + requestUri);
        return profile;
    }

    /** 组装列表/详情/导出共用的人员权限行（同一口径）。 */
    public Map<String, Object> toRow(StaffPersonnel staff, AccessProfile profile) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("staffId", staff.getId());
        row.put("staffNo", staff.getStaffNo());
        row.put("staffName", staff.getStaffName());
        row.put("phone", staff.getPhone());
        row.put("status", staff.getStatus());
        row.put("currentPostCode", staff.getCurrentPostCode());
        row.put("currentPostName", staff.getCurrentPostName());
        row.put("accessActive", profile.isAccessActive());
        row.put("denyReason", profile.getDenyReason());
        List<String> postCodes = new ArrayList<>();
        List<String> menuUnion = new ArrayList<>();
        List<Map<String, Object>> postViews = new ArrayList<>();
        for (PostAccess post : profile.getPosts()) {
            postCodes.add(post.getPostCode());
            for (String menu : post.getMenus()) {
                if (!menuUnion.contains(menu)) {
                    menuUnion.add(menu);
                }
            }
            Map<String, Object> pv = new LinkedHashMap<>();
            pv.put("postCode", post.getPostCode());
            pv.put("postName", post.getPostName());
            pv.put("source", post.getSource());
            pv.put("basisNo", post.getBasisNo());
            pv.put("certExpiry", post.getCertExpiry());
            pv.put("grantEnd", post.getGrantEnd());
            pv.put("menus", post.getMenus());
            pv.put("apis", post.getApis());
            postViews.add(pv);
        }
        row.put("activePostCodes", postCodes);
        row.put("activeMenus", menuUnion);
        row.put("activePosts", postViews);
        return row;
    }

    static List<String> splitCsv(String csv) {
        List<String> result = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) {
            return result;
        }
        for (String item : Arrays.asList(csv.split(","))) {
            String t = item.trim();
            if (!t.isEmpty() && !result.contains(t)) {
                result.add(t);
            }
        }
        return result;
    }
}
