package io.taucoin.news.publishing.core.utils.selecttext;

import java.io.Serializable;

/**
 * 选择文本的event
 */
public class SelectTextEvent implements Serializable {
    // 关闭所有弹窗 dismissAllPop
    // 延迟关闭所有弹窗 dismissAllPopDelayed
    // 关闭操作弹窗 dismissOperatePop
    private String type;

    public SelectTextEvent(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}