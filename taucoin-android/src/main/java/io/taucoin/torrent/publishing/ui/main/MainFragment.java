package io.taucoin.torrent.publishing.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.text.style.URLSpan;
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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.model.data.OperationMenuItem;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.ObservableUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.FragmentMainBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 群组列表页面
 */
public class MainFragment extends BaseFragment implements MainListAdapter.ClickListener {

    private MainActivity activity;

    private MainListAdapter adapter;
    private FragmentMainBinding binding;
    private MainViewModel viewModel;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private boolean dataChanged = false;
    private FloatMenu operationsMenu;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (MainActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(activity);
        viewModel = provider.get(MainViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        settingsRepo = RepositoryHelper.getSettingsRepository(MainApplication.getInstance());
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

        communityViewModel.getJoinedResult().observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess()) {
                Bundle bundle = new Bundle();
                bundle.putInt(IntentExtra.TYPE, 0);
                bundle.putString(IntentExtra.ID, result.getMsg());
                activity.updateMainRightFragment(bundle);
            }
        });
        showProgressDialog();
        viewModel.getHomeData().observe(getViewLifecycleOwner(), this::showCommunityList);
    }

    private void subscribeMainViewModel() {
        viewModel.queryHomeData();

        disposables.add(viewModel.observeHomeChanged()
                .subscribeOn(Schedulers.io())
                .subscribe(o -> dataChanged = true));

        disposables.add(ObservableUtil.interval(500)
                .subscribeOn(Schedulers.io())
                .subscribe(o -> {
                    if (dataChanged) {
                        viewModel.queryHomeData();
                        dataChanged = false;
                    }
                }));

        handleWarningView();
        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSettingsChanged));
    }

    private void handleWarningView() {
        binding.llWarning.setVisibility(View.VISIBLE);
        String interfacesKey = getString(R.string.pref_key_network_interfaces);
        if (!settingsRepo.internetState()) {
            binding.tvWarning.setText(getString(R.string.main_network_unavailable));
        } else if (StringUtil.isEquals(settingsRepo.getStringValue(interfacesKey, ""), "0.0.0.0")) {
            binding.tvWarning.setText(getString(R.string.main_no_ipv4));
        } else if (!NetworkSetting.isHaveAvailableData()) {
            binding.tvWarning.setText(getString(R.string.main_data_used_up));
        } else if (DeviceUtils.isSpaceInsufficient()) {
            binding.tvWarning.setText(getString(R.string.main_insufficient_device_space));
        } else {
            binding.llWarning.setVisibility(View.GONE);
        }
    }

    /**
     * 用户设置参数变化
     * @param key
     */
    private void handleSettingsChanged(String key) {
        if (StringUtil.isEquals(key, getString(R.string.pref_key_network_interfaces))) {
            handleWarningView();
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_internet_state))) {
            handleWarningView();
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_metered_prompt_limit))) {
            handleWarningView();
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_wifi_prompt_limit))) {
            handleWarningView();
        }
    }

    private void showCommunityList(List<CommunityAndFriend> communities) {
        closeProgressDialog();
        if (communities != null) {
            adapter.submitList(communities);
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
        subscribeMainViewModel();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
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

    @Override
    public void onCommunityJoined(String chainID) {
        communityViewModel.joinCommunity(chainID);
    }
}