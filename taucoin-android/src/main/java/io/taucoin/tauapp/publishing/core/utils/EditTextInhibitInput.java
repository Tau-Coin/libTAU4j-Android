package io.taucoin.tauapp.publishing.core.utils;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EditText抑制输入
 */
public class EditTextInhibitInput implements InputFilter {
    public static final String WELL_REGEX = "^[#]+$";
    public static final String COMMUNITY_NAME_REGEX = "^[A-Za-z0-9\\s-]+$"; // 字母、数字、空格、-
    public static final String NICKNAME_REGEX = "^[%&]+$"; // 禁止%，&
    private String regex;
    private boolean isCanInput;

    public EditTextInhibitInput(String regex) {
        this.regex = regex;
        this.isCanInput = true;
    }

    public EditTextInhibitInput(String regex, boolean isCanInput) {
        this.regex = regex;
        this.isCanInput = isCanInput;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        if (StringUtil.isNotEmpty(regex)) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(source.toString());
            boolean isFind = matcher.find();
            if (isFind) {
                if (isCanInput) {
                    return null;
                } else {
                    return "";
                }
            } else {
                if (isCanInput) {
                    return "";
                } else {
                    return null;
                }
            }
        }
        return null;
    }
}
