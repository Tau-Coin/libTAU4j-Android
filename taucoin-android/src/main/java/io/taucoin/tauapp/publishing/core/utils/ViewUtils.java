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

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ViewUtils {

    public static String getText(TextView view) {
        return view.getText().toString().trim();
    }

    public static String getStringTag(View view) {
        Object tag = view.getTag();
        if(tag != null){
            return view.getTag().toString().trim();
        }
        return "";
    }

    public static int getIntTag(View view) {
        String tag = getStringTag(view);
        try {
            return Integer.parseInt(tag);
        }catch (Exception ignore){
        }
        return 0;
    }

    public static long getLongTag(View view) {
        String tag = getStringTag(view);
        try {
            return Long.parseLong(tag);
        }catch (Exception ignore){
        }
        return 0L;
    }

    public static double getDoubleText(TextView view) {
        String text = getText(view);
        try {
            return Double.parseDouble(text);
        }catch (Exception ignore){
        }
        return 0d;
    }

    public static float getFloatText(TextView view) {
        String text = getText(view);
        try {
            return Float.parseFloat(text);
        }catch (Exception ignore){
        }
        return 0f;
    }

    public static float getFloatTag(TextView view) {
        String text = getStringTag(view);
        try {
            return Float.parseFloat(text);
        }catch (Exception ignore){
        }
        return 0f;
    }

    public static boolean getBooleanTag(View view) {
        try {
            Object obj = view.getTag();
            return Boolean.parseBoolean(obj.toString());
        } catch (Exception ignore) {
        }
        return false;
    }

    public static int getIntText(TextView view) {
        String text = getText(view);
        try {
            return Integer.parseInt(text);
        }catch (Exception ignore){
        }
        return 0;
    }

    public static void updateViewWeight(View view, float weight) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)
                view.getLayoutParams();
        layoutParams.weight = weight;
        view.setLayoutParams(layoutParams);
    }
}
