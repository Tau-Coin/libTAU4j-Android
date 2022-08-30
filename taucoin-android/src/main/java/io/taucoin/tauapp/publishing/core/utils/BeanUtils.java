package io.taucoin.tauapp.publishing.core.utils;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * javabean工具类
 */
public class BeanUtils {
    private static final Logger logger = LoggerFactory.getLogger("BeanUtils");

    /**
     * 把Map转化为javabean
     * @param map Map数据
     * @param classOfT 数据结构类
     * @return javabean
     */
    public static <T> T map2bean(Map map, Class<T> classOfT) {
        Gson gson = new Gson();
        String json = gson.toJson(map);
        return gson.fromJson(json, classOfT);
    }

    /**
     * 把Object（javabean）转化为Map
     * 只处理有getXXX()方法的域
     * 这里为什么不使用Gson转化的原因：int会转化为Double等问题，所以采用直接反射处理
     * @param obj javabean
     * @return Map<String, ?>
     */
    public static Map<String, ?> bean2map(Object obj) {
        Map<String, Object> keyValues = new HashMap<>();
        Method[] methods = obj.getClass().getDeclaredMethods();
        Field[] fields = obj.getClass().getDeclaredFields();
        try {
            for (Field field : fields) {
                String fieldName = field.getName();
                for (Method method : methods) {
                    String methodName = method.getName();
                    if (StringUtil.isEquals(methodName.toLowerCase(),
                            ("get" + fieldName).toLowerCase())) {
                        Object value = method.invoke(obj);
                        keyValues.put(fieldName, value);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("bean2map error: ", e);
        }
        return keyValues;
    }
}
