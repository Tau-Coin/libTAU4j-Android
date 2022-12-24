package io.taucoin.news.publishing.core.utils;

public class GeoUtils {
    /**
     * 计算两经纬度点之间的距离（单位：米）
     * @param lng1 点1经度
     * @param lat1 点1纬度
     * @param lng2 点2经度
     * @param lat2 点2纬度
     * @return 距离
     */
    public static double getDistance(double lng1, double lat1, double lng2, double lat2){
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1) - Math.toRadians(lng2);

        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));

        s = s * 6378137.0;// 取WGS84标准参考椭球中的地球长半径(单位:m)
        return s;
    }

    public static String getDistanceStr(double lng1, double lat1,double lng2, double lat2) {
        double distance = getDistance(lng1, lat1, lng2, lat2);
        if (distance < 1000) {
            return "<1km";
        } else if (distance < 10000) {
            return "<10km";
        } else if  (distance < 50000) {
            return "<50km";
        } else if  (distance < 100000)  {
            return "<100km";
        } else {
            return ">100km";
        }
    }
}


