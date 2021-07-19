package io.taucoin.torrent.publishing;


import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class InvokeUnitTest {

    @Test
    public void TestMsgSplit() {
        System.out.println("*************************************");
        TestInvoke test = TestInvoke.getInstance("小狗", "土狗");
        showContent(test);
        System.out.println("*************************************");
        System.out.println("\n\n");
        Class<?> threadClazz = null;
        try {
            threadClazz = Class.forName("io.taucoin.torrent.publishing.TestInvoke");
            Method method = threadClazz.getMethod("getInstance", String.class, String.class);
            Object test1 = method.invoke(null, null, null);
            showContent(test1);
        } catch (Exception e) {
            System.out.println("error=" + e.getLocalizedMessage());
        }
        System.out.println("*************************************");
    }

    private void showContent(Object test) {
        try {
            Field dog = test.getClass().getDeclaredField("dog");

            dog.setAccessible(true);
            System.out.println("dog=" + dog.get(test));

            Object object = dog.get(test);
            Field name = object.getClass().getDeclaredField("name");
            name.setAccessible(true);
            System.out.println("name=" + name.get(object));

            Field type = object.getClass().getDeclaredField("type");
            type.setAccessible(true);
            System.out.println("type=" + type.get(object));

            Field maxAge = object.getClass().getDeclaredField("maxAge");
            maxAge.setAccessible(true);
            System.out.println("maxAge=" + maxAge.get(object));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}