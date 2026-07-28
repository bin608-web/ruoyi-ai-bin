package org.ruoyi.controller.mcp;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.base.ThreadContext;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.domain.bo.mcp.UserMcpConfigBo;
import org.ruoyi.domain.dto.mcp.McpToolListResult;
import org.ruoyi.domain.vo.mcp.UserMcpConfigVo;
import org.ruoyi.service.mcp.IUserMcpConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 MCP 配置管理 Controller
 *
 * @author ruoyi team
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/mcp/user-config")
@Slf4j
public class UserMcpConfigController extends BaseController {

    private final IUserMcpConfigService userMcpConfigService;

    /**
     * 查询用户 MCP 配置列表
     */
    @SaCheckPermission("mcp:user-config:list")
    @GetMapping("/list")
    public TableDataInfo<UserMcpConfigVo> list(UserMcpConfigBo bo, PageQuery pageQuery) {
        return userMcpConfigService.selectPageList(bo, pageQuery);
    }

    /**
     * 查询当前用户的 MCP 配置列表
     */
    @GetMapping("/my-list")
    public R<List<UserMcpConfigVo>> myList(UserMcpConfigBo bo) {
        Long userId = LoginHelper.getUserId();
        bo.setUserId(userId);
        List<UserMcpConfigVo> list = userMcpConfigService.queryList(bo);
        return R.ok(list);
    }

    /**
     * 导出用户 MCP 配置列表
     */
    @SaCheckPermission("mcp:user-config:export")
    @Log(title = "用户 MCP 配置管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(UserMcpConfigBo bo, HttpServletResponse response) {
        List<UserMcpConfigVo> list = userMcpConfigService.queryList(bo);
        ExcelUtil.exportExcel(list, "用户 MCP 配置", UserMcpConfigVo.class, response);
    }

    /**
     * 根据配置 ID 获取详细信息
     *
     * @param id 配置 ID
     */
    @SaCheckPermission("mcp:user-config:query")
    @GetMapping("/{id}")
    public R<UserMcpConfigVo> getInfo(@PathVariable Long id) {
        return R.ok(userMcpConfigService.selectById(id));
    }

    /**
     * 新增用户 MCP 配置
     */
    @SaCheckPermission("mcp:user-config:add")
    @Log(title = "用户 MCP 配置管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated @RequestBody UserMcpConfigBo bo) {
        userMcpConfigService.insert(bo);
        return R.ok();
    }

    /**
     * 批量新增用户 MCP 配置
     */
    @SaCheckPermission("mcp:user-config:add")
    @Log(title = "用户 MCP 配置管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/batch")
    public R<String> addBatch(@Validated @RequestBody UserMcpConfigBo bo) {
        String result = userMcpConfigService.insertBatch(bo);
        return R.ok(result);
    }

    /**
     * 修改用户 MCP 配置
     */
    @SaCheckPermission("mcp:user-config:edit")
    @Log(title = "用户 MCP 配置管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated @RequestBody UserMcpConfigBo bo) {
        userMcpConfigService.update(bo);
        return R.ok();
    }

    /**
     * 删除用户 MCP 配置
     *
     * @param ids 配置 ID 串
     */
    @SaCheckPermission("mcp:user-config:remove")
    @Log(title = "用户 MCP 配置管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        userMcpConfigService.deleteByIds(List.of(ids));
        return R.ok();
    }

    /**
     * 删除当前用户的所有配置
     */
    @SaCheckPermission("mcp:user-config:remove")
    @Log(title = "用户 MCP 配置管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/my-all")
    public R<Void> removeMyAll() {
        Long userId = LoginHelper.getUserId();
        userMcpConfigService.deleteByUserId(userId);
        return R.ok();
    }

    /**
     * 更新配置状态
     */
    @SaCheckPermission("mcp:user-config:edit")
    @Log(title = "用户 MCP 配置管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        userMcpConfigService.updateStatus(id, status);
        return R.ok();
    }

    /**
     * 获取当前用户可用的 MCP 工具列表
     */
    @GetMapping("/my-available-tools")
    public R<McpToolListResult> getMyAvailableTools() {
        Long userId = LoginHelper.getUserId();
        McpToolListResult result = userMcpConfigService.getUserAvailableTools(userId);
        return R.ok(result);
    }

    /**
     * 根据工具 ID 删除配置
     *
     * @param toolId 工具 ID
     */
    @SaCheckPermission("mcp:user-config:remove")
    @Log(title = "用户 MCP 配置管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/tool/{toolId}")
    public R<Void> removeByToolId(@PathVariable Long toolId) {
        userMcpConfigService.deleteByToolId(toolId);
        return R.ok();
    }
}
