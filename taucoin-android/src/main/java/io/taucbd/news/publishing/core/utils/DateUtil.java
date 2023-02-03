/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucbd.news.publishing.core.utils;

import android.content.Context;
import android.widget.Chronometer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;

/**
 * Description: Date tools
 * Author:yang
 * Date: 2017/10/15
 */
public class DateUtil {

    /**
     * yyyy-MM-dd HH:mm:ss
     */
    public static final String pattern0 = "HH:mm";
    public static final String pattern1 = "mm:ss";
    public static final String pattern2 = "MM-dd";
    public static final String pattern3 = "yyyy-MM";
    public static final String pattern4 = "yyyy-MM-dd";
    public static final String pattern5 = "yyyy-MM-dd HH:mm";
    public static final String pattern6 = "yyyy-MM-dd HH:mm:ss";
    public static final String pattern7 = "yyyy-MM-dd\'T\'HH:mm:ss";
    public static final String pattern8 = "yyyy-MM-dd\'T\'HH:mm:ss.SS SZ";
    public static final String pattern9 = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String pattern10 = "HH:mm:ss\nyyyy-MM-dd";
    public static final String pattern11 = "yyyy-MM-dd \'at\' HH:mm";
    public static final String pattern12 = "yyyyMMdd";
    public static final String pattern13 = "MM-dd HH:mm";
    public static final String pattern14 = "MMM-dd";

    private static final String[] weeks = {"Sun", "Mon","Tue","Wed","Thu","Fri","Sat"};
    private static final ThreadLocal<SimpleDateFormat> tlFormat = new ThreadLocal<>();

    public static SimpleDateFormat getSimpleDateFormat() {
        if (null == tlFormat.get()) {
            tlFormat.set(new SimpleDateFormat(pattern6, Locale.ENGLISH));
        }
        return tlFormat.get();
    }

    public static String format(String time, String parsePattern, String pattern) {
        try {
            SimpleDateFormat format = getSimpleDateFormat();
            format.applyPattern(parsePattern);
            Date parse = format.parse(time);
            TimeZone timeZone = TimeZone.getDefault();
            format.setTimeZone(timeZone);
            format.applyPattern(pattern);
            return format.format(parse);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String format(long time, String pattern) {
        SimpleDateFormat format = getSimpleDateFormat();
        format.applyPattern(pattern);
        TimeZone timeZone = TimeZone.getDefault();
        format.setTimeZone(timeZone);
        return format.format(new Date(time));
    }

    public static String getCurrentTime() {
        Date date = new Date();
        Long time = date.getTime();
        time = time / 1000;
        return String.valueOf(time);
    }

    public static String getDateTime() {
        Date date = new Date();
        Long time = date.getTime();
        return String.valueOf(time);
    }

    public static Long getMillisTime() {
        Date date = new Date();
        return date.getTime();
    }

    public static long getTime() {
        Date date = new Date();
        Long time = date.getTime();
        time = time / 1000;
        return time;
    }

    public static String formatTime(String time, String pattern) {
        try {
            long timeSeconds = Long.valueOf(time);
            return formatTime(timeSeconds, pattern);
        }catch (Exception ignore){}
        return time;
    }

    public static String formatTime(long timeSeconds, String pattern) {
        timeSeconds = timeSeconds * 1000;
        Date date = new Date(timeSeconds);
        TimeZone timeZone = TimeZone.getDefault();
        SimpleDateFormat format = getSimpleDateFormat();
        format.setTimeZone(timeZone);

        format.applyPattern(pattern);
        return format.format(date);
    }

    public static String formatBestTime(long timeSeconds) {
        long minutes = timeSeconds / (1000 * 60);
        long seconds = (timeSeconds - minutes*(1000 * 60))/1000;
        String diffTime = minutes < 10 ? "0" + minutes+":" : minutes+":";
        diffTime = seconds < 10 ? diffTime +"0" + seconds : diffTime + seconds ;

        long millisecond = (int) (timeSeconds%1000/10);
        String count = millisecond > 9 ? "." + millisecond : ".0" + millisecond;
        diffTime += count;
        return diffTime;
    }

    /**
     *
     * @param cmt  Chronometer
     * @return hour+min+send
     */
    public  static int getChronometerSeconds(Chronometer cmt) {
        int total = 0;
        String string = cmt.getText().toString();
        if(string.length()==7){

            String[] split = string.split(":");
            String string2 = split[0];
            int hour = Integer.parseInt(string2);
            int Hours =hour*3600;
            String string3 = split[1];
            int min = Integer.parseInt(string3);
            int minCount =min*60;
            int  SS = Integer.parseInt(split[2]);
            total = Hours+minCount+SS;
        } else if(string.length()==5){

            String[] split = string.split(":");
            String string3 = split[0];
            int min = Integer.parseInt(string3);
            int minCount =min*60;
            int  SS = Integer.parseInt(split[1]);
            total =minCount+SS;
        }
        return total;


    }

    @SuppressWarnings("SameParameterValue")
    private static long getLong(String time, String pattern) {
        try {
            SimpleDateFormat format = getSimpleDateFormat();
            format.applyPattern(pattern);
            TimeZone timeZone = TimeZone.getDefault();
            format.setTimeZone(timeZone);
            return format.parse(time).getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean compare(String formerTime, String latterTime, String pattern) {
       if(StringUtil.isNotEmpty(formerTime) && StringUtil.isNotEmpty(latterTime)){
           return getLong(formerTime, pattern) > getLong(latterTime, pattern);
       }
       return false;
    }

    public static int compareDay(long formerTime, long latterTime) {
        int day = 0;
        if(latterTime > formerTime){
            try {
                Date date1 = new Date(formerTime);
                Date date2 = new Date(latterTime);
                day = differentDays(date1, date2);
            }catch (Exception ignore){
            }
        }
        return day;
    }

    private static int differentDays(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        int day1= cal1.get(Calendar.DAY_OF_YEAR);
        int day2 = cal2.get(Calendar.DAY_OF_YEAR);

        int year1 = cal1.get(Calendar.YEAR);
        int year2 = cal2.get(Calendar.YEAR);
        if(year1 != year2) {
            int timeDistance = 0 ;
            for(int i = year1 ; i < year2 ; i ++) {
                if(i%4==0 && i%100!=0 || i%400==0){
                    timeDistance += 366;
                } else {
                    timeDistance += 365;
                }
            }
            return timeDistance + (day2 - day1) ;
        } else {
            return day2 - day1;
        }
    }

    public static String formatUTCTime(String formerTime) {
        try {
            SimpleDateFormat format = getSimpleDateFormat();
            SimpleDateFormat temFormat = (SimpleDateFormat) format.clone();
            temFormat.applyPattern(pattern6);
            temFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = temFormat.parse(formerTime);
            long time = date.getTime();
            time = time / 1000;
            return String.valueOf(time);
        } catch (Exception ignore) {
        }
        return formerTime;
    }

    public static long addTimeDuration(int duration) {
        Calendar calendar = Calendar.getInstance();
        long timeInMillis = System.currentTimeMillis();

        calendar.setTimeInMillis(timeInMillis);
        calendar.set(Calendar.HOUR_OF_DAY, duration / (60 * 60));
        calendar.set(Calendar.MINUTE, duration / 60);
        calendar.set(Calendar.SECOND, duration % 60);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() < timeInMillis + 2000L){
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return calendar.getTimeInMillis();
    }


    /**
     * 获取显示时间
     * 当天时间显示hh:mm
     * 前一周显示周几
     * 一周前显示日期
     * @return 显示时期
     */
    public static String getWeekTime(long time) {
        Date  date1 = new Date(time);
        Date  date2 = new Date();
        int days = differentDays(date1, date2);
        if(days == 0){
            return format(time, pattern0);
        }else if(days > weeks.length){
            return format(time, pattern4);
        }else{
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getDefault());
            cal.setTimeInMillis(time);
            int weekIndex = cal.get(Calendar.DAY_OF_WEEK) - 1;
            if(weekIndex < 0){
                weekIndex = 0;
            }
            return weeks[weekIndex];
        }
    }

    public static String getWeekTimeWithHours(long time) {
        Date  date1 = new Date(time);
        Date  date2 = new Date();
        int days = differentDays(date1, date2);
        if(days == 0){
            return format(time, pattern0);
        }else if(days > weeks.length){
            return format(time, pattern5);
        }else{
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getDefault());
            cal.setTimeInMillis(time);
            int weekIndex = cal.get(Calendar.DAY_OF_WEEK) - 1;
            if(weekIndex < 0){
                weekIndex = 0;
            }
            return weeks[weekIndex] + " " + format(time, pattern0);
        }
    }

    /**
     * 得到今天剩余秒数
     * @return
     */
    public static int getTodayLastSeconds(){
        SimpleDateFormat format = getSimpleDateFormat();
        format.applyPattern(pattern4);
        TimeZone timeZone = TimeZone.getDefault();
        format.setTimeZone(timeZone);
        String today = format.format(new Date());
        // 得到今天 晚上的最后一刻 最后时间
        String last = today + " 23:59:59";
        format.applyPattern(pattern6);
        try {
            // 转换为今天
            Date latDate = format.parse(last);
            // 得到的毫秒 除以1000转换 为秒
            return (int)(latDate.getTime() - System.currentTimeMillis()) / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 得到今天剩余秒数
     * @return
     */
    public static int getTomorrowLastSeconds(int tomorrowHours){
        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        if (hours >= tomorrowHours) {
            // tomorrow
            calendar.add(Calendar.DATE, 1);
        }
        SimpleDateFormat format = getSimpleDateFormat();
        format.applyPattern(pattern4);
        TimeZone timeZone = TimeZone.getDefault();
        format.setTimeZone(timeZone);

        String tomorrow = format.format(calendar.getTime());
        // 得到明天的具体几点的最后时间
        String tomorrowHoursStr;
        tomorrowHours = tomorrowHours - 1;
        if (tomorrowHours < 10) {
            tomorrowHoursStr = "0" + tomorrowHours;
        } else {
            tomorrowHoursStr = "" + tomorrowHours;
        }
        String last = tomorrow + " " + tomorrowHoursStr + ":59:59";
        format.applyPattern(pattern6);
        try {
            // 转换为今天
            Date latDate = format.parse(last);
            // 得到的毫秒 除以1000转换 为秒
            return (int)(latDate.getTime() - System.currentTimeMillis()) / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getSeconds(long time1, long time2){
        return (int)(time2 - time1) / 1000;
    }

    /**
     * 获取当前时间前pastTime的时间
     * @param pastSecond 减去的时间，单位：秒
     */
    public static long getPastTime(int pastSecond){
        Date date = new Date();
        try {
            SimpleDateFormat format = getSimpleDateFormat();
            format.applyPattern(pattern6);
            format.format(date);

            Calendar newTime = Calendar.getInstance();
            newTime.setTime(date);
            newTime.add(Calendar.SECOND, -pastSecond);

            Date newDate = newTime.getTime();
            return newDate.getTime()  / 1000;
        } catch(Exception ignore) {
        }
        return date.getTime() / 1000;
    }

    /**
     * 获取当天的小时
     * @return 小时
     */
    public static int getHourOfDay() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取时间的小时
     * @return 小时
     */
    public static int getHourOfDay(long time) {
        try {
            Date date = new Date(time);
            SimpleDateFormat format = getSimpleDateFormat();
            format.applyPattern(pattern6);
            format.format(date);

            Calendar newTime = Calendar.getInstance();
            newTime.setTime(date);
            return newTime.get(Calendar.HOUR_OF_DAY);
        } catch(Exception ignore) {
        }
        return 0;
    }

    /**
     * 格式化时间显示
     * @param timeSeconds 秒数
     * @return 显示小时、分钟、秒
     */
    public static String getFormatTime(long timeSeconds) {
        Context context = MainApplication.getInstance();
        int unitResId;
        double time;
        if (timeSeconds < 60 ) {
            unitResId = R.string.setting_running_time_seconds;
            time = timeSeconds;
        } else if (timeSeconds < 60 * 60 ) {
            unitResId = R.string.setting_running_time_minutes;
            time = timeSeconds * 1.0 / 60;
        } else {
            unitResId = R.string.setting_running_time_hours;
            time = timeSeconds * 1.0 / 3600;
        }
        String timeStr = FmtMicrometer.formatTwoDecimal(time);
        return context.getString(unitResId, timeStr);
    }

    public static float timeDiffHours(long date1, long date2) {
        long millisecond;
        if (date1 >= date2) {
            millisecond = date1 - date2;
        } else {
            millisecond = date2 - date1;
        }
        return millisecond * 1.0f / 3600000;
    }

    public static float timeDiffMinutes(long date1, long date2) {
        long millisecond;
        if (date1 >= date2) {
            millisecond = date1 - date2;
        } else {
            millisecond = date2 - date1;
        }
        return millisecond * 1.0f / 60000;
    }

    public static boolean isShowTime(long currentTime, long previousTime) {
        if (previousTime > 0) {
            int interval = DateUtil.getSeconds(previousTime, currentTime);
            return interval > 2 * 60;
        }
        return true;
    }

    public static CharSequence getNewsTime(long timestamp) {
        long currentTime = getMillisTime();
        int seconds = getSeconds(timestamp, currentTime);
        int minute = seconds / 60 % 60;
        int hours = seconds / 60 / 60;
        int days = hours / 24;
        if (days > 7) {
            return DateUtil.format(timestamp, DateUtil.pattern4);
        } else if (days > 0) {
            return days + "day";
        } else if (hours <= 0) {
            return minute + "mins";
        } else {
            return hours + "h";
        }
    }
}