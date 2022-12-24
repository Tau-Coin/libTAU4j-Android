package io.taucoin.news.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;

import org.slf4j.LoggerFactory;

import java.util.List;

import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.utils.LargeValueFormatter;

/**
 * 自定义MyMarkerView
 */
@SuppressLint("ViewConstructor")
public class MyMarkerView extends MarkerView {

    private TextView tvContent;
    private LargeValueFormatter leftFormatter;
    private LargeValueFormatter rightFormatter;
    private List<String> xValues;

    public MyMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e instanceof CandleEntry) {
            CandleEntry ce = (CandleEntry) e;
            tvContent.setText(Utils.formatNumber(ce.getHigh(), 2, false));
        } else {
            String value;
            if (highlight.getAxis().equals(YAxis.AxisDependency.RIGHT)) {
                value = getRightFormatter().getFormattedValue(e.getY());
            } else {
                value = getLeftFormatter().getFormattedValue(e.getY());
            }
            if (xValues != null && e.getX() < xValues.size()) {
                value =xValues.get((int) e.getX()) + ", " + value;
            }
            tvContent.setText(value);
//            tvContent.setText(Utils.formatNumber(e.getY(), 2, false));
        }
        LoggerFactory.getLogger("MyMarkerView").debug("MyMarkerView text::{}",
                tvContent.getText().toString());
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() * 1.0f / 2), -getHeight());
    }

    public void setXValues(List<String> xValues) {
        this.xValues = xValues;
    }

    private LargeValueFormatter getLeftFormatter() {
        if (null == leftFormatter) {
            leftFormatter = new LargeValueFormatter();
        }
        return leftFormatter;
    }

    private LargeValueFormatter getRightFormatter() {
        if (null == rightFormatter) {
            rightFormatter = new LargeValueFormatter();
        }
        return rightFormatter;
    }

    public void setLeftValueFormatter(LargeValueFormatter formatter) {
        this.leftFormatter = formatter;
    }

    public void setRightValueFormatter(LargeValueFormatter formatter) {
        this.rightFormatter = formatter;
    }
}
