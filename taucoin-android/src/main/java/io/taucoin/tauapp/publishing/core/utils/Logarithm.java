package io.taucoin.tauapp.publishing.core.utils;

public class Logarithm {

    /**
     * 求以base为底的对数
     * @param value 数
     * @param base 底
     * @return 对数
     */
    public static double log(double value, double base) {
        return Math.log(value) / Math.log(base);
    }

    /**
     * 求以2为底的对数
     * @param value 数
     * @return 对数
     */
    public static double log2(double value) {
        return log(value, 2);
    }

//    public static long log2(long value) {
//        return (long) log(value, 2);
//    }
}
