package io.taucoin.news.publishing.ui.transaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import io.taucoin.news.publishing.R;

public class SpinnerAdapter extends BaseAdapter {
    private Context context;
    private int[] array;
    private int selectPos;

    public SpinnerAdapter(Context context, int[] array) {
        this.context = context;
        this.array = array;
    }

    public void setSelectPos(int selectPos) {
        this.selectPos = selectPos;
    }

    public int getSelectPos() {
        return selectPos;
    }

    @Override
    public int getCount() {
        return null == array ? 0 : array.length;
    }

    @Override
    public Object getItem(int position) {
        return array[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = layoutInflater.inflate(R.layout.item_spinner_dropdown, parent, false);
        }
        TextView tvContent = view.findViewById(R.id.tv_content);
        View line = view.findViewById(R.id.view_line);
        tvContent.setText(view.getResources().getString(array[position]));
        line.setVisibility(position == array.length - 1 ? View.GONE : View.VISIBLE);
        return view;
    }
}
