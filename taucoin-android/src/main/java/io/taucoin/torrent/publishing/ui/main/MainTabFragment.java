package io.taucoin.torrent.publishing.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.noober.menu.FloatMenu;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.MyAccountManager;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.model.data.OperationMenuItem;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.FragmentMainTabBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 群组列表Tab页面
 */
public class MainTabFragment extends BaseFragment implements MainListAdapter.ClickListener {

    private MainActivity activity;
    private MainListAdapter adapter;
    private FragmentMainTabBinding binding;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private FloatMenu operationsMenu;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_tab, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (MainActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(activity);
        communityViewModel = provider.get(CommunityViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        initView();
    }

    private void initView() {
        adapter = new MainListAdapter(this);
        binding.refreshLayout.setRefreshing(false);
        binding.refreshLayout.setEnabled(false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.groupList.setLayoutManager(layoutManager);
        binding.groupList.setItemAnimator(null);
        binding.groupList.setAdapter(adapter);
        if (getArguments() != null) {
            List<CommunityAndFriend> list = getArguments().getParcelableArrayList(IntentExtra.BEAN);
            if (list != null) {
                adapter.submitList(list);
            }
        }
        Context context = activity.getApplicationContext();
        MyAccountManager myAccountManager = TauDaemon.getInstance(context).getMyAccountManager();
        myAccountManager.getNotExpiredChain().observe(this.getViewLifecycleOwner(), set -> {
                    adapter.notifyDataSetChanged();
        });
    }

    void showDataList(List<CommunityAndFriend> list) {
        if (adapter != null && list != null) {
            adapter.submitList(list);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)
            activity = (MainActivity) context;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (operationsMenu != null) {
            operationsMenu.setOnItemClickListener(null);
            operationsMenu.dismiss();
        }
    }

    /**
     * 社区ListItem点击事件
     */
    @Override
    public void onItemClicked(@NonNull CommunityAndFriend item) {
        Bundle bundle = new Bundle();
        bundle.putInt(IntentExtra.TYPE, item.type);
        bundle.putString(IntentExtra.ID, item.ID);
        activity.updateMainRightFragment(bundle);
    }

    @Override
    public void onItemLongClicked(CommunityAndFriend bean) {
        List<OperationMenuItem> menuList = new ArrayList<>();
        if (bean.msgUnread == 1) {
            menuList.add(new OperationMenuItem(R.string.main_operation_mark_read));
        } else {
            menuList.add(new OperationMenuItem(R.string.main_operation_mark_unread));
        }
        if (bean.stickyTop == 1) {
            menuList.add(new OperationMenuItem(R.string.main_operation_remove_top));
        } else {
            menuList.add(new OperationMenuItem(R.string.main_operation_sticky_top));
        }
        if (bean.type == 0) {
            menuList.add(new OperationMenuItem(R.string.main_operation_ban_community));
        } else {
            // 不能拉黑自己
            if (StringUtil.isNotEquals(bean.ID, MainApplication.getInstance().getPublicKey())) {
                menuList.add(new OperationMenuItem(R.string.main_operation_ban_friend));
            }
        }
        operationsMenu = new FloatMenu(activity);
        operationsMenu.items(menuList);
        operationsMenu.setOnItemClickListener((v, position) -> {
            OperationMenuItem menuItem = menuList.get(position);
            int resId = menuItem.getResId();
            switch (resId) {
                case R.string.main_operation_mark_read:
                case R.string.main_operation_mark_unread:
                    int status = bean.msgUnread == 0 ? 1 : 0;
                    if (bean.type == 0) {
                        communityViewModel.markReadOrUnread(bean.ID, status);
                    } else {
                        userViewModel.markReadOrUnread(bean.ID, status);
                    }
                    break;
                case R.string.main_operation_sticky_top:
                case R.string.main_operation_remove_top:
                    int top = bean.stickyTop == 0 ? 1 : 0;
                    if (bean.type == 0) {
                        communityViewModel.topStickyOrRemove(bean.ID, top);
                    } else {
                        userViewModel.topStickyOrRemove(bean.ID, top);
                    }
                    break;
                case R.string.main_operation_ban_community:
                    communityViewModel.setCommunityBlacklist(bean.ID, true);
                    break;
                case R.string.main_operation_ban_friend:
                    userViewModel.setUserBlacklist(bean.ID, true);
                    break;
            }
        });
        operationsMenu.show(activity.getPoint());
    }
}