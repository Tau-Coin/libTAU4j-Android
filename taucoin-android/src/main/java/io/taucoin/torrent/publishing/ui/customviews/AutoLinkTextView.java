package io.taucoin.torrent.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class AutoLinkTextView extends TextView {

    private AutoLinkListener listener;
    // 消费了点击事件
    private boolean usedClickListener = false;

    public AutoLinkTextView(Context context) {
        this(context, null);
    }

    public AutoLinkTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoLinkTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnClickListener(l -> {
            if (usedClickListener) {
                usedClickListener = false;
                return;
            }
            if (listener != null) {
                listener.onClick(this);
            }
        });

        setOnLongClickListener(l -> {
            if (listener != null) {
                listener.onLongClick(this);
            }
            return true;
        });

        // 此setMovementMethod可被修改
        setMovementMethod(new LinkMovementMethodInterceptor());
    }

    /**
     * 处理内容链接跳转
     */
    private class LinkMovementMethodInterceptor extends LinkMovementMethod {

        private long downLinkTime;

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                ClickableSpan[] links =
                        buffer.getSpans(off, off, ClickableSpan.class);

                if (links.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        // 长按
                        if (downLinkTime + ViewConfiguration.getLongPressTimeout() < System.currentTimeMillis()) {
                            return false;
                        }
                        // 点击
                        if (links[0] instanceof URLSpan) {
                            URLSpan url = (URLSpan) links[0];
                            if (!TextUtils.isEmpty(url.getURL())) {
                                if (null != listener) {
                                    usedClickListener = true;
                                    listener.onLinkClick(url.getURL());
                                }
                                return true;
                            } else {
                                links[0].onClick(widget);
                            }
                        }
                    } else if (action == MotionEvent.ACTION_DOWN) {
                        downLinkTime = System.currentTimeMillis();
                        Selection.setSelection(buffer,
                                buffer.getSpanStart(links[0]),
                                buffer.getSpanEnd(links[0]));
                    }
                    return true;
                } else {
                    Selection.removeSelection(buffer);
                }
            }

            return super.onTouchEvent(widget, buffer, event);
        }

    }

    public void setAutoLinkListener(AutoLinkListener listener) {
        this.listener = listener;
    }

    public interface AutoLinkListener {

        void onClick(AutoLinkTextView view);

        void onLongClick(AutoLinkTextView view);

        void onLinkClick(String link);
    }
}
