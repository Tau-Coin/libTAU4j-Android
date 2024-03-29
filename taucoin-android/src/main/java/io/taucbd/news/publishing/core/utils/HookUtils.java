package io.taucbd.news.publishing.core.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 处理android 9及以上版本，@hide反射限制的问题
 */
class HookUtils {

    /**
     * 两次反射，代理Class.getDeclaredMethod方法
     */
    public static Method getDeclaredMethod(Class<?> cls, String name, Class<?>... parameterTypes) {
        try {
            Method dMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);

            return (Method) dMethod.invoke(cls, name, parameterTypes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 两次反射，代理Class.getDeclaredField方法
     */
    public static Field getDeclaredField(Class<?> cls, String name) {
        try {
            Method dMethod = Class.class.getDeclaredMethod("getDeclaredField", String.class);

            return (Field) dMethod.invoke(cls, name);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 两次反射，代理Field.get方法
     */
    public static Object fieldGetValue(Field field, Object obj) {
        try {
            Method dMethod = HookUtils.getDeclaredMethod(Field.class,"get", Object.class);
            field.setAccessible(true);
            return obj == null ? dMethod.invoke(field, obj) : dMethod.invoke(field, obj);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 两次反射，代理Field.set方法
     */
    public static void fieldSetValue(Field field, Object obj, Object value) {
        try {
            Method dMethod = HookUtils.getDeclaredMethod(Field.class,"set", Object.class, Object.class);
            field.setAccessible(true);
            dMethod.invoke(field, obj, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}