package io.taucbd.news.publishing.ui.customviews;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

public abstract class FragmentStatePagerAdapter extends androidx.fragment.app.FragmentStatePagerAdapter {
    private int count;
    public FragmentStatePagerAdapter(@NonNull FragmentManager fm, int count) {
        super(fm);
        this.count = count;
    }

    @Override
    public int getCount() {
        return count;
    }
}
