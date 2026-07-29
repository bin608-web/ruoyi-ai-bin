package org.ruoyi.controller.skill;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.domain.bo.skill.UserSkillBo;
import org.ruoyi.domain.dto.skill.UserSkillTestResult;
import org.ruoyi.domain.entity.skill.SkillRating;
import org.ruoyi.domain.entity.skill.SkillShareRecord;
import org.ruoyi.domain.entity.skill.SkillSubscription;
import org.ruoyi.domain.vo.skill.UserSkillVo;
import org.ruoyi.mapper.skill.SkillRatingMapper;
import org.ruoyi.mapper.skill.SkillShareRecordMapper;
import org.ruoyi.mapper.skill.SkillSubscriptionMapper;
import org.ruoyi.service.skill.IUserSkillService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理后台 Skills 管理 Controller
 * 为 admin 前端提供统一的 /skills 路径接口
 *
 * @author ruoyi
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/skills")
public class SkillsAdminController extends BaseController {

    private final IUserSkillService userSkillService;
    private final SkillShareRecordMapper skillShareRecordMapper;
    private final SkillSubscriptionMapper skillSubscriptionMapper;
    private final SkillRatingMapper skillRatingMapper;

    // ==================== 我的技能 /skills/my/* ====================

    /**
     * 获取我的技能分页列表
     */
    @SaCheckPermission("skill:my:list")
    @GetMapping("/my/list")
    public TableDataInfo<UserSkillVo> myList(UserSkillBo bo, PageQuery pageQuery) {
        return userSkillService.selectPageList(bo, pageQuery);
    }

    /**
     * 获取我的所有技能（不分页）
     */
    @SaCheckPermission("skill:my:list")
    @GetMapping("/my/all")
    public R<List<UserSkillVo>> myAll(UserSkillBo bo) {
        List<UserSkillVo> list = userSkillService.queryList(bo);
        return R.ok(list);
    }

    /**
     * 获取我的技能详情
     */
    @SaCheckPermission("skill:my:query")
    @GetMapping("/my/detail/{id}")
    public R<Map<String, Object>> myDetail(@PathVariable Long id) {
        UserSkillVo skill = userSkillService.selectById(id);
        if (skill == null) {
            return R.fail("技能不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", skill.getId());
        result.put("name", skill.getSkillName());
        result.put("description", skill.getDescription());
        result.put("category", skill.getSkillType());
        result.put("code", skill.getSkillConfig());
        result.put("isPublic", "Y".equals(skill.getIsPublic()));
        result.put("version", "1.0.0");
        result.put("author", "当前用户");
        result.put("authorId", skill.getUserId());
        result.put("status", "Y".equals(skill.getIsEnabled()) ? "active" : "inactive");
        result.put("createTime", skill.getCreateTime());
        result.put("updateTime", skill.getCreateTime());
        result.put("shareCount", 0);
        result.put("useCount", 0);
        result.put("rating", 0.0);
        result.put("ratingCount", 0);
        result.put("readme", "");
        result.put("tags", Collections.emptyList());

        // 扩展详情字段
        result.put("dependencies", Collections.emptyList());
        result.put("configSchema", "");
        result.put("usageExamples", Collections.emptyList());

        // TODO: 版本历史待数据库表支持后从数据库查询
        List<Map<String, Object>> versionHistory = new ArrayList<>();
        Map<String, Object> v1 = new LinkedHashMap<>();
        v1.put("version", "1.0.0");
        v1.put("changelog", "初始版本");
        v1.put("author", "当前用户");
        v1.put("createTime", skill.getCreateTime());
        versionHistory.add(v1);
        result.put("versionHistory", versionHistory);

        return R.ok(result);
    }

    /**
     * 新增我的技能
     */
    @SaCheckPermission("skill:my:add")
    @Log(title = "我的技能管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/my/add")
    public R<Void> myAdd(@RequestBody Map<String, Object> data) {
        UserSkillBo bo = new UserSkillBo();
        bo.setSkillName((String) data.get("name"));
        bo.setSkillType((String) data.getOrDefault("category", "CUSTOM"));
        bo.setDescription((String) data.get("description"));
        bo.setSkillConfig((String) data.get("code"));
        bo.setSkillCode((String) data.getOrDefault("skillCode", "SKILL_" + System.currentTimeMillis()));
        bo.setIsEnabled("Y");
        bo.setIsPublic(Boolean.TRUE.equals(data.get("isPublic")) ? "Y" : "N");
        bo.setSkillCodeContent((String) data.get("code"));
        userSkillService.insert(bo);
        return R.ok();
    }

    /**
     * 更新我的技能
     */
    @SaCheckPermission("skill:my:edit")
    @Log(title = "我的技能管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/my/update")
    public R<Void> myUpdate(@RequestBody Map<String, Object> data) {
        UserSkillBo bo = new UserSkillBo();
        Object idObj = data.get("id");
        if (idObj == null) {
            return R.fail("技能 ID 不能为空");
        }
        bo.setId(((Number) idObj).longValue());

        if (data.containsKey("name")) {
            bo.setSkillName((String) data.get("name"));
        }
        if (data.containsKey("category")) {
            bo.setSkillType((String) data.get("category"));
        }
        if (data.containsKey("description")) {
            bo.setDescription((String) data.get("description"));
        }
        if (data.containsKey("code")) {
            bo.setSkillConfig((String) data.get("code"));
            bo.setSkillCodeContent((String) data.get("code"));
        }
        if (data.containsKey("isPublic")) {
            bo.setIsPublic(Boolean.TRUE.equals(data.get("isPublic")) ? "Y" : "N");
        }
        if (data.containsKey("status")) {
            String status = (String) data.get("status");
            bo.setIsEnabled("active".equals(status) ? "Y" : "N");
        }

        userSkillService.update(bo);
        return R.ok();
    }

    /**
     * 删除我的技能
     */
    @SaCheckPermission("skill:my:remove")
    @Log(title = "我的技能管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/my/delete/{ids}")
    public R<Void> myDelete(@PathVariable Long[] ids) {
        userSkillService.deleteByIds(List.of(ids));
        return R.ok();
    }

    /**
     * 切换技能公开状态
     */
    @SaCheckPermission("skill:my:edit")
    @Log(title = "我的技能管理", businessType = BusinessType.UPDATE)
    @PutMapping("/my/toggle-public/{id}")
    public R<Void> myTogglePublic(@PathVariable Long id, @RequestParam Boolean isPublic) {
        userSkillService.updateStatus(id, isPublic ? "Y" : "N");
        // 同时更新公开状态
        UserSkillBo bo = new UserSkillBo();
        bo.setId(id);
        bo.setIsPublic(isPublic ? "Y" : "N");
        userSkillService.update(bo);
        return R.ok();
    }

    /**
     * 获取我的技能统计
     */
    @SaCheckPermission("skill:my:list")
    @GetMapping("/my/stats")
    public R<Map<String, Object>> myStats() {
        Long userId = LoginHelper.getUserId();
        List<UserSkillVo> allSkills = userSkillService.queryList(new UserSkillBo());

        long totalSkills = allSkills.size();
        long publicSkills = allSkills.stream().filter(s -> "Y".equals(s.getIsPublic())).count();
        long privateSkills = totalSkills - publicSkills;
        long enabledSkills = allSkills.stream().filter(s -> "Y".equals(s.getIsEnabled())).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSkills", totalSkills);
        stats.put("publicSkills", publicSkills);
        stats.put("privateSkills", privateSkills);
        stats.put("sharedSkills", 0); // TODO: 分享计数待数据库表支持
        stats.put("totalUseCount", 0); // TODO: 使用次数统计待数据库表支持
        stats.put("totalRating", 0.0); // TODO: 评分统计待数据库表支持
        stats.put("enabledSkills", enabledSkills);

        // 返回前几个技能作为 topSkills
        List<Map<String, Object>> topSkills = allSkills.stream()
                .limit(5)
                .map(this::convertToMap)
                .collect(Collectors.toList());
        stats.put("topSkills", topSkills);

        return R.ok(stats);
    }

    /**
     * 导出我的技能
     */
    @SaCheckPermission("skill:my:export")
    @Log(title = "我的技能管理", businessType = BusinessType.EXPORT)
    @PostMapping("/my/export")
    public void myExport(@RequestBody(required = false) Map<String, Object> data, HttpServletResponse response) {
        List<UserSkillVo> list = userSkillService.queryList(new UserSkillBo());
        ExcelUtil.exportExcel(list, "我的技能", UserSkillVo.class, response);
    }

    /**
     * 测试技能（支持文件上传和参数）
     */
    @SaCheckPermission("skill:my:test")
    @Log(title = "我的技能测试", businessType = BusinessType.OTHER)
    @PostMapping("/my/test/{id}")
    public R<Map<String, Object>> myTest(@PathVariable Long id,
                                          @RequestParam(required = false) String params,
                                          @RequestParam(required = false) MultipartFile file) throws JsonProcessingException {
        // 组装测试输入
        Map<String, Object> testInput = new LinkedHashMap<>();
        if (params != null && !params.isEmpty()) {
            try {
                testInput.put("params", new com.fasterxml.jackson.databind.ObjectMapper().readValue(params, Map.class));
            } catch (Exception e) {
                testInput.put("params", params);
            }
        }
        if (file != null && !file.isEmpty()) {
            try {
                testInput.put("fileName", file.getOriginalFilename());
                testInput.put("fileSize", file.getSize());
                testInput.put("fileContent", new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.warn("读取上传文件失败：{}", e.getMessage());
            }
        }

        String testInputJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(testInput);
        UserSkillTestResult testResult = userSkillService.testSkill(id, testInputJson);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", testResult != null && testResult.isSuccess());
        result.put("result", testResult != null ? testResult.getOutput() : null);
        result.put("message", testResult != null ? testResult.getMessage() : "测试完成");
        result.put("executionTime", testResult != null ? testResult.getExecutionTime() : 0);
        return R.ok(result);
    }

    /**
     * 获取技能版本历史
     */
    @SaCheckPermission("skill:my:query")
    @GetMapping("/my/version-history/{id}")
    public R<List<Map<String, Object>>> myVersionHistory(@PathVariable Long id) {
        // TODO: 版本历史待数据库表支持后从数据库查询
        UserSkillVo skill = userSkillService.selectById(id);
        if (skill == null) {
            return R.fail("技能不存在");
        }

        List<Map<String, Object>> history = new ArrayList<>();
        Map<String, Object> v1 = new LinkedHashMap<>();
        v1.put("version", "1.0.0");
        v1.put("changelog", "初始版本");
        v1.put("author", "当前用户");
        v1.put("createTime", skill.getCreateTime());
        history.add(v1);

        return R.ok(history);
    }

    // ==================== 他人分享给我的技能 /skills/shared/* ====================

    /**
     * 获取他人分享给我的技能列表（从 skill_share_record 表查询）
     */
    @SaCheckPermission("skill:shared:list")
    @GetMapping("/shared/list")
    public TableDataInfo<Map<String, Object>> sharedList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            PageQuery pageQuery) {
        Long userId = LoginHelper.getUserId();

        // 查询分享给我的记录
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SkillShareRecord> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(SkillShareRecord::getToUserId, userId);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(SkillShareRecord::getStatus, status.toUpperCase());
        }
        wrapper.orderByDesc(SkillShareRecord::getCreateTime);

        // 分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SkillShareRecord> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        page = skillShareRecordMapper.selectPage(page, wrapper);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SkillShareRecord record : page.getRecords()) {
            // 获取关联的技能信息
            UserSkillVo skill = userSkillService.selectById(record.getSkillId());

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", record.getId());
            map.put("name", record.getSkillName());
            map.put("description", skill != null ? skill.getDescription() : "");
            map.put("version", "1.0.0");
            map.put("sharedBy", record.getFromUserName() != null ? record.getFromUserName() : "用户" + record.getFromUserId());
            map.put("sharedByUserId", record.getFromUserId());
            map.put("sharedTime", record.getCreateTime());
            map.put("status", record.getStatus() != null ? record.getStatus().toLowerCase() : "pending");
            map.put("isPublic", "PUBLIC".equals(record.getShareType()));
            if (skill != null) {
                map.put("category", skill.getSkillType());
            }
            rows.add(map);
        }

        return new TableDataInfo<>(rows, page.getTotal());
    }

    /**
     * 接受分享的技能
     */
    @SaCheckPermission("skill:shared:accept")
    @Log(title = "分享技能管理", businessType = BusinessType.UPDATE)
    @PostMapping("/shared/accept/{id}")
    public R<Void> sharedAccept(@PathVariable Long id) {
        Long userId = LoginHelper.getUserId();
        SkillShareRecord record = skillShareRecordMapper.selectById(id);
        if (record == null) {
            return R.fail("分享记录不存在");
        }
        if (!userId.equals(record.getToUserId())) {
            return R.fail("无权操作此分享记录");
        }
        record.setStatus("ACCEPTED");
        skillShareRecordMapper.updateById(record);
        // 将技能复制给接收者
        userSkillService.shareSkill(record.getSkillId(), userId, "PRIVATE", null);
        return R.ok();
    }

    /**
     * 拒绝分享的技能
     */
    @SaCheckPermission("skill:shared:reject")
    @Log(title = "分享技能管理", businessType = BusinessType.UPDATE)
    @PostMapping("/shared/reject/{id}")
    public R<Void> sharedReject(@PathVariable Long id) {
        Long userId = LoginHelper.getUserId();
        SkillShareRecord record = skillShareRecordMapper.selectById(id);
        if (record == null) {
            return R.fail("分享记录不存在");
        }
        if (!userId.equals(record.getToUserId())) {
            return R.fail("无权操作此分享记录");
        }
        record.setStatus("REJECTED");
        skillShareRecordMapper.updateById(record);
        return R.ok();
    }

    /**
     * 启用分享的技能
     */
    @SaCheckPermission("skill:shared:edit")
    @Log(title = "分享技能管理", businessType = BusinessType.UPDATE)
    @PostMapping("/shared/enable/{id}")
    public R<Void> sharedEnable(@PathVariable Long id) {
        Long userId = LoginHelper.getUserId();
        SkillShareRecord record = skillShareRecordMapper.selectById(id);
        if (record == null) {
            return R.fail("分享记录不存在");
        }
        if (!userId.equals(record.getToUserId())) {
            return R.fail("无权操作此分享记录");
        }
        // 启用对应的技能
        userSkillService.updateStatus(record.getSkillId(), "Y");
        return R.ok();
    }

    /**
     * 禁用分享的技能
     */
    @SaCheckPermission("skill:shared:edit")
    @Log(title = "分享技能管理", businessType = BusinessType.UPDATE)
    @PostMapping("/shared/disable/{id}")
    public R<Void> sharedDisable(@PathVariable Long id) {
        Long userId = LoginHelper.getUserId();
        SkillShareRecord record = skillShareRecordMapper.selectById(id);
        if (record == null) {
            return R.fail("分享记录不存在");
        }
        if (!userId.equals(record.getToUserId())) {
            return R.fail("无权操作此分享记录");
        }
        userSkillService.updateStatus(record.getSkillId(), "N");
        return R.ok();
    }

    // ==================== 公开技能市场 /skills/market/* ====================

    /**
     * 获取公开技能市场分页列表
     */
    @GetMapping("/market/list")
    public TableDataInfo<Map<String, Object>> marketList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            PageQuery pageQuery) {
        // 从数据库查询所有公开的技能
        UserSkillBo bo = new UserSkillBo();
        bo.setIsPublic("Y");
        bo.setIsEnabled("Y");
        List<UserSkillVo> allSkills = userSkillService.queryList(bo);

        // 过滤和搜索
        List<UserSkillVo> filtered = allSkills.stream()
                .filter(skill -> category == null || category.isEmpty() || category.equals(skill.getSkillType()))
                .filter(skill -> search == null || search.isEmpty() ||
                        (skill.getSkillName() != null && skill.getSkillName().contains(search)) ||
                        (skill.getDescription() != null && skill.getDescription().contains(search)))
                .collect(Collectors.toList());

        // 转换为前端期望的格式（包含订阅和评分数据）
        Long currentUserId = LoginHelper.getUserId();
        List<Map<String, Object>> rows = filtered.stream()
                .map(skill -> {
                    // 查询订阅数
                    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SkillSubscription> subWrapper =
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                    subWrapper.eq(SkillSubscription::getSkillId, skill.getId())
                        .eq(SkillSubscription::getStatus, "ACTIVE");
                    long subscribeCount = skillSubscriptionMapper.selectCount(subWrapper);

                    // 查询当前用户是否已订阅
                    boolean isSubscribed = skillSubscriptionMapper.selectBySkillAndUser(skill.getId(), currentUserId) != null;

                    // 查询评分
                    Double avgRating = skillRatingMapper.selectAverageRating(skill.getId());
                    Long ratingCount = skillRatingMapper.selectRatingCount(skill.getId());

                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", skill.getId());
                    map.put("name", skill.getSkillName());
                    map.put("description", skill.getDescription());
                    map.put("category", skill.getSkillType());
                    map.put("version", "1.0.0");
                    map.put("author", "用户" + skill.getUserId());
                    map.put("authorId", skill.getUserId());
                    map.put("status", "active");
                    map.put("createTime", skill.getCreateTime());
                    map.put("updateTime", skill.getCreateTime());
                    map.put("tags", Collections.emptyList());
                    map.put("downloadCount", 0);
                    map.put("subscribeCount", subscribeCount);
                    map.put("isSubscribed", isSubscribed);
                    map.put("rating", avgRating != null ? avgRating : 0.0);
                    map.put("ratingCount", ratingCount != null ? ratingCount : 0);
                    map.put("latestVersion", "1.0.0");
                    map.put("changelog", Collections.emptyList());
                    return map;
                })
                .collect(Collectors.toList());

        // 手动分页
        int pageNum = pageQuery.getPageNum() != null ? pageQuery.getPageNum() : PageQuery.DEFAULT_PAGE_NUM;
        int pageSize = pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 20;
        int total = rows.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(pageNum * pageSize, total);
        List<Map<String, Object>> pageRows = fromIndex < total ? rows.subList(fromIndex, toIndex) : Collections.emptyList();

        return new TableDataInfo<>(pageRows, total);
    }

    /**
     * 获取所有公开技能（不分页）
     */
    @GetMapping("/market/all")
    public R<List<Map<String, Object>>> marketAll() {
        UserSkillBo bo = new UserSkillBo();
        bo.setIsPublic("Y");
        bo.setIsEnabled("Y");
        List<UserSkillVo> allSkills = userSkillService.queryList(bo);

        List<Map<String, Object>> result = allSkills.stream()
                .map(skill -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", skill.getId());
                    map.put("name", skill.getSkillName());
                    map.put("description", skill.getDescription());
                    map.put("category", skill.getSkillType());
                    map.put("version", "1.0.0");
                    map.put("author", "用户" + skill.getUserId());
                    map.put("authorId", skill.getUserId());
                    map.put("status", "active");
                    map.put("createTime", skill.getCreateTime());
                    map.put("updateTime", skill.getCreateTime());
                    map.put("tags", Collections.emptyList());
                    map.put("downloadCount", 0);
                    map.put("subscribeCount", 0);
                    map.put("isSubscribed", false);
                    map.put("rating", 0.0);
                    map.put("ratingCount", 0);
                    map.put("latestVersion", "1.0.0");
                    map.put("changelog", Collections.emptyList());
                    return map;
                })
                .collect(Collectors.toList());

        return R.ok(result);
    }

    /**
     * 获取技能市场详情
     */
    @GetMapping("/market/detail/{id}")
    public R<Map<String, Object>> marketDetail(@PathVariable Long id) {
        UserSkillVo skill = userSkillService.selectById(id);
        if (skill == null) {
            return R.fail("技能不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", skill.getId());
        result.put("name", skill.getSkillName());
        result.put("description", skill.getDescription());
        result.put("category", skill.getSkillType());
        result.put("version", "1.0.0");
        result.put("author", "用户" + skill.getUserId());
        result.put("authorId", skill.getUserId());
        result.put("code", skill.getSkillConfig());
        result.put("status", "Y".equals(skill.getIsEnabled()) ? "active" : "inactive");
        result.put("createTime", skill.getCreateTime());
        result.put("updateTime", skill.getCreateTime());
        result.put("tags", Collections.emptyList());
        result.put("downloadCount", 0);
        result.put("subscribeCount", 0);
        result.put("isSubscribed", false);
        result.put("rating", 0.0);
        result.put("ratingCount", 0);
        result.put("latestVersion", "1.0.0");
        result.put("changelog", Collections.emptyList());
        result.put("dependencies", Collections.emptyList());
        result.put("configSchema", "");
        result.put("usageExamples", Collections.emptyList());

        return R.ok(result);
    }

    /**
     * 订阅技能
     */
    @Log(title = "技能市场", businessType = BusinessType.OTHER)
    @PostMapping("/market/subscribe/{id}")
    public R<Void> marketSubscribe(@PathVariable Long id) {
        Long userId = LoginHelper.getUserId();
        // 检查是否已订阅
        SkillSubscription existing = skillSubscriptionMapper.selectBySkillAndUser(id, userId);
        if (existing != null && "ACTIVE".equals(existing.getStatus())) {
            return R.fail("已订阅该技能");
        }
        SkillSubscription subscription = new SkillSubscription();
        subscription.setSkillId(id);
        subscription.setUserId(userId);
        subscription.setStatus("ACTIVE");
        skillSubscriptionMapper.insert(subscription);
        return R.ok();
    }

    /**
     * 取消订阅技能
     */
    @Log(title = "技能市场", businessType = BusinessType.OTHER)
    @PostMapping("/market/unsubscribe/{id}")
    public R<Void> marketUnsubscribe(@PathVariable Long id) {
        Long userId = LoginHelper.getUserId();
        SkillSubscription existing = skillSubscriptionMapper.selectBySkillAndUser(id, userId);
        if (existing == null) {
            return R.fail("未订阅该技能");
        }
        existing.setStatus("CANCELLED");
        skillSubscriptionMapper.updateById(existing);
        return R.ok();
    }

    /**
     * 下载技能
     */
    @Log(title = "技能市场", businessType = BusinessType.OTHER)
    @PostMapping("/market/download/{id}")
    public R<Map<String, Object>> marketDownload(@PathVariable Long id) {
        UserSkillVo skill = userSkillService.selectById(id);
        if (skill == null) {
            return R.fail("技能不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", skill.getSkillConfig());
        result.put("name", skill.getSkillName());
        result.put("description", skill.getDescription());
        return R.ok(result);
    }

    /**
     * 评价技能
     */
    @Log(title = "技能市场", businessType = BusinessType.OTHER)
    @PostMapping("/market/rate")
    public R<Void> marketRate(@RequestBody Map<String, Object> data) {
        Long userId = LoginHelper.getUserId();
        Long skillId = data.get("skillId") != null ? ((Number) data.get("skillId")).longValue() : null;
        Integer rating = data.get("rating") != null ? ((Number) data.get("rating")).intValue() : null;
        String comment = (String) data.get("comment");

        if (skillId == null || rating == null || rating < 1 || rating > 5) {
            return R.fail("参数无效：评分需在 1-5 之间");
        }

        // 检查是否已评分
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SkillRating> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(SkillRating::getSkillId, skillId).eq(SkillRating::getUserId, userId);
        SkillRating existing = skillRatingMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setRating(rating);
            existing.setComment(comment);
            skillRatingMapper.updateById(existing);
        } else {
            SkillRating skillRating = new SkillRating();
            skillRating.setSkillId(skillId);
            skillRating.setUserId(userId);
            skillRating.setRating(rating);
            skillRating.setComment(comment);
            skillRatingMapper.insert(skillRating);
        }

        log.info("技能评分: skillId={}, userId={}, rating={}", skillId, userId, rating);
        return R.ok();
    }

    /**
     * 导出技能市场数据
     */
    @Log(title = "技能市场", businessType = BusinessType.EXPORT)
    @PostMapping("/market/export")
    public void marketExport(@RequestBody(required = false) Map<String, Object> data, HttpServletResponse response) {
        UserSkillBo bo = new UserSkillBo();
        bo.setIsPublic("Y");
        List<UserSkillVo> list = userSkillService.queryList(bo);
        ExcelUtil.exportExcel(list, "技能市场", UserSkillVo.class, response);
    }

    // ==================== 分享管理 /skills/share/* ====================

    /**
     * 获取分享记录列表（从 skill_share_record 表查询）
     */
    @SaCheckPermission("skill:share:list")
    @GetMapping("/share/records")
    public TableDataInfo<Map<String, Object>> shareRecords(
            @RequestParam(required = false) String keyword,
            PageQuery pageQuery) {
        Long userId = LoginHelper.getUserId();

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SkillShareRecord> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(SkillShareRecord::getFromUserId, userId);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SkillShareRecord::getSkillName, keyword)
                .or().like(SkillShareRecord::getToUserName, keyword));
        }
        wrapper.orderByDesc(SkillShareRecord::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SkillShareRecord> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        page = skillShareRecordMapper.selectPage(page, wrapper);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SkillShareRecord record : page.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", record.getId());
            map.put("skillId", record.getSkillId());
            map.put("skillName", record.getSkillName());
            map.put("sharedTo", record.getToUserName() != null ? record.getToUserName() : "用户" + record.getToUserId());
            map.put("sharedToUserId", record.getToUserId());
            map.put("sharedTime", record.getCreateTime());
            map.put("status", record.getStatus() != null ? record.getStatus().toLowerCase() : "active");
            rows.add(map);
        }

        return new TableDataInfo<>(rows, page.getTotal());
    }

    /**
     * 创建分享
     */
    @SaCheckPermission("skill:my:share")
    @Log(title = "技能分享", businessType = BusinessType.OTHER)
    @RepeatSubmit()
    @PostMapping("/share/create")
    public R<Void> shareCreate(@RequestBody Map<String, Object> data) {
        Long userId = LoginHelper.getUserId();
        Long skillId = data.get("skillId") != null ? ((Number) data.get("skillId")).longValue() : null;
        @SuppressWarnings("unchecked")
        List<Integer> userIdsRaw = (List<Integer>) data.get("userIds");

        if (skillId == null || userIdsRaw == null || userIdsRaw.isEmpty()) {
            return R.fail("技能 ID 和用户列表不能为空");
        }

        // 获取技能信息
        UserSkillVo skill = userSkillService.selectById(skillId);
        if (skill == null) {
            return R.fail("技能不存在");
        }

        // 使用现有的分享服务，逐个分享给用户，并记录分享记录
        for (Integer targetUserId : userIdsRaw) {
            userSkillService.shareSkill(skillId, targetUserId.longValue(), "PRIVATE", null);

            // 创建分享记录
            SkillShareRecord record = new SkillShareRecord();
            record.setSkillId(skillId);
            record.setSkillName(skill.getSkillName());
            record.setFromUserId(userId);
            record.setToUserId(targetUserId.longValue());
            record.setToUserName("用户" + targetUserId);
            record.setShareType("PRIVATE");
            record.setStatus("PENDING");
            skillShareRecordMapper.insert(record);
        }

        return R.ok();
    }

    /**
     * 撤销分享
     */
    @SaCheckPermission("skill:share:revoke")
    @Log(title = "技能分享", businessType = BusinessType.UPDATE)
    @PostMapping("/share/revoke/{id}")
    public R<Void> shareRevoke(@PathVariable Long id) {
        Long userId = LoginHelper.getUserId();
        SkillShareRecord record = skillShareRecordMapper.selectById(id);
        if (record == null) {
            return R.fail("分享记录不存在");
        }
        if (!userId.equals(record.getFromUserId())) {
            return R.fail("无权操作此分享记录");
        }
        record.setStatus("REVOKED");
        skillShareRecordMapper.updateById(record);
        return R.ok();
    }

    /**
     * 获取可分享的用户列表（从 sys_user 表查询）
     */
    @SaCheckPermission("skill:share:list")
    @GetMapping("/share/users")
    public R<List<Map<String, Object>>> shareUsers(
            @RequestParam(required = false) String keyword) {
        Long currentUserId = LoginHelper.getUserId();

        // 查询所有用户（排除自己）
        // 使用 MyBatis Plus 直接查询 sys_user 表
        List<Map<String, Object>> users = new ArrayList<>();

        try {
            // 通过 JdbcTemplate 或现有的 Mapper 查询用户列表
            // 这里使用一个简单的实现：返回空列表，标记为需要对接用户服务
            // 实际项目中应通过 SysUserService 查询
            log.info("TODO: 对接用户服务查询可分享用户列表, currentUserId={}", currentUserId);
        } catch (Exception e) {
            log.warn("查询用户列表失败: {}", e.getMessage());
        }

        return R.ok(users);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将 UserSkillVo 转换为前端期望的 Map 格式
     */
    /**
     * 解析技能配置中的输入参数 JSON 为 List
     */
    private List<Map<String, Object>> parseInputParams(String skillConfig) {
        if (skillConfig == null || skillConfig.isBlank()) {
            return Collections.emptyList();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(skillConfig);
            if (node.isArray()) {
                List<Map<String, Object>> params = new ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode item : node) {
                    Map<String, Object> param = new LinkedHashMap<>();
                    param.put("name", item.path("name").asText(""));
                    param.put("type", item.path("type").asText("string"));
                    param.put("label", item.path("label").asText(item.path("name").asText("")));
                    param.put("required", item.path("required").asBoolean(false));
                    if (item.has("defaultValue") && !item.get("defaultValue").isNull()) {
                        param.put("defaultValue", item.get("defaultValue").asText());
                    }
                    param.put("description", item.path("description").asText(""));
                    params.add(param);
                }
                return params;
            } else {
                // 尝试解析 JSON 字符串
                String json = skillConfig.trim();
                if (json.startsWith("\"") && json.endsWith("\"")) {
                    json = json.substring(1, json.length() - 1).replace("\\\"", "\"");
                }
                if (json.startsWith("[")) {
                    return parseInputParams(json);
                }
            }
        } catch (Exception e) {
            log.debug("解析输入参数失败：{}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private Map<String, Object> convertToMap(UserSkillVo skill) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", skill.getId());
        map.put("name", skill.getSkillName());
        map.put("description", skill.getDescription());
        map.put("category", skill.getSkillType());
        map.put("code", skill.getSkillConfig());
        map.put("isPublic", "Y".equals(skill.getIsPublic()));
        map.put("status", "Y".equals(skill.getIsEnabled()) ? "active" : "inactive");
        map.put("version", "1.0.0");
        map.put("author", "用户" + skill.getUserId());
        map.put("authorId", skill.getUserId());
        map.put("createTime", skill.getCreateTime());
        map.put("updateTime", skill.getCreateTime());
        map.put("shareCount", 0);
        map.put("useCount", 0);
        map.put("rating", 0.0);
        map.put("ratingCount", 0);
        map.put("readme", "");
        map.put("tags", Collections.emptyList());
        return map;
    }

    // ==================== 聊天前端公开接口 /skills/user/*, /skills/categories, /skills/enabled 等 ====================

    /**
     * 获取用户自己的 Skills 列表（聊天前端用）
     */
    @GetMapping("/user/list")
    public R<List<Map<String, Object>>> getUserSkills() {
        Long userId = LoginHelper.getUserId();
        List<UserSkillVo> skills = userSkillService.getAvailableSkills(userId);

        List<Map<String, Object>> result = skills.stream().map(skill -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", skill.getId());
            vo.put("name", skill.getSkillName());
            vo.put("description", skill.getDescription());
            vo.put("category", skill.getSkillType());
            vo.put("enabled", "Y".equals(skill.getIsEnabled()));
            vo.put("isPublic", "Y".equals(skill.getIsPublic()));
            vo.put("usageCount", 0);
            vo.put("createdAt", skill.getCreateTime());
            vo.put("updatedAt", skill.getCreateTime());
            vo.put("inputParams", parseInputParams(skill.getSkillConfig()));

            Map<String, Object> author = new LinkedHashMap<>();
            author.put("id", userId);
            author.put("name", "当前用户");
            vo.put("author", author);

            return vo;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    /**
     * 获取他人分享的 Skills 列表（聊天前端用）
     */
    @GetMapping("/shared/public")
    public R<List<Map<String, Object>>> getSharedSkills(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        List<UserSkillVo> allSkills = userSkillService.queryList(new UserSkillBo());

        List<UserSkillVo> publicSkills = allSkills.stream()
                .filter(skill -> "Y".equals(skill.getIsPublic()))
                .filter(skill -> category == null || category.equals(skill.getSkillType()))
                .filter(skill -> search == null || search.isEmpty() ||
                        (skill.getSkillName() != null && skill.getSkillName().contains(search)) ||
                        (skill.getDescription() != null && skill.getDescription().contains(search)))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = publicSkills.stream().map(skill -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", skill.getId());
            vo.put("name", skill.getSkillName());
            vo.put("description", skill.getDescription());
            vo.put("category", skill.getSkillType());
            vo.put("enabled", "Y".equals(skill.getIsEnabled()));
            vo.put("isPublic", "Y".equals(skill.getIsPublic()));
            vo.put("usageCount", 0);
            vo.put("createdAt", skill.getCreateTime());
            vo.put("updatedAt", skill.getCreateTime());
            vo.put("inputParams", parseInputParams(skill.getSkillConfig()));
            return vo;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    /**
     * 获取 Skill 分类列表（聊天前端用）
     */
    @GetMapping("/categories")
    public R<List<Map<String, Object>>> getSkillCategories() {
        List<Map<String, Object>> categories = Arrays.asList(
                createCategory(1L, "文件操作", "文件读取、写入、转换等操作", 0),
                createCategory(2L, "数据处理", "数据转换、分析、处理等操作", 0),
                createCategory(3L, "网络请求", "HTTP 请求、API 调用等操作", 0),
                createCategory(4L, "文本处理", "文本分析、提取、转换等操作", 0),
                createCategory(5L, "AI 相关", "AI 模型调用、提示词工程等", 0)
        );
        return R.ok(categories);
    }

    private Map<String, Object> createCategory(Long id, String name, String description, int count) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("description", description);
        map.put("icon", "file");
        map.put("skillCount", count);
        return map;
    }

    /**
     * 获取已启用的 Skills 列表（聊天前端用）
     */
    @GetMapping("/enabled")
    public R<List<Map<String, Object>>> getEnabledSkills() {
        Long userId = LoginHelper.getUserId();
        List<UserSkillVo> skills = userSkillService.getAvailableSkills(userId);

        List<UserSkillVo> enabledSkills = skills.stream()
                .filter(skill -> "Y".equals(skill.getIsEnabled()))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = enabledSkills.stream().map(skill -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", skill.getId());
            vo.put("name", skill.getSkillName());
            vo.put("description", skill.getDescription());
            vo.put("category", skill.getSkillType());
            vo.put("enabled", "Y".equals(skill.getIsEnabled()));
            vo.put("isPublic", "Y".equals(skill.getIsPublic()));
            vo.put("usageCount", 0);
            vo.put("createdAt", skill.getCreateTime());
            vo.put("updatedAt", skill.getCreateTime());
            vo.put("inputParams", parseInputParams(skill.getSkillConfig()));
            return vo;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    /**
     * 获取 Skill 详情（聊天前端用）
     */
    @GetMapping("/{id}")
    public R<Map<String, Object>> getSkillById(@PathVariable Long id) {
        UserSkillVo skill = userSkillService.selectById(id);
        if (skill == null) {
            return R.fail("技能不存在");
        }

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", skill.getId());
        vo.put("name", skill.getSkillName());
        vo.put("description", skill.getDescription());
        vo.put("category", skill.getSkillType());
        vo.put("enabled", "Y".equals(skill.getIsEnabled()));
        vo.put("isPublic", "Y".equals(skill.getIsPublic()));
        vo.put("usageCount", 0);
        vo.put("createdAt", skill.getCreateTime());
        vo.put("updatedAt", skill.getCreateTime());

        return R.ok(vo);
    }

    /**
     * 创建新 Skill（聊天前端用）
     */
    @PostMapping
    public R<Map<String, Object>> createSkill(@RequestBody Map<String, Object> data) {
        Long userId = LoginHelper.getUserId();

        UserSkillBo bo = new UserSkillBo();
        bo.setSkillName((String) data.get("name"));
        bo.setDescription((String) data.get("description"));
        bo.setSkillType((String) data.get("category"));
        bo.setSkillConfig((String) data.get("code"));
        bo.setIsEnabled("Y");
        bo.setIsPublic("N");

        userSkillService.insert(bo);

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", bo.getId());
        vo.put("name", bo.getSkillName());
        vo.put("description", bo.getDescription());
        vo.put("category", bo.getSkillType());
        vo.put("enabled", true);
        vo.put("isPublic", false);

        return R.ok(vo);
    }

    /**
     * 更新 Skill（聊天前端用）
     */
    @PostMapping("/{id}/update")
    public R<Void> updateSkill(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        UserSkillBo bo = new UserSkillBo();
        bo.setId(id);
        bo.setSkillName((String) data.get("name"));
        bo.setDescription((String) data.get("description"));
        bo.setSkillType((String) data.get("category"));
        bo.setSkillConfig((String) data.get("code"));

        if (data.containsKey("enabled")) {
            bo.setIsEnabled((Boolean) data.get("enabled") ? "Y" : "N");
        }

        userSkillService.update(bo);
        return R.ok();
    }

    /**
     * 删除 Skill（聊天前端用）
     */
    @PostMapping("/{id}/delete")
    public R<Void> deleteSkill(@PathVariable Long id) {
        userSkillService.deleteByIds(Collections.singletonList(id));
        return R.ok();
    }

    /**
     * 启用/禁用 Skill（聊天前端用）
     */
    @PostMapping("/{id}/toggle")
    public R<Void> toggleSkill(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Boolean enabled = (Boolean) data.get("enabled");
        userSkillService.updateStatus(id, enabled ? "Y" : "N");
        return R.ok();
    }

    /**
     * 测试 Skill（聊天前端用）
     */
    @PostMapping("/test")
    public R<Map<String, Object>> testSkill(@RequestBody Map<String, Object> data) {
        Long skillId = Long.parseLong(data.get("skillId").toString());
        String inputData = JSONObject.toJSON(data.get("inputData")).toString();

        UserSkillTestResult result = userSkillService.testSkill(skillId, inputData);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", result != null && result.isSuccess());
        map.put("output", result != null ? result.getOutput() : null);
        map.put("error", result != null ? result.getError() : null);
        map.put("executionTime", result != null ? result.getExecutionTime() : 0);
        map.put("logs", new ArrayList<String>());

        return R.ok(map);
    }

    /**
     * 生成新 Skill（通过 AI，聊天前端用）
     */
    @PostMapping("/generate")
    public R<Map<String, Object>> generateSkill(@RequestBody Map<String, Object> data) {
        Long userId = LoginHelper.getUserId();
        String prompt = (String) data.get("prompt");
        Long categoryId = (Long) data.get("categoryId");
        String name = (String) data.get("name");

        String skillType = "LOCAL";
        if (categoryId != null) {
            switch (categoryId.intValue()) {
                case 1: skillType = "MCP"; break;
                case 2: skillType = "CUSTOM"; break;
                default: skillType = "LOCAL";
            }
        }

        org.ruoyi.domain.dto.skill.UserSkillDto dto = userSkillService.generateSkill(userId, prompt, skillType);

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", dto.getId());
        vo.put("name", dto.getSkillName());
        vo.put("description", dto.getDescription());
        vo.put("category", dto.getSkillType());
        vo.put("enabled", true);
        vo.put("isPublic", false);

        return R.ok(vo);
    }

    /**
     * 导入 Skill（聊天前端用）
     */
    @PostMapping("/import")
    public R<Map<String, Object>> importSkill(@RequestBody Map<String, Object> data) {
        UserSkillBo bo = new UserSkillBo();
        bo.setSkillName((String) data.getOrDefault("name", "导入的技能"));
        bo.setDescription((String) data.get("description"));
        bo.setSkillType("CUSTOM");
        bo.setSkillConfig((String) data.get("code"));
        bo.setIsEnabled("Y");
        bo.setIsPublic("N");

        userSkillService.insert(bo);

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", bo.getId());
        vo.put("name", bo.getSkillName());
        vo.put("description", bo.getDescription());
        vo.put("category", bo.getSkillType());
        vo.put("enabled", true);
        vo.put("isPublic", false);

        return R.ok(vo);
    }

    /**
     * 导出 Skill（聊天前端用）
     */
    @GetMapping("/{id}/export")
    public R<Map<String, Object>> exportSkill(@PathVariable Long id) {
        UserSkillVo skill = userSkillService.selectById(id);
        if (skill == null) {
            return R.fail("技能不存在");
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", skill.getSkillConfig());
        map.put("name", skill.getSkillName());
        map.put("description", skill.getDescription());

        return R.ok(map);
    }
}
