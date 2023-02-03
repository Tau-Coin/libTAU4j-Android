package io.taucbd.news.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckedTextView;

import io.taucbd.news.publishing.R;

@SuppressLint("AppCompatCustomView")
public class SpinnerTextView extends CheckedTextView {

    public SpinnerTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setCheckMarkDrawable(getResources().getDrawable(R.mipmap.icon_done, null));
    }
}
