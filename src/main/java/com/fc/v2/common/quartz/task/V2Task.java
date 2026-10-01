package com.fc.v2.common.quartz.task;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.service.ITIsHazTaskService;
import com.fc.v2.service.ITSysUserService;
import com.fc.v2.service.dispatch.RoundResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *测试类
 * @CLASSNAME   V2Task
 * @Description 定时调度具体工作类
 * @Auther Jan  橙寂
 * @DATE 2019/9/2 0002 15:33
 */
@Component("v2Task")
public class V2Task {

	@Autowired
    private ITSysUserService userService;

    @Autowired
    private ITIsHazTaskService isHazTaskService;

    /**
     * 隐患整改催办条派发轮询：由 Quartz 按点调度。
     * 窗外静默 / 本轮无活 / 派发结果都在 RoundResult 里区分，日志照实打。
     */
    public void hazTaskDispatch() {
        RoundResult r = isHazTaskService.runOnce(new java.util.Date());
        if (r.isWindowClosed()) {
            System.out.println("催办派发：" + r.getMessage());
            return;
        }
        System.out.println("催办派发窗口=" + r.getWindow() + " 轮次=" + r.getRoundNo()
                + (r.isHasWork() ? "" : "（本轮无活）")
                + " 本轮派出=" + r.getRoundDispatched()
                + " 催办=" + r.getRoundUrged()
                + " 落派不出去=" + r.getRoundStuck()
                + " | 库存待派=" + r.getWaitingCount()
                + " 已派=" + r.getDispatchedCount()
                + " 挂起=" + r.getSuspendedCount()
                + " " + DateUtil.now());
    }
    /**
     * 无参的任务
     */
    public void runTask1()
    {
        System.out.println("正在执行定时任务，无参方法");
    }

    /**
     * 有参任务
     * 目前仅执行常见的数据类型  Integer Long  带L  string  带 ''  bool Double 带 d
     * @param a
     * @param b
     */
    public void runTask2(Integer a,Long b,String c,Boolean d,Double e)
    {
    	List<TSysUser> list=  userService.selectTSysUserList(new QueryWrapper<TSysUser>());
    	System.err.println("用户查询num："+list.size());
        System.out.println("正在执行定时任务，带多个参数的方法"+a+"   "+b+" "+c+"  "+d+" "+e+"执行时间:"+DateUtil.now());
    }
}
