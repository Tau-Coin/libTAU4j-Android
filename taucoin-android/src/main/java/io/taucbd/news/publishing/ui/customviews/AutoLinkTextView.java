package io.taucbd.news.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

import androidx.annotation.Nullable;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;

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

        //android9和10的行高问题的解决暂时解决方案
        //会造成开销，最好还是想办法把中英文的行高固定下来(降低中文行高)
        // @see https://www.jianshu.com/p/f8e69ffa0c13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setFallbackLineSpacing(false);
        }
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

    private final int maxLineCount = 5;
    private String mText;
    private StaticLayout sl;
    final String ellipsizeText = "......";
    public void setEllipsizeText(String text) {
        mText = text;
        // 设置要显示的文字，这一行必须要，否则 onMeasure 宽度测量不正确
        setText(text);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (StringUtil.isEmpty(mText)) {
            return;
        }
        // 文字计算辅助工具
        sl = new StaticLayout(mText, getPaint(), getMeasuredWidth() - getPaddingLeft() - getPaddingRight()
                , Layout.Alignment.ALIGN_CENTER, 1, 0, true);
        // 总计行数
        int lineCount = sl.getLineCount();
        if (lineCount > maxLineCount) {
                lineCount = maxLineCount;
                // 省略文字的宽度
                float dotWidth = getPaint().measureText(ellipsizeText);
                // 找出第 showLineCount 行的文字
                int start = sl.getLineStart(lineCount - 1);
                int end = sl.getLineEnd(lineCount - 1);
                String lineText = mText.substring(start, end);
                lineText = lineText.replaceAll("\n", "");
                if (lineText.length() > 6) {
                    // 将第 showLineCount 行最后的文字替换为 ellipsizeText
                    char[] charArray = lineText.toCharArray();
                    StringBuilder str = new StringBuilder();
                    int endIndex = 0;
                    for (int i = charArray.length - 1; i >= 0; i--) {
                        str.append(charArray[i]);
                        if (getPaint().measureText(str.toString()) >= dotWidth) {
                            endIndex = i;
                            break;
                        }
                    }
                    str = new StringBuilder();
                    for (int i = 0; i <  endIndex; i++) {
                        str.append(charArray[i]);
                    }
                    lineText = str + ellipsizeText;
//                    int endIndex = 0;
//                    for (int i = lineText.length() - 1; i >= 0; i--) {
//                        String str = lineText.substring(i);
//                        // 找出文字宽度大于 ellipsizeText 的字符
//                        if (getPaint().measureText(str) >= dotWidth) {
//                            endIndex = i;
//                            break;
//                        }
//                    }
                    // 新的第 showLineCount 的文字
//                    lineText = lineText.substring(0, endIndex) + ellipsizeText;
                } else {
                    lineText = lineText + ellipsizeText;
                }
                // 最终显示的文字
                setText(mText.substring(0, start) + lineText);
        } else {
            setText(mText);
        }

        // 重新计算高度
        int lineHeight = 0;
        for (int i = 0; i < lineCount; i++) {
//            Rect lineBound = new Rect();
//            sl.getLineBounds(i, lineBound);
//            lineHeight += lineBound.height();
            // 上面方法也可以
            lineHeight += sl.getLineDescent(i) - sl.getLineAscent(i);
        }
        lineHeight += getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(getMeasuredWidth(), lineHeight);
        addAutoLinks();
    }

    private void addAutoLinks() {
        setAutoLinkMask(0);
        Linkify.addLinks(this, Linkify.WEB_URLS);
        Linkify.addLinks(this, LinkUtil.REFERRAL, null);
        Linkify.addLinks(this, LinkUtil.AIRDROP, null);
        Linkify.addLinks(this, LinkUtil.CHAIN, null);
        Linkify.addLinks(this, LinkUtil.FRIEND, null);
    }
}
