package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TWsidTplBook;
import com.fc.v2.service.ITWsidTplBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 专用标识证面版式模板册 Controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "专用标识证面版式模板册")
@Controller
@RequestMapping("/WsidTplBookController")
public class WsidTplBookController extends BaseController {

    private final String prefix = "admin/wsidTplBook";

    @Autowired
    private ITWsidTplBookService wsidTplBookService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("wsid:wsidTplBook:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "专用标识证面版式模板册集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("wsid:wsidTplBook:list")
    @ResponseBody
    public ResultTable list(TWsidTplBook record) {
        // 检索口子与读法只认服务层那一条方法，控制层不另摆条件；一屏几条由请求配置带来
        startPage();
        com.github.pagehelper.PageInfo<TWsidTplBook> page =
                new com.github.pagehelper.PageInfo<TWsidTplBook>(
                        wsidTplBookService.searchTWsidTplBook(record, null, null));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "专用标识证面版式模板册新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("wsid:wsidTplBook:add")
    @ResponseBody
    public AjaxResult add(TWsidTplBook record) {
        return toAjax(wsidTplBookService.insertTWsidTplBook(record));
    }

    @Log(title = "专用标识证面版式模板册修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("wsid:wsidTplBook:edit")
    @ResponseBody
    public AjaxResult editSave(TWsidTplBook record) {
        return toAjax(wsidTplBookService.updateTWsidTplBook(record));
    }

    @Log(title = "专用标识证面版式模板册删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("wsid:wsidTplBook:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(wsidTplBookService.deleteTWsidTplBookByIds(ids));
    }
}
