package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TIsDispatchLog;
import com.fc.v2.model.auto.TIsHazTask;
import com.fc.v2.service.ITIsHazTaskService;
import com.fc.v2.service.haz.RoundScreen;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 隐患整改催办条 Controller（scheduling-job 形状：明细台账 + 立即跑一轮 + 撤条/转办/调档）。
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

    @ApiOperation(value = "催办明细台账跳转", notes = "挂起/已撤/转办各情形都翻得到")
    @GetMapping("/view")
    @RequiresPermissions("isHazTask:view")
    public String view(ModelMap model) {
        model.put("latest", isHazTaskService.latestRound());
        return prefix + "/list";
    }

    @Log(title = "隐患整改催办条明细", action = "list")
    @ApiOperation(value = "明细分页查询", notes = "挂起条仍在明细页翻得到；status 不传取全部")
    @GetMapping("/list")
    @RequiresPermissions("isHazTask:list")
    @ResponseBody
    public ResultTable list(@RequestParam(required = false) Integer status) {
        Date now = new Date();
        startPage();
        List<TIsHazTask> list = isHazTaskService.listBills(status, now);
        com.github.pagehelper.PageInfo<TIsHazTask> page =
                new com.github.pagehelper.PageInfo<>(list);
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "隐患整改催办条明细", action = "detail")
    @ApiOperation(value = "单条明细+全过程留痕", notes = "谁接手、改到哪、催过几轮，一翻便知")
    @GetMapping("/detail")
    @RequiresPermissions("isHazTask:list")
    @ResponseBody
    public AjaxResult detail(Long id) {
        TIsHazTask bill = isHazTaskService.selectTIsHazTaskById(id, new Date());
        if (bill == null) {
            return AjaxResult.error("催办条不存在");
        }
        List<TIsDispatchLog> logs = isHazTaskService.listLogs(id);
        Map<String, Object> data = new HashMap<>();
        data.put("bill", bill);
        data.put("logs", logs);
        return AjaxResult.successData(200, data);
    }

    @Log(title = "隐患整改催办条", action = "generate")
    @ApiOperation(value = "按限期整改要求生成催办条", notes = "已销号/部门撤并的就地挂起并写明原由")
    @PostMapping("/generate")
    @RequiresPermissions("isHazTask:run")
    @ResponseBody
    public AjaxResult generate() {
        List<TIsHazTask> created = isHazTaskService.generateDueBills(new Date());
        return AjaxResult.success("已生成（含挂起）" + created.size() + " 条")
                .put("data", created);
    }

    @Log(title = "隐患整改催办轮次", action = "run")
    @ApiOperation(value = "立即跑一轮派发", notes = "窗口说了算；屏上三个数与库同一次点算")
    @PostMapping("/run")
    @RequiresPermissions("isHazTask:run")
    @ResponseBody
    public AjaxResult run() {
        RoundScreen screen = isHazTaskService.runOnce(new Date());
        AjaxResult r;
        switch (screen.getState()) {
            case ERROR:
                r = AjaxResult.error(500, screen.getMessage());
                break;
            case CLOSED:
            case SKIPPED:
                r = AjaxResult.success(202, screen.getMessage(), screen);
                break;
            case NO_WORK:
                // 本轮无活是正常结果，单独文案，绝不与系统出错混用。
                r = AjaxResult.success(200, screen.getMessage(), screen);
                break;
            default:
                r = AjaxResult.success(screen.getMessage()).put("data", screen);
        }
        return r;
    }

    @ApiOperation(value = "最近一轮点算", notes = "明细页亮一屏")
    @GetMapping("/latestRound")
    @RequiresPermissions("isHazTask:list")
    @ResponseBody
    public AjaxResult latestRound() {
        return AjaxResult.successData(200, isHazTaskService.latestRound());
    }

    @Log(title = "隐患整改催办条撤条", action = "revoke")
    @ApiOperation(value = "撤条留痕", notes = "期限过了仍派不出去；最初整改日与初派时刻原样保留")
    @PostMapping("/revoke")
    @RequiresPermissions("isHazTask:handle")
    @ResponseBody
    public AjaxResult revoke(Long id, String reason) {
        try {
            isHazTaskService.revoke(id, reason);
            return AjaxResult.success("撤条已留痕");
        } catch (RuntimeException ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }

    @Log(title = "隐患整改催办条转办", action = "transfer")
    @ApiOperation(value = "转办", notes = "期限过了仍派不出去的另一条出路，原子接单")
    @PostMapping("/transfer")
    @RequiresPermissions("isHazTask:handle")
    @ResponseBody
    public AjaxResult transfer(Long id, Long toDeptId, String reason) {
        try {
            isHazTaskService.transfer(id, toDeptId, reason);
            return AjaxResult.success("转办已派出");
        } catch (RuntimeException ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }

    @Log(title = "隐患整改催办条调档", action = "tune")
    @ApiOperation(value = "只调还剩几日露面档", notes = "期限那一刻动不得")
    @PostMapping("/tune")
    @RequiresPermissions("isHazTask:handle")
    @ResponseBody
    public AjaxResult tune(Long id, BigDecimal leadDays) {
        try {
            isHazTaskService.tuneLeadDays(id, leadDays);
            return AjaxResult.success("露面档已调，期限未动");
        } catch (RuntimeException ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }
}
