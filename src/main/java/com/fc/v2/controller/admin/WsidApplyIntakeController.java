package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TWsidApplyReceipt;
import com.fc.v2.model.auto.TWsidApplyRow;
import com.fc.v2.model.custom.wsid.WsidIntakeCmd;
import com.fc.v2.model.custom.wsid.WsidResumeCmd;
import com.fc.v2.service.IWsidApplyIntakeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 标识申领名册核收 Controller（batch-process 形状：一整串进门）。
 *
 * <p>这轮界面不改，这里只出接口。三件事都在这里：每行去处怎么定（intake）、
 * 第二遍到怎么对付（同口再调，只对照不改账）、回撤谁跟着撤（rollback）。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
@Api(value = "标识申领名册核收")
@Controller
@RequestMapping("/wsidApplyIntake")
public class WsidApplyIntakeController extends BaseController {

    @Autowired
    private IWsidApplyIntakeService wsidApplyIntakeService;

    @Log(title = "标识申领名册整串进门", action = "intake")
    @ApiOperation(value = "一整串进门（四步连办）；同一串同一来路再调即第二遍，只对照不改账", notes = "一整串进门")
    @PostMapping("/intake")
    @RequiresPermissions("wsid:wsidApply:intake")
    @ResponseBody
    public AjaxResult intake(@RequestBody WsidIntakeCmd cmd) {
        return AjaxResult.success(200, "整串已收讫", wsidApplyIntakeService.intake(cmd));
    }

    @Log(title = "标识申领名册续办补报", action = "resume")
    @ApiOperation(value = "续办：只补未勾／退回再报的行，行次认当初排定的", notes = "续办补报")
    @PostMapping("/resume")
    @RequiresPermissions("wsid:wsidApply:intake")
    @ResponseBody
    public AjaxResult resume(@RequestBody WsidResumeCmd cmd) {
        return AjaxResult.success(200, "续办已落账", wsidApplyIntakeService.resume(cmd));
    }

    @Log(title = "标识申领名册回撤", action = "rollback")
    @ApiOperation(value = "回撤：只有未勾行摆得回，已落定的一行不跟回；行面单面一起动", notes = "回撤")
    @PostMapping("/rollback/{batchNo}")
    @RequiresPermissions("wsid:wsidApply:intake")
    @ResponseBody
    public AjaxResult rollback(@PathVariable("batchNo") String batchNo,
                               @RequestParam("putKind") String putKind,
                               @RequestBody List<Integer> rowNos) {
        return AjaxResult.success(200, "回撤已成",
                wsidApplyIntakeService.rollback(batchNo, putKind, rowNos));
    }

    @ApiOperation(value = "取本串本面收讫单面", notes = "取收讫单面")
    @GetMapping("/receipt/{batchNo}")
    @RequiresPermissions("wsid:wsidApply:intake")
    @ResponseBody
    public AjaxResult receipt(@PathVariable("batchNo") String batchNo,
                              @RequestParam("putKind") String putKind) {
        TWsidApplyReceipt receipt = wsidApplyIntakeService.getReceipt(batchNo, putKind);
        return receipt == null ? AjaxResult.error("本面还没进门，单面查不着")
                : AjaxResult.success(200, "单面在此", receipt);
    }

    @ApiOperation(value = "取本串逐行（按行次升序，存量行一条不减）", notes = "取逐行")
    @GetMapping("/rows/{batchNo}")
    @RequiresPermissions("wsid:wsidApply:intake")
    @ResponseBody
    public AjaxResult rows(@PathVariable("batchNo") String batchNo,
                           @RequestParam(value = "putKind", required = false) String putKind) {
        List<TWsidApplyRow> rows = wsidApplyIntakeService.listRows(batchNo, putKind);
        return AjaxResult.success(200, "逐行在此", rows);
    }

    @ApiOperation(value = "倒着核：行上去处跟单面三笔逐项对账，不符按行点名", notes = "倒查")
    @GetMapping("/audit/{batchNo}")
    @RequiresPermissions("wsid:wsidApply:intake")
    @ResponseBody
    public AjaxResult audit(@PathVariable("batchNo") String batchNo) {
        return AjaxResult.success(200, "倒查已成", wsidApplyIntakeService.audit(batchNo));
    }
}
