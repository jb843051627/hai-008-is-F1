package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TIsHazCard;
import com.fc.v2.service.ITIsHazCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 重大危险源档案卡 Controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "重大危险源档案卡")
@Controller
@RequestMapping("/IsHazCardController")
public class IsHazCardController extends BaseController {

    private final String prefix = "admin/isHazCard";

    @Autowired
    private ITIsHazCardService isHazCardService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("is:isHazCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "重大危险源档案卡集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("is:isHazCard:list")
    @ResponseBody
    public ResultTable list(TIsHazCard record) {
        QueryWrapper<TIsHazCard> queryWrapper = new QueryWrapper<TIsHazCard>();
        startPage();
        com.github.pagehelper.PageInfo<TIsHazCard> page =
                new com.github.pagehelper.PageInfo<TIsHazCard>(isHazCardService.selectTIsHazCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "重大危险源档案卡新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("is:isHazCard:add")
    @ResponseBody
    public AjaxResult add(TIsHazCard record) {
        return toAjax(isHazCardService.insertTIsHazCard(record));
    }

    @Log(title = "重大危险源档案卡修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("is:isHazCard:edit")
    @ResponseBody
    public AjaxResult editSave(TIsHazCard record) {
        return toAjax(isHazCardService.updateTIsHazCard(record));
    }

    @Log(title = "重大危险源档案卡删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("is:isHazCard:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(isHazCardService.deleteTIsHazCardByIds(ids));
    }
}
