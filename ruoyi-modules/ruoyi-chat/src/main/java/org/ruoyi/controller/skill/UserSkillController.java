package org.ruoyi.controller.skill;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import org.ruoyi.domain.dto.skill.UserSkillDto;
import org.ruoyi.domain.dto.skill.UserSkillTestResult;
import org.ruoyi.domain.vo.skill.UserSkillVo;
import org.ruoyi.service.skill.IUserSkillService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户技能管理 Controller
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/skill/my")
public class UserSkillController extends BaseController {

    private final IUserSkillService userSkillService;

    /**
     * 查询用户技能列表
     */
    @SaCheckPermission("skill:my:list")
    @GetMapping("/list")
    public TableDataInfo<UserSkillVo> list(UserSkillBo bo, PageQuery pageQuery) {
        return userSkillService.selectPageList(bo, pageQuery);
    }

    /**
     * 查询用户技能列表（不分页）
     */
    @SaCheckPermission("skill:my:list")
    @GetMapping("/all")
    public List<UserSkillVo> all(UserSkillBo bo) {
        return userSkillService.queryList(bo);
    }

    /**
     * 导出用户技能列表
     */
    @SaCheckPermission("skill:my:export")
    @Log(title = "用户技能管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(UserSkillBo bo, HttpServletResponse response) {
        List<UserSkillVo> list = userSkillService.queryList(bo);
        ExcelUtil.exportExcel(list, "用户技能", UserSkillVo.class, response);
    }

    /**
     * 根据技能 ID 获取详细信息
     */
    @SaCheckPermission("skill:my:query")
    @GetMapping("/{id}")
    public R<UserSkillVo> getInfo(@PathVariable Long id) {
        return R.ok(userSkillService.selectById(id));
    }

    /**
     * 根据技能编码获取详细信息
     */
    @SaCheckPermission("skill:my:query")
    @GetMapping("/code/{skillCode}")
    public R<UserSkillVo> getByCode(@PathVariable String skillCode) {
        return R.ok(userSkillService.selectBySkillCode(skillCode));
    }

    /**
     * 新增技能
     */
    @SaCheckPermission("skill:my:add")
    @Log(title = "用户技能管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated @RequestBody UserSkillBo bo) {
        userSkillService.insert(bo);
        return R.ok();
    }

    /**
     * 修改技能
     */
    @SaCheckPermission("skill:my:edit")
    @Log(title = "用户技能管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated @RequestBody UserSkillBo bo) {
        userSkillService.update(bo);
        return R.ok();
    }

    /**
     * 删除技能
     */
    @SaCheckPermission("skill:my:remove")
    @Log(title = "用户技能管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        userSkillService.deleteByIds(List.of(ids));
        return R.ok();
    }

    /**
     * 更新技能状态
     */
    @SaCheckPermission("skill:my:edit")
    @Log(title = "用户技能管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String isEnabled) {
        userSkillService.updateStatus(id, isEnabled);
        return R.ok();
    }

    /**
     * 测试技能
     */
    @SaCheckPermission("skill:my:test")
    @Log(title = "用户技能测试", businessType = BusinessType.OTHER)
    @PostMapping("/{id}/test")
    public R<UserSkillTestResult> testSkill(@PathVariable Long id, @RequestBody(required = false) UserSkillBo bo) {
        String testInput = bo != null ? bo.getTestInput() : null;
        return R.ok(userSkillService.testSkill(id, testInput));
    }

    /**
     * 生成技能（基于对话内容）
     */
    @SaCheckPermission("skill:my:add")
    @Log(title = "技能生成", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public R<UserSkillDto> generateSkill(@RequestBody UserSkillBo bo) {
        Long userId = LoginHelper.getUserId();
        // 优先用 description 作为 prompt，其次用 skillName
        String prompt = bo.getDescription() != null && !bo.getDescription().isBlank()
            ? bo.getDescription() : bo.getSkillName();
        // skillType 默认 LOCAL
        String skillType = bo.getSkillType() != null && !bo.getSkillType().isBlank()
            ? bo.getSkillType() : "LOCAL";
        return R.ok(userSkillService.generateSkill(userId, prompt, skillType));
    }

    /**
     * 分享技能
     */
    @SaCheckPermission("skill:my:share")
    @Log(title = "技能分享", businessType = BusinessType.OTHER)
    @PostMapping("/{id}/share")
    public R<Void> shareSkill(@PathVariable Long id,
                              @RequestParam(required = false) Long toUserId,
                              @RequestParam String shareType,
                              @RequestParam(required = false) String message) {
        userSkillService.shareSkill(id, toUserId, shareType, message);
        return R.ok();
    }

    /**
     * 获取用户可用的技能列表（用于聊天时选择）
     */
    @GetMapping("/available")
    public R<List<UserSkillVo>> getAvailableSkills() {
        Long userId = LoginHelper.getUserId();
        return R.ok(userSkillService.getAvailableSkills(userId));
    }
}
