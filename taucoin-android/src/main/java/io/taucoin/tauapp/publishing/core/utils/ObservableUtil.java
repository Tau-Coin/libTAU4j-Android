package io.taucoin.tauapp.publishing.core.utils;

import io.reactivex.Observable;

public class ObservableUtil {

    public static Observable<Long> interval(long millis) {
        return Observable.create(emitter -> {
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
        return interval(seconds * 1000);
    }
}
