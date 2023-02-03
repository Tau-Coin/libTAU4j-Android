package io.taucbd.news.publishing.core.utils;

import io.reactivex.Observable;

public class ObservableUtil {

    public static Observable<Long> interval(long millis) {
        return interval(millis, false);
    }
    public static Observable<Long> interval(long millis, boolean initSend) {
        return Observable.create(emitter -> {
            if (initSend) {
                emitter.onNext(System.currentTimeMillis());
            }
            while (true) {
                try {
                    if (!emitter.isDisposed()) {
                        Thread.sleep(millis);
                        emitter.onNext(System.currentTimeMillis());
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }
            emitter.onComplete();
        });
    }

    public static Observable<Long> intervalSeconds(long seconds) {
        return intervalSeconds(seconds, false);
    }

    public static Observable<Long> intervalSeconds(long seconds, boolean initSend) {
        return interval(seconds * 1000, initSend);
    }
}
