package io.taucoin.torrent.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.widget.EditText;

import io.taucoin.torrent.publishing.BuildConfig;
import io.taucoin.torrent.publishing.core.utils.ChineseFilter;

/**
 * 过滤中文输入框
 */
@SuppressLint({"AppCompatCustomView"})
public class FilterEditText extends EditText {
    public FilterEditText(Context context) {
        super(context);
        initFilters();
    }

    public FilterEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFilters();
    }

    public FilterEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFilters();
    }

    public FilterEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFilters();
    }

    private void initFilters() {
        if (BuildConfig.DEBUG) {
            return;
        }
        setFilters(new InputFilter[]{new ChineseFilter()});
    }
}
