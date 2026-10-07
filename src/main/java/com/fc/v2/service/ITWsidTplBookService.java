package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TWsidTplBook;

import java.util.List;

/**
 * 专用标识证面版式模板册 Service接口
 *
 * <p>建版取式只认这一处服务层：行能不能落库、空槽上溯到哪一版、完成比轧几位小数、
 * 三个检索口子各认哪一路写法、撤下的那一条还动不动得了，都不许在控制层与页面上
 * 另辟径路绕开。既有方法的入参与返回一概照旧；换一种取数思路，另立一条方法去担。</p>
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITWsidTplBookService {

    /** 按主键查询 */
    TWsidTplBook selectTWsidTplBookById(Long id);

    /** 按条件查询列表（分页由调用方统一处理；撤下的行仍旧列得出来） */
    List<TWsidTplBook> selectTWsidTplBookList(Wrapper<TWsidTplBook> queryWrapper);

    /** 新增（代号由系统排上；三样撞车挡回；槽位文面空着进不了库） */
    int insertTWsidTplBook(TWsidTplBook record);

    /** 修改（撤下的行文面改不了；完成比落库前重轧一遍） */
    int updateTWsidTplBook(TWsidTplBook record);

    /** 批量删除（与单条同一处说法；撤下的行抽不走） */
    int deleteTWsidTplBookByIds(String ids);

    /** 推进一态（在位 → 已撤下；情形只两态） */
    int advanceTWsidTplBook(Long id);

    /** 退回一态（撤下是单程，无回路） */
    int revertTWsidTplBook(Long id);

    /** 按主键删除 */
    int deleteTWsidTplBookById(Long id);

    /**
     * 模糊检索那一路：物种代号、层别名目只认起头那几个字（前匹配），槽位文面认全文
     * 关键词（包含）。{@code levelScope} 非空则只出该层（本层值守）；{@code specScope}
     * 非空则限于划归物种，空集合即一段没划归——空着回话，不从邻层牵行来填。
     */
    List<TWsidTplBook> searchTWsidTplBook(TWsidTplBook query, String levelScope, List<Integer> specScope);

    /** 全等检索那一路：三个口子都给全等，与模糊那一路各开一路 */
    List<TWsidTplBook> matchTWsidTplBook(TWsidTplBook query, String levelScope, List<Integer> specScope);

    /**
     * 取式：顺继承链 县定→市定→省定→系统兜底 取说话的那一版；同名目并立几行先挑
     * 最后落笔的那条，落笔分不出先后再比代号串字面取靠后那条；落到系统兜底那一版时
     * 只出正联。寻不见行不答空白，一层层退上去；链上全空才回 null。
     */
    TWsidTplBook resolveTWsidTplBook(Integer specId, String tplName);
}
