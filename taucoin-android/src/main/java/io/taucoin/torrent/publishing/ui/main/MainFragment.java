package io.taucoin.torrent.publishing.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.FragmentMainBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 群组列表页面
 */
public class MainFragment extends BaseFragment implements MainListAdapter.ClickListener {

    private MainActivity activity;

    private MainListAdapter adapter;
    private FragmentMainBinding binding;
    private MainViewModel viewModel;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();

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
        settingsRepo = RepositoryHelper.getSettingsRepository(MainApplication.getInstance());
        initView();
    }

    private void initView() {
        adapter = new MainListAdapter(this);
//        /*
//         * A RecyclerView by default creates another copy of the ViewHolder in order to
//         * fade the views into each other. This causes the problem because the old ViewHolder gets
//         * the payload but then the new one doesn't. So needs to explicitly tell it to reuse the old one.
//         */
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.groupList.setLayoutManager(layoutManager);
        binding.groupList.setItemAnimator(animator);
        binding.groupList.setAdapter(adapter);
    }

    private void subscribeMainViewModel() {
        disposables.add(viewModel.observeCommunitiesAndFriends()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showCommunityList));

        handleSettingsChanged(getString(R.string.pref_key_network_interfaces));
        handleSettingsChanged(getString(R.string.pref_key_dht_nodes));
        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSettingsChanged));
    }

    private void handleWarningView(long nodes) {
        binding.llWarning.setVisibility(View.VISIBLE);
        if (nodes <= 0) {
            binding.tvWarning.setText(getString(R.string.main_connecting));
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
            String networkInterfaces = settingsRepo.getStringValue(key, "");
            if (StringUtil.isEquals(networkInterfaces, "0.0.0.0")) {
                binding.tvWarning.setText(getString(R.string.main_no_ipv4));
            }
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_dht_nodes))) {
            long nodes = settingsRepo.getLongValue(key, 0L);
            handleWarningView(nodes);
        }
    }

    private void showCommunityList(List<CommunityAndFriend> communities) {
        if(communities != null){
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
}