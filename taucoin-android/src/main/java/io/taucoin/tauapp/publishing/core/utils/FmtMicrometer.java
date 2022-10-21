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
package io.taucoin.tauapp.publishing.core.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class FmtMicrometer {

    public static String fmtBalance(long balance) {
        DecimalFormat df = getDecimalFormatInstance();
        df.applyPattern("###,##0.##");
        df.setRoundingMode(RoundingMode.FLOOR);
        BigDecimal bigDecimal = new BigDecimal(balance);
        return df.format(bigDecimal);
    }

    public static String fmtMiningIncome(long balance) {
        DecimalFormat df = getDecimalFormatInstance();
        df.applyPattern("###,##0.##");
        df.setRoundingMode(RoundingMode.FLOOR);
        BigDecimal bigDecimal = new BigDecimal(balance);
        return df.format(bigDecimal);
    }

    public static String fmtLong(long power) {
        return fmtString(String.valueOf(power));
    }

    public static String fmtBigInteger(BigInteger bigInteger) {
        try {
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("###,##0");
            df.setRoundingMode(RoundingMode.FLOOR);
            BigDecimal bigDecimal = new BigDecimal(bigInteger.toString());
            return df.format(bigDecimal);
        }catch (Exception ignore) {

        }
        return new BigInteger("0").toString();
    }

    public static String fmtString(String power) {
        try {
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("###,##0");
            df.setRoundingMode(RoundingMode.FLOOR);
            BigDecimal bigDecimal = new BigDecimal(power);
            return df.format(bigDecimal);
        }catch (Exception ignore) {

        }
        return new BigInteger("0").toString();
    }

    private static String fmtDecimal(double value, String pattern) {
        try {
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern(pattern);
            df.setRoundingMode(RoundingMode.FLOOR);
            BigDecimal bigDecimal = new BigDecimal(value);
            return df.format(bigDecimal);
        } catch (Exception ignore) {
        }
        return new BigInteger("0").toString();
    }

    public static String fmtFixedDecimal(double value) {
        return fmtDecimal(value, "###,##0.00");
    }

    public static String fmtDecimal(double value) {
        return fmtDecimal(value, "###,##0.##");
    }

    static DecimalFormat getDecimalFormatInstance() {
        DecimalFormat df;
        try{
            df = (DecimalFormat)NumberFormat.getInstance(Locale.CHINA);
        }catch (Exception e){
            df = new DecimalFormat();
        }
        return df;
    }

    public static String fmtFormat(String num) {
        try {
            BigDecimal number = new BigDecimal(num);
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.##");
            return df.format(number);
        } catch (Exception e) {
            return num;
        }
    }

    public static String fmtFeeValue(long value) {
        return fmtFeeValue(String.valueOf(value));
    }

    public static String fmtFeeValue(String value) {
        try{
            BigDecimal bigDecimal = new BigDecimal(value);
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.##");
            return df.format(bigDecimal);
        }catch (Exception ignore){

        }
        return new BigInteger("0").toString();
    }

    public static String fmtTxValue(String value) {
        try{
            BigDecimal bigDecimal = new BigDecimal(value);
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0");
            return df.format(bigDecimal);
        }catch (Exception ignore){

        }
        return new BigInteger("0").toString();
    }

    public static long fmtTxLongValue(String value) {
        String txValue = fmtTxValue(value);
        return new BigInteger(txValue).longValue();
    }

    public static String fmtFormatFee(String num, String multiply) {
        try {
            BigDecimal number = new BigDecimal(num);
            number = number.multiply(new BigDecimal(multiply));

            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.##");
            return df.format(number);
        } catch (Exception e) {
            return num;
        }
    }

    public static String fmtFormatAdd(String amount, String fee) {
        try {
            BigDecimal bigDecimal = new BigDecimal(amount);
            bigDecimal = bigDecimal.add(new BigDecimal(fee));
            return bigDecimal.toString();
        } catch (Exception e) {
            return amount;
        }
    }

    public static String formatTwoDecimal(double num) {
        try {
            BigDecimal number = new BigDecimal(num);
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.##");
            return df.format(number);
        } catch (Exception e) {
            return new BigInteger("0").toString();
        }
    }

    public static String fmtTestData(long balance) {
        DecimalFormat df = getDecimalFormatInstance();
        df.applyPattern("00000");
        df.setRoundingMode(RoundingMode.FLOOR);
        BigDecimal bigDecimal = new BigDecimal(balance);
        return df.format(bigDecimal);
    }

    public static double formatDecimal(double num, int scale) {
        BigDecimal bd = new BigDecimal(num);
        return bd.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
