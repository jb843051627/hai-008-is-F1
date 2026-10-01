package com.fc.v2.service.dispatch;

/**
 * 隐患销号判定端口：条子生成前，隐患是否已销号结案。
 *
 * <p>生产由隐患台账侧实现；无接入时默认一律未销号（{@link #NONE}）。
 *
 * @author fuce
 * @date 2026-09-30
 */
public interface HazardClosurePort {

    /** 该隐患（条号/源事项号）是否已销号结案。 */
    boolean isClosed(String itemNo);

    /** 默认端口：什么都没销号。 */
    HazardClosurePort NONE = new HazardClosurePort() {
        @Override
        public boolean isClosed(String itemNo) {
            return false;
        }
    };
}
