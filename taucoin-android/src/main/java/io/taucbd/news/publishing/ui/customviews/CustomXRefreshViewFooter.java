package io.taucbd.news.publishing.ui.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.andview.refreshview.R.string;
import com.andview.refreshview.R.id;
import com.andview.refreshview.R.layout;
import com.andview.refreshview.XRefreshView;
import com.andview.refreshview.callback.IFooterCallBack;

import io.taucbd.news.publishing.R;

public class CustomXRefreshViewFooter extends LinearLayout implements IFooterCallBack {
    private Context mContext;
    private View mContentView;
    private View mProgressBar;
    private TextView mHintView;
    private TextView mClickView;
    private boolean showing = true;

    public CustomXRefreshViewFooter(Context context) {
        super(context);
        this.initView(context);
    }

    public CustomXRefreshViewFooter(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initView(context);
    }

    public void callWhenNotAutoLoadMore(final XRefreshView xRefreshView) {
        this.mClickView.setText(string.xrefreshview_footer_hint_click);
        this.mClickView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                xRefreshView.notifyLoadMore();
            }
        });
    }

    public void onStateReady() {
        this.mHintView.setVisibility(GONE);
        this.mProgressBar.setVisibility(GONE);
        this.mClickView.setText(string.xrefreshview_footer_hint_click);
        this.mClickView.setVisibility(VISIBLE);
    }

    public void onStateRefreshing() {
        this.mHintView.setVisibility(GONE);
        this.mProgressBar.setVisibility(VISIBLE);
        this.mClickView.setVisibility(GONE);
        this.show(true);
    }

    public void onReleaseToLoadMore() {
        this.mHintView.setVisibility(GONE);
        this.mProgressBar.setVisibility(GONE);
        this.mClickView.setText(string.xrefreshview_footer_hint_release);
        this.mClickView.setVisibility(VISIBLE);
    }

    public void onStateFinish(boolean hideFooter) {
        if (hideFooter) {
            this.mHintView.setText(string.xrefreshview_footer_hint_normal);
        } else {
            this.mHintView.setText(string.xrefreshview_footer_hint_fail);
        }

        this.mHintView.setVisibility(VISIBLE);
        this.mProgressBar.setVisibility(GONE);
        this.mClickView.setVisibility(GONE);
    }

    public void onStateComplete() {
        this.mHintView.setText(string.xrefreshview_footer_hint_complete);
        this.mHintView.setVisibility(VISIBLE);
        this.mProgressBar.setVisibility(GONE);
        this.mClickView.setVisibility(GONE);
    }

    public void show(boolean show) {
        if (show != this.showing) {
            this.showing = show;
            LayoutParams lp = (LayoutParams)this.mContentView.getLayoutParams();
            lp.height = show ? -2 : VISIBLE;
            this.mContentView.setLayoutParams(lp);
        }
    }

    public boolean isShowing() {
        return this.showing;
    }

    private void initView(Context context) {
        this.mContext = context;
        ViewGroup moreView = (ViewGroup) LayoutInflater.from(this.mContext).inflate(layout.xrefreshview_footer, this);
        moreView.setLayoutParams(new LayoutParams(-1, -2));
        this.mContentView = moreView.findViewById(id.xrefreshview_footer_content);
        this.mProgressBar = moreView.findViewById(id.xrefreshview_footer_progressbar);
        ViewGroup.LayoutParams layoutParams = this.mProgressBar.getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelSize(R.dimen.widget_size_35);
        this.mProgressBar.setLayoutParams(layoutParams);
        this.mHintView = (TextView)moreView.findViewById(id.xrefreshview_footer_hint_textview);
        this.mClickView = (TextView)moreView.findViewById(id.xrefreshview_footer_click_textview);
    }

    public int getFooterHeight() {
        return this.getMeasuredHeight();
    }
}
