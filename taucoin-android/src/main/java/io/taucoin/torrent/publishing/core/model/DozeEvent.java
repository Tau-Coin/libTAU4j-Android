package io.taucoin.torrent.publishing.core.model;

public enum  DozeEvent {
    FORE_BACK,                  // 前台和后台切换
    NODES_CHANGED,              // nodes数变化
    DOZE_TIME_CHANGED,          // doze时间变化
    SYS_DOZE_END,               // 系统doze模式结束
    TOUCH_EVENT,                // 界面touch事件
    KEY_DOWN,                   // 界面物理键事件
    TEXT_INPUT;                 // 文本输入
}
