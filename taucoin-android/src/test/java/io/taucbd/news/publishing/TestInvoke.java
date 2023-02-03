package io.taucbd.news.publishing;

/**
 * @hide
 */
public class TestInvoke {
    private Dog dog;
    private static TestInvoke sInstance;
    private static final Object mLock = new Object();

    public TestInvoke(String name, String type) {
        this.dog = new Dog(name, type);
    }

    public static TestInvoke getInstance(String name, String type) {
        TestInvoke test;
        synchronized (mLock) {
            if (sInstance == null) {
                sInstance = new TestInvoke(name, type);
            }
            test = sInstance;
        }
        return test;
    }
}
