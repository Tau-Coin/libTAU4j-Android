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
package io.taucoin.tauapp.publishing.ui.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.databinding.DataBindingUtil;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.databinding.ViewLeftRightLineBinding;

public class LeftRightLineView extends RelativeLayout {
    private String leftText;
    private String rightText;
    private int leftTextColor;
    private int rightTextColor;
    private int leftImage;
    private int rightImage;
    private boolean lineVisibility;
    private int lineHeight;
    private int lineLeftTextStartMargin;
    private ViewLeftRightLineBinding binding;

    public LeftRightLineView(Context context) {
        this(context, null);
    }

    public LeftRightLineView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LeftRightLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(attrs);

    }

    private void initData(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.LeftRightLineView);
        this.leftImage = a.getResourceId(R.styleable.LeftRightLineView_lineLeftImage, -1);
        this.leftText = a.getString(R.styleable.LeftRightLineView_lineLeftText);
        this.leftTextColor = a.getColor(R.styleable.LeftRightLineView_lineLeftTextColor, getResources().getColor(R.color.color_black));
        this.rightText = a.getString(R.styleable.LeftRightLineView_lineRightText);
        this.rightImage = a.getResourceId(R.styleable.LeftRightLineView_lineRightImage, -1);
        this.rightTextColor = a.getColor(R.styleable.LeftRightLineView_lineRightTextColor, getResources().getColor(R.color.gray_dark));
        this.lineVisibility = a.getBoolean(R.styleable.LeftRightLineView_lineVisibility, true);
        this.lineHeight = a.getDimensionPixelSize(R.styleable.LeftRightLineView_lineHeight, getResources().getDimensionPixelSize(R.dimen.widget_size_44));
        this.lineLeftTextStartMargin = a.getDimensionPixelSize(R.styleable.LeftRightLineView_lineLeftTextStartMargin, getResources().getDimensionPixelSize(R.dimen.widget_size_5));
        a.recycle();
        loadView();
    }

    private void loadView() {
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.view_left_right_line, this, true);
        if (leftImage != -1) {
            binding.ivLeft.setImageResource(leftImage);
        } else {
            binding.ivLeft.setVisibility(GONE);
        }
        if (StringUtil.isNotEmpty(leftText)) {
            if (leftImage != -1) {
                binding.tvLeft1.setText(leftText);
                binding.tvLeft2.setVisibility(GONE);
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) binding.tvLeft1.getLayoutParams();
                layoutParams.leftMargin = lineLeftTextStartMargin;
                binding.tvLeft1.setLayoutParams(layoutParams);
            } else {
                binding.tvLeft2.setText(leftText);
                binding.tvLeft1.setVisibility(GONE);
            }
        }
        if(StringUtil.isNotEmpty(rightText)){
            binding.tvRight.setText(rightText);
        } else {
            binding.tvRight.setVisibility(GONE);
        }
        if (rightImage != -1) {
            binding.ivRight.setImageResource(rightImage);
        } else {
            binding.ivRight.setVisibility(GONE);
        }
        binding.tvLeft1.setTextColor(leftTextColor);
        binding.tvLeft2.setTextColor(leftTextColor);
        binding.tvRight.setTextColor(rightTextColor);
        binding.lineView.setVisibility(lineVisibility ? VISIBLE : GONE);
        binding.rlItem.setMinimumHeight(lineHeight);
    }

    public void setRightText(CharSequence rightText) {
        this.rightText = rightText.toString();
       if(binding != null){
           binding.tvRight.setText(rightText);
           binding.tvRight.setVisibility(VISIBLE);
       }
    }

    public void setLeftText2(int resId) {
        if(binding != null){
            binding.tvLeft2.setText(resId);
        }
    }
}
