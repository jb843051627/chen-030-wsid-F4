package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.custom.wsid.WsidScanResult;
import com.fc.v2.service.ITWsidRemindItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 年度核查与换证提醒 Controller（scheduling-job 形状：按日照跑）。
 *
 * <p>六问只许服务层那一条扫描方法开口：这里不先亮数、不留第二道门，三笔数与逐行记号
 * 一律取当轮 {@code scan} 返回的那一份。版面这一轮照旧，不出新页面。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
@Api(value = "年度核查与换证提醒")
@Controller
@RequestMapping("/wsidRemind")
public class WsidRemindController extends BaseController {

    @Autowired
    private ITWsidRemindItemService wsidRemindItemService;

    @Log(title = "年度核查与换证提醒按日照跑", action = "scan")
    @ApiOperation(value = "一轮日照扫描（挑行/回落/静默/缺栏/去重/三笔，唯一一道门）", notes = "一轮扫描")
    @PostMapping("/scan")
    @RequiresPermissions("wsid:wsidRemind:scan")
    @ResponseBody
    public AjaxResult scan(@RequestParam(value = "at", required = false) String at) {
        Date moment = null;
        if (at != null && !at.trim().isEmpty()) {
            try {
                moment = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(at.trim());
            } catch (java.text.ParseException e) {
                return AjaxResult.error("开跑那一刻须写成 yyyy-MM-dd HH:mm:ss");
            }
        }
        WsidScanResult result = wsidRemindItemService.scan(moment);
        return AjaxResult.success(200, result.isEmpty() ? "本轮空跑" : "本轮扫描已成", result);
    }

    @ApiOperation(value = "按主键回查一条", notes = "回查条目")
    @GetMapping("/item/{id}")
    @RequiresPermissions("wsid:wsidRemind:scan")
    @ResponseBody
    public AjaxResult item(@PathVariable("id") Long id) {
        return AjaxResult.success(200, "条目在此", wsidRemindItemService.selectTWsidRemindItemById(id));
    }
}
