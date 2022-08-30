package io.taucoin.tauapp.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class SquareTextView extends TextView {

    public SquareTextView(Context context) {
        super(context);
    }

    public SquareTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
