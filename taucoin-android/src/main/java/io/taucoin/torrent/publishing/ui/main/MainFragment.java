package io.taucoin.torrent.publishing.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
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
import io.taucoin.torrent.publishing.core.utils.ObservableUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.FragmentMainBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.FragmentStatePagerAdapter;

/**
 * 群组列表页面
 */
public class MainFragment extends BaseFragment {

    private MainActivity activity;

    private FragmentMainBinding binding;
    private MainViewModel viewModel;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private boolean dataChanged = false;

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
        showProgressDialog();
        viewModel.getHomeAllData().observe(getViewLifecycleOwner(), list -> {
            closeProgressDialog();
            showFragmentData();
        });

        //自定义的Adapter继承自FragmentPagerAdapter
        StateAdapter stateAdapter = new StateAdapter(getChildFragmentManager(),
                binding.tabLayout.getTabCount());
        // ViewPager设置Adapter
        binding.viewPager.setAdapter(stateAdapter);
        binding.viewPager.setOffscreenPageLimit(3);

        binding.tabLayout.setupWithViewPager(binding.viewPager);
        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener);
    }

    private TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            int currentTab = tab.getPosition();
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentTab != currentItem) {
                binding.viewPager.setCurrentItem(currentTab);
            }
            showFragmentData();
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding.tabLayout.removeOnTabSelectedListener(onTabSelectedListener);
        }
    }

    public class StateAdapter extends FragmentStatePagerAdapter {

        public StateAdapter(@NonNull FragmentManager fm, int count) {
            super(fm, count);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return createFragmentView(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 1) {
                return getString(R.string.main_tab_community);
            } else if (position == 2) {
                return getString(R.string.main_tab_personal);
            } else {
                return getString(R.string.main_tab_all);
            }
        }
    }

    private void showFragmentData() {
        int pos = binding.tabLayout.getSelectedTabPosition();
        FragmentManager fragmentManager = getChildFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        if (pos >= 0 && fragments!= null && pos < fragments.size()) {
            Fragment currentFragment = fragments.get(pos);
            if (currentFragment instanceof MainTabFragment) {
                ((MainTabFragment)currentFragment).showDataList(getDataList(pos));
            }
        }
    }

    private ArrayList<CommunityAndFriend> getDataList(int pos) {
        if (pos == 1) {
            return viewModel.getHomeCommunityData().getValue();
        } else if (pos == 2) {
            return viewModel.getHomeFriendData().getValue();
        } else {
            return viewModel.getHomeAllData().getValue();
        }
    }

    private MainTabFragment createFragmentView(int position) {
        closeProgressDialog();
        int pos = position < binding.tabLayout.getTabCount() ? position : 0;
        MainTabFragment tab = new MainTabFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(IntentExtra.BEAN, getDataList(pos));
        tab.setArguments(bundle);
        return tab;
    }
}