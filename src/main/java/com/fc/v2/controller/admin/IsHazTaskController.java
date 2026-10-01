package com.fc.v2.controller.admin;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TIsHazTask;
import com.fc.v2.service.ITIsHazTaskService;
import com.fc.v2.service.dispatch.RoundResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 隐患整改催办条 Controller（scheduling-job 形状：明细台账 + 手动触发一轮 + 撤条/转办/调提前档）。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Api(value = "隐患整改催办条")
@Controller
@RequestMapping("/isHazTask")
public class IsHazTaskController extends BaseController {

    private final String prefix = "admin/isHazTask";

    @Autowired
    private ITIsHazTaskService isHazTaskService;

    @ApiOperation(value = "催办条明细台账跳转", notes = "含挂起条，撤条条留痕在留痕页查")
    @GetMapping("/view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "催办条明细台账", action = "list")
    @ApiOperation(value = "明细分页查询", notes = "挂起条仍在结果中；撤条条默认不列出")
    @GetMapping("/list")
    @ResponseBody
    public ResultTable list(TIsHazTask record) {
        QueryWrapper<TIsHazTask> queryWrapper = new QueryWrapper<TIsHazTask>()
                .eq("del_flag", 0)
                .orderByAsc("status")
                .orderByDesc("due_at");
        if (record.getStatus() != null) {
            queryWrapper.eq("status", record.getStatus());
        }
        if (record.getItemNo() != null && !record.getItemNo().trim().isEmpty()) {
            queryWrapper.like("item_no", record.getItemNo().trim());
        }
        startPage();
        com.github.pagehelper.PageInfo<TIsHazTask> page =
                new com.github.pagehelper.PageInfo<TIsHazTask>(isHazTaskService.selectList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "一条子的派发处置留痕", notes = "谁接手、催过几轮、撤转原由")
    @GetMapping("/logs")
    @ResponseBody
    public AjaxResult logs(Long id) {
        return AjaxResult.successData(200, isHazTaskService.listLogs(id));
    }

    @Log(title = "催办条生成", action = "generate")
    @ApiOperation(value = "生成催办条", notes = "期限统一规范到当日最后一刻；销号/部门撤并的就地挂起")
    @PostMapping("/generate")
    @ResponseBody
    public AjaxResult generate(TIsHazTask record) {
        TIsHazTask saved = isHazTaskService.generate(record);
        return AjaxResult.success("已生成，情形=" + statusText(saved.getStatus())
                + (saved.getSuspendReason() == null ? "" : "（" + saved.getSuspendReason() + "）"))
                .put("data", saved);
    }

    @Log(title = "催办派发轮次", action = "run")
    @ApiOperation(value = "立刻跑一轮", notes = "窗外静默/本轮无活/系统出错在返回里分清楚")
    @PostMapping("/run")
    @ResponseBody
    public AjaxResult run() {
        RoundResult r = isHazTaskService.runOnce(new Date());
        AjaxResult ajax = AjaxResult.successData(200, r);
        if (r.isWindowClosed()) {
            ajax.put("msg", "窗外静默：" + r.getMessage());
        } else if (!r.isHasWork()) {
            ajax.put("msg", "本轮无活");
        } else {
            ajax.put("msg", "本轮派出 " + r.getRoundDispatched() + " 条，挂起 "
                    + r.getRoundSuspended() + " 条");
        }
        return ajax;
    }

    @Log(title = "催办条撤条", action = "cancel")
    @ApiOperation(value = "撤条", notes = "软删留痕；最初期限与首派时刻不被覆盖")
    @PostMapping("/cancel")
    @ResponseBody
    public AjaxResult cancel(Long id, String reason) {
        return isHazTaskService.cancel(id, reason) ? AjaxResult.success("已撤条并留痕")
                : AjaxResult.error("撤条失败：条不存在、已撤或已挂起");
    }

    @Log(title = "催办条转办", action = "transfer")
    @ApiOperation(value = "转办", notes = "派不出去/逾期后改挂另一责任部门；首派时刻原样保留")
    @PostMapping("/transfer")
    @ResponseBody
    public AjaxResult transfer(Long id, Long toDeptId, String reason) {
        TIsHazTask r = isHazTaskService.transfer(id, toDeptId, reason);
        return r != null ? AjaxResult.success("已转办").put("data", r)
                : AjaxResult.error("转办失败：条不可转办或目标部门已撤并");
    }

    @Log(title = "催办条提前档调整", action = "adjustLeadDays")
    @ApiOperation(value = "调还剩几日露面", notes = "只许调这一档，期限那一刻动不得")
    @PostMapping("/adjustLeadDays")
    @ResponseBody
    public AjaxResult adjustLeadDays(Long id, BigDecimal leadDays) {
        return isHazTaskService.adjustLeadDays(id, leadDays)
                ? AjaxResult.success("已调整露面剩余天数")
                : AjaxResult.error("调整失败：天数非法或该条不可调");
    }

    private static String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待派";
            case 1:
                return "已派";
            case 2:
                return "派不出去";
            case 3:
                return "挂起";
            default:
                return "未知";
        }
    }
}
