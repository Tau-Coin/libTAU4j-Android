package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import io.taucoin.torrent.publishing.R;

public class SpinnerAdapter extends BaseAdapter {
    private Context context;
    private String[] array;
    private int selectPos;

    SpinnerAdapter(Context context, String[] array) {
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
        ImageView ivSelected = view.findViewById(R.id.iv_selected);
        TextView tvContent = view.findViewById(R.id.tv_content);
        ivSelected.setVisibility(position == selectPos ? View.VISIBLE : View.INVISIBLE);
        tvContent.setText(array[position]);
        return view;
    }
}
