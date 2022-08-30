package io.taucoin.tauapp.publishing.core.utils.bus;


import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * 事件总线通信库
 */
public class RxBus2 {
    private static volatile RxBus2 mInstance;
    private final Subject<Object> bus;
    private final ConcurrentHashMap<Class<?>, Object> mStickyEventMap;
    public RxBus2() {
        bus = PublishSubject.create().toSerialized();
        mStickyEventMap = new ConcurrentHashMap<>();
    }

    /**
     * 单例模式RxBus
     * @return
     */
    public static RxBus2 getInstance() {
        RxBus2 rxBus2 = mInstance;
        if (mInstance == null) {
            synchronized (RxBus2.class) {
                rxBus2 = mInstance;
                if (mInstance == null) {
                    rxBus2 = new RxBus2();
                    mInstance = rxBus2;
                }
            }
        }
        return rxBus2;
    }

    /**
     * 发送消息
     * @param object
     */
    public void post(Object object) {
        bus.onNext(object);
    }

    /**
     * 接收消息
     * @param eventType
     * @param <T>
     * @return
     */
    public <T> Observable<T> toObservable(Class<T> eventType) {
        return bus.ofType(eventType);
    }

    /**
     * 返回被观察者
     * @return
     */
    public Observable<Object> toObservable() {
        return bus;
    }

    public boolean hasObservers() {
        return bus.hasObservers();
    }

    public void postSticky(Object event) {
        synchronized (mStickyEventMap) {
            mStickyEventMap.put(event.getClass(), event);
        }
    }

    public  <T> Observable<T> toObservableSticky(Class<T> eventType) {
        synchronized (mStickyEventMap) {
            Observable<T> observable = bus.ofType(eventType);
            Object event = mStickyEventMap.get(eventType);
            if (event != null) {
                return observable.mergeWith(Observable.create(emitter -> emitter.onNext(eventType.cast(event))));
            } else {
                return observable;
            }
        }
    }

    public  <T> T getStickyOnce(Class<T> eventType) {
        synchronized (mStickyEventMap) {
            Object event = mStickyEventMap.get(eventType);
            if (event != null) {
                eventType.cast(event);
                removeStickyEvent(eventType);
            }
            return null;
        }
    }

    public <T> Disposable registerStickyEvent(Class<T> eventType, Consumer<T> consumer) {
        return toObservableSticky(eventType)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer);

    }


    public <T> T removeStickyEvent(Class<T> eventType) {
        synchronized (mStickyEventMap) {
            return eventType.cast(mStickyEventMap.remove(eventType));
        }
    }

    public void removeAllStickyEvents() {
        synchronized (mStickyEventMap) {
            mStickyEventMap.clear();
        }
    }
}