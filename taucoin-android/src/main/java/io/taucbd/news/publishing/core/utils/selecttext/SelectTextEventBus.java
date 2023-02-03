package io.taucbd.news.publishing.core.utils.selecttext;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 选择文本的事件总线
 */
public class SelectTextEventBus {
    public static final String DISMISS_ALL_POP = "dismissAllPop";
    public static final String DISMISS_ALL_POP_DELAYED = "dismissAllPopDelayed";
    public static final String DISMISS_OPERATE_POP = "dismissOperatePop";
    private static volatile SelectTextEventBus defaultInstance;
    private final Map<Object, List<Class<?>>> typesBySubscriber;

    public SelectTextEventBus() {
        typesBySubscriber = new HashMap<>();
    }

    public static SelectTextEventBus getDefault() {
        if (defaultInstance == null) {
            synchronized (SelectTextEventBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new SelectTextEventBus();
                }
            }
        }
        return defaultInstance;
    }

    public void register(Object subscriber) {
        register(subscriber, SelectTextEvent.class);
    }

    public void register(Object subscriber, Class eventClass) {
        EventBus.getDefault().register(subscriber);
        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if (subscribedEvents == null) {
            subscribedEvents = new ArrayList<>();
            typesBySubscriber.put(subscriber, subscribedEvents);
        }
        subscribedEvents.add(eventClass);
    }

    public synchronized boolean isRegistered(Object subscriber) {
        if (EventBus.getDefault().isRegistered(subscriber)) {
            return true;
        }
        return typesBySubscriber.containsKey(subscriber);
    }

    /**
     * 这里主要实现了注销功能
     */
    public synchronized void unregister() {
        for (Object key : typesBySubscriber.keySet()) {
            EventBus.getDefault().unregister(key);
        }

        typesBySubscriber.clear();
    }

    /**
     * 注销
     */
    public synchronized void unregister(Object subscriber) {
        if (typesBySubscriber.containsKey(subscriber)) {
            EventBus.getDefault().unregister(subscriber);
            typesBySubscriber.remove(subscriber);
        }
    }

    /**
     * 分发事件
     *
     * @param event
     */
    public void dispatch(Object event) {
        EventBus.getDefault().post(event);
    }

    /**
     * 分发关闭所有Pop窗口事件
     */
    public void dispatchDismissAllPop() {
        dispatch(new SelectTextEvent(DISMISS_ALL_POP));
    }

    /**
     * 分发关闭所有Pop窗口事件
     */
    public void dispatchDismissAllPopDelayed() {
        dispatch(new SelectTextEvent(DISMISS_ALL_POP_DELAYED));
    }

    /**
     * 分发关闭操作Pop窗口事件
     */
    public void dispatchDismissOperatePop() {
        dispatch(new SelectTextEvent(DISMISS_OPERATE_POP));
    }

}