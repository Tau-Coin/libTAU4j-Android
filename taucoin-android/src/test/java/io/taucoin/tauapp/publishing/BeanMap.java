package io.taucoin.tauapp.publishing;

import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.taucoin.tauapp.publishing.core.utils.BeanUtils;

public class BeanMap {

    @Test
    public void map2bean() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "张三");
        map.put("age", 22);
        map.put("address", "pu dong".getBytes());
        map.put("bigInt", BigInteger.valueOf(123));
        map.put("longNum", 123L);

        map2print(map);

        Bean bean = BeanUtils.map2bean(map, Bean.class);
        System.out.println("name::" + bean.getName());
        System.out.println("age::" + bean.getAge());
        System.out.println("address::" + new String(bean.getAddress()));
    }

    @Test
    public void bean2map() {
        Bean bean = new Bean();
        bean.setName("张三");
        bean.setAge(22);
        bean.setAddress("pu dong".getBytes());
        bean.setBigInt(BigInteger.valueOf(123));
        bean.setLongNum(123L);

        Map<String, ?> params = BeanUtils.bean2map(bean);
        //实际调用
        map2print(params);
    }

    private void map2print(Map<String, ?> map) {
        Iterator var3 = map.keySet().iterator();
        while(var3.hasNext()) {
            String k = (String)var3.next();
            Object v = map.get(k);
            if (v instanceof String) {
                System.out.println("String key=" + k);
            } else if (v instanceof Integer) {
                System.out.println("Integer key=" + k);
            } else if (v instanceof List) {
                System.out.println("List key=" + k);
            } else if (v instanceof Map) {
                System.out.println("Map key=" + k);
            } else if (v instanceof byte[]) {
                System.out.println("byte[] key=" + k);
            } else {
                System.out.println(TypeToken.get(v.getClass()).getType() + ", Other key=" + k);
            }
        }
    }

    static class Bean {
        private String name;
        private Integer age;
        private BigInteger bigInt;
        private long longNum;
        private byte[] address;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public byte[] getAddress() {
            return address;
        }

        public void setAddress(byte[] address) {
            this.address = address;
        }

        public BigInteger getBigInt() {
            return bigInt;
        }

        public void setBigInt(BigInteger bigInt) {
            this.bigInt = bigInt;
        }

        public long getLongNum() {
            return longNum;
        }

        public void setLongNum(long longNum) {
            this.longNum = longNum;
        }
    }
}
