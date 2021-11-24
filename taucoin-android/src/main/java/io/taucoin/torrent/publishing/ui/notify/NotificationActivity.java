package io.taucoin.torrent.publishing.ui.notify;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.NotificationAndUser;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.databinding.ActivityNotificationsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;

/**
 * 通知页面
 */
public class NotificationActivity extends BaseActivity implements View.OnClickListener {
    private ActivityNotificationsBinding binding;
    private CommunityViewModel communityViewModel;
    private NotificationViewModel notifyViewModel;
    private NotificationAdapter adapter;
    private CompositeDisposable disposables = new CompositeDisposable();
    private boolean isEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_notifications);
        ViewModelProvider provider = new ViewModelProvider(this);
        notifyViewModel = provider.get(NotificationViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.notifications_title);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new NotificationAdapter();
        /*
         * A RecyclerView by default creates another copy of the ViewHolder in order to
         * fade the views into each other. This causes the problem because the old ViewHolder gets
         * the payload but then the new one doesn't. So needs to explicitly tell it to reuse the old one.
         */
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(animator);
        binding.recyclerList.setEmptyView(binding.emptyViewList);
        binding.recyclerList.setAdapter(adapter);

//        notifyViewModel.observerNotifications().observe(this, list -> {
//            adapter.submitList(list);
//            notifyViewModel.readAllNotifications();
//        });

        List<NotificationAndUser> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            NotificationAndUser nu = new NotificationAndUser("123123123123123123123123123123123123",
                    "", "TEST#123123123123123123123123123123123123", DateUtil.getMillisTime());
            list.add(nu);
        }
        adapter.submitList(list);
    }

    @Override
    public void onStart() {
        super.onStart();
        notifyViewModel.getDeleteState().observe(this, result -> {
            if(result.isSuccess()){
                adapter.getSelectedList().clear();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_ignore:
                break;
            case R.id.tv_accept:
                break;
        }
    }
}