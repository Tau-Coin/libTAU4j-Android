package io.taucoin.tauapp.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import io.taucoin.tauapp.publishing.BuildConfig;
import io.taucoin.tauapp.publishing.core.model.DozeEvent;
import io.taucoin.tauapp.publishing.core.model.TauDaemon;
import io.taucoin.tauapp.publishing.core.utils.ChineseFilter;

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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeTextChangedListener(textWatcher);
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            TauDaemon.getInstance(getContext().getApplicationContext()).newActionEvent(DozeEvent.TEXT_INPUT);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void initFilters() {
        addTextChangedListener(textWatcher);
        if (BuildConfig.DEBUG) {
            return;
        }
        setFilters(new InputFilter[]{new ChineseFilter()});
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
