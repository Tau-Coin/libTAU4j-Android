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
package io.taucbd.news.publishing.ui.customviews;

import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.databinding.DialogGuideBinding;
import io.taucbd.news.publishing.databinding.ItemGuideBinding;

/**
 * 引导页对话框
 */
public class GuideDialog extends Dialog {

    public GuideDialog(Context context) {
        super(context);
    }

    public GuideDialog(Context context, int theme) {
        super(context, theme);
    }

    public static class Builder {
        private Context context;
        private boolean isCanCancel = true;
        private GuideListener guideListener;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setCanceledOnTouchOutside(boolean cancel) {
            this.isCanCancel = cancel;
            return this;
        }

        public Builder setGuideListener(GuideListener guideListener) {
            this.guideListener = guideListener;
            return this;
        }

        public GuideDialog create() {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            DialogGuideBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_guide,
                    null, false);
            View layout = binding.getRoot();
            final GuideDialog guideDialog = new GuideDialog(context, R.style.CommonDialog);
            binding.tvSkip.setOnClickListener(v -> {
                if (guideDialog.isShowing()) {
                    guideDialog.closeDialog();
                }
                if (guideListener != null) {
                    guideListener.onCancel();
                }
            });
            guideDialog.addContentView(layout, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            guideDialog.setCanceledOnTouchOutside(isCanCancel);
            guideDialog.setOnCancelListener(dialog -> {
                if (guideListener != null) {
                    guideListener.onCancel();
                }
            });

            WindowManager windowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            LayoutParams layoutParams = layout.getLayoutParams();
            layoutParams.width = (int) (display.getWidth() * 0.85);
            layout.setLayoutParams(layoutParams);

            int pointSize = context.getResources().getDimensionPixelSize(R.dimen.widget_size_7);
            LinearLayout.LayoutParams pointParams = new LinearLayout.LayoutParams(pointSize, pointSize);
            pointParams.leftMargin = context.getResources().getDimensionPixelSize(R.dimen.widget_size_2);
            pointParams.rightMargin = pointParams.leftMargin;
            List<View> viewList = new ArrayList<>();
            int[] guides = new int[]{R.mipmap.icon_guide2};
            int[] texts = new int[]{R.string.home_guide2};
            for (int i = 0; i < guides.length; i++) {
                ItemGuideBinding itemBinding = DataBindingUtil.inflate(inflater, R.layout.item_guide,
                        null, false);
                viewList.add(itemBinding.getRoot());
                itemBinding.ivGuide.setImageResource(guides[i]);
                itemBinding.tvGuide.setText(Html.fromHtml(context.getString(texts[i])));

                ImageView iv_point = new ImageView(context);
                iv_point.setLayoutParams(pointParams);
                if (i == 0) {
                    iv_point.setBackgroundResource(R.drawable.point_grey);
                } else {
                    iv_point.setBackgroundResource(R.drawable.point_grey_border);
                }
                binding.llPoint.addView(iv_point);
                binding.llPoint.setVisibility(guides.length > 1 ? View.VISIBLE : View.GONE);
            }
            binding.viewPager.setAdapter(new GuidePageAdapter(viewList));
            binding.viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                   int count = binding.llPoint.getChildCount();
                    for (int i = 0; i < count; i++) {
                        ImageView point = (ImageView) binding.llPoint.getChildAt(i);
                        if (i == position) {
                            point.setBackgroundResource(R.drawable.point_grey);
                        } else {
                            point.setBackgroundResource(R.drawable.point_grey_border);
                        }
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
            return guideDialog;
        }
    }

    private static class GuidePageAdapter extends PagerAdapter {
        private List<View> viewList;
        GuidePageAdapter(List<View> viewList) {
            this.viewList = viewList;
        }

        @Override
        public int getCount() {
            if (viewList != null) {
                return viewList.size();
            }
            return 0;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            container.addView(viewList.get(position));
            return viewList.get(position);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView(viewList.get(position));
        }
    }

    public interface GuideListener {
        void onCancel();
    }

    public void closeDialog(){
        if(isShowing()){
            dismiss();
        }
    }
}