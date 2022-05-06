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

    public OnPasteCallback mOnPasteCallback;

    public interface OnPasteCallback {
        void onPaste();
    }

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
//        if (BuildConfig.DEBUG) {
//            return;
//        }
//        setFilters(new InputFilter[]{new ChineseFilter()});
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        switch (id) {
            case android.R.id.cut:
                // 剪切
                break;
            case android.R.id.copy:
                // 复制
                break;
            case android.R.id.paste:
                // 粘贴
                if (mOnPasteCallback != null) {
                    mOnPasteCallback.onPaste();
                }
        }
        return super.onTextContextMenuItem(id);
    }

    public void setOnPasteCallback(OnPasteCallback onPasteCallback) {
        mOnPasteCallback = onPasteCallback;
    }
}
