package io.taucoin.news.publishing.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.slf4j.LoggerFactory;

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
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.news.publishing.core.storage.RepositoryHelper;
import io.taucoin.news.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.AppUtil;
import io.taucoin.news.publishing.core.utils.DeviceUtils;
import io.taucoin.news.publishing.core.utils.ObservableUtil;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ViewUtils;
import io.taucoin.news.publishing.core.utils.bus.HomeAllData;
import io.taucoin.news.publishing.core.utils.bus.HomeCommunitiesData;
import io.taucoin.news.publishing.core.utils.bus.HomeFriendsData;
import io.taucoin.news.publishing.core.utils.bus.RxBus2;
import io.taucoin.news.publishing.databinding.FragmentMainBinding;
import io.taucoin.news.publishing.ui.BaseFragment;
import io.taucoin.news.publishing.ui.community.PasteLinkActivity;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.customviews.FragmentStatePagerAdapter;
import io.taucoin.news.publishing.ui.setting.TrafficTipsActivity;
import io.taucoin.news.publishing.ui.transaction.NewsTabFragment;

/**
 * 群组列表页面
 */
public class
MainFragment extends BaseFragment implements View.OnClickListener {

    private MainActivity activity;

    private FragmentMainBinding binding;
    private MainViewModel viewModel;
    private SettingsRepository settingsRepo;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean dataChanged = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);
        binding.setListener(this);
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
        StateAdapter stateAdapter = new StateAdapter(this.getChildFragmentManager(),
                binding.tabLayout.getTabCount());
        // ViewPager设置Adapter
        binding.viewPager.setAdapter(stateAdapter);
        binding.viewPager.setOffscreenPageLimit(3);
        binding.tabLayout.setupWithViewPager(binding.viewPager);

//        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(binding.tabLayout,
//                binding.viewPager, (tab, position) -> {
//                    if (position == 1) {
//                        tab.setText(getString(R.string.main_tab_community));
//                    } else if (position == 2) {
//                        tab.setText(getString(R.string.main_tab_personal));
//                    } else {
//                        tab.setText(getString(R.string.main_tab_all));
//                    }
//                });
//        tabLayoutMediator.attach();

        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener);

        for (int i = 0; i < binding.tabLayout.getTabCount(); i++ ) {
            TabLayout.Tab tab = binding.tabLayout.getTabAt(i);
            if (tab != null) {
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) tab.view.getLayoutParams();
                layoutParams.width = 0;
                if (i == 0) {
                    layoutParams.weight = 1.2f;
                } else if (i == 1) {
                    layoutParams.weight = 2.0f;
                } else {
                    layoutParams.weight = 1.5f;
                }
                tab.view.setLayoutParams(layoutParams);
            }
        }
        updateTabBadgeDrawable(0, true, false);

        binding.viewPager.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }

    private final TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {

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
                .subscribe(o -> {
                    dataChanged = true;
                }));

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

        disposables.add(viewModel.observeUnreadNews()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(msgUnread -> {
                    updateTabBadgeDrawable(0, false, msgUnread > 0);
                }));
    }

    private void handleWarningView() {
        binding.llWarning.setTag(false);
        binding.llWarning.setVisibility(View.VISIBLE);
        String interfacesKey = getString(R.string.pref_key_network_interfaces);
        if (!settingsRepo.internetState()) {
            binding.tvWarning.setText(getString(R.string.main_network_unavailable));
        } else if (StringUtil.isEquals(settingsRepo.getStringValue(interfacesKey, ""), "0.0.0.0")) {
            binding.tvWarning.setText(getString(R.string.main_no_ipv4));
        }
//        else if (!NetworkSetting.isHaveAvailableData()) {
//            binding.tvWarning.setText(getString(R.string.main_data_used_up));
//            binding.llWarning.setTag(true);
//        }
        else if (DeviceUtils.isSpaceInsufficient()) {
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
        }
//        else if (StringUtil.isEquals(key, getString(R.string.pref_key_metered_prompt_limit))) {
//            handleWarningView();
//        }
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

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.ll_warning) {
            boolean isShowTips = ViewUtils.getBooleanTag(view);
            if (isShowTips) {
                if (!AppUtil.isForeground(activity, TrafficTipsActivity.class)) {
                    Intent intent = new Intent(activity, TrafficTipsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                }
            }
        } else if (view.getId() == R.id.tv_paste_link) {
            ActivityUtil.startActivity(activity, PasteLinkActivity.class);
        }
    }

    private void updateTabBadgeDrawable(int index, boolean init, boolean visible) {
        TabLayout.Tab tab = binding.tabLayout.getTabAt(index);
        if (tab != null) {
            BadgeDrawable badgeDrawable = tab.getOrCreateBadge();
            if (init) {
                int badgeOffset = getResources().getDimensionPixelSize(R.dimen.widget_size_5);
                badgeDrawable.setHorizontalOffset(-badgeOffset);
                badgeDrawable.setVerticalOffset(badgeOffset);
                badgeDrawable.setBackgroundColor(getResources().getColor(R.color.color_red));
            }
            // 红点显示并且不在当前tab页
            badgeDrawable.setVisible(true);
            badgeDrawable.setVisible(visible && binding.tabLayout.getSelectedTabPosition() != index);
        }
    }

    public class StateAdapter extends FragmentStatePagerAdapter {

        private final int count;
        public StateAdapter(@NonNull FragmentManager fm, int count) {
            super(fm, count);
            this.count = count;
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

        @Override
        public int getCount() {
            return count;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return createFragmentView(position);
        }
    }

//    public class StateAdapter extends FragmentStateAdapter {
//
//        private final int count;
//        public StateAdapter(@NonNull Fragment fragment, int count) {
//            super(fragment);
//            this.count = count;
//        }
//
//        @NonNull
//        @Override
//        public Fragment createFragment(int position) {
//            return createFragmentView(position);
//        }
//
//        @Override
//        public int getItemCount() {
//            return count;
//        }
//    }

    private void showFragmentData() {
        int pos = binding.tabLayout.getSelectedTabPosition();
        FragmentManager fragmentManager = getChildFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        LoggerFactory.getLogger("showFragmentData").debug("fragments::{}", fragments.size());
        if (pos >= 0 && pos < fragments.size()) {
            for (Fragment fragment : fragments) {
                if (fragment instanceof MainTabFragment) {
                    MainTabFragment currentFragment = (MainTabFragment) fragment;
                    if (StringUtil.isEquals(currentFragment.getCustomTag(), String.valueOf(pos))) {
                        currentFragment.showDataList(getDataList(pos));
                    }
                }
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

    private Fragment createFragmentView(int position) {
        closeProgressDialog();
        int pos = position < binding.tabLayout.getTabCount() ? position : 0;
        if (pos == 0) {
            return new NewsTabFragment();
        }
        MainTabFragment tab = new MainTabFragment();
        Bundle bundle = new Bundle();
        bundle.putString(IntentExtra.CUSTOM_TAG, String.valueOf(pos));
//        bundle.putParcelableArrayList(IntentExtra.BEAN, getDataList(pos));
        tab.setArguments(bundle);
        if (pos == 1) {
            RxBus2.getInstance().postSticky(new HomeCommunitiesData(getDataList(pos)));
        } else if (pos == 2) {
            RxBus2.getInstance().postSticky(new HomeFriendsData(getDataList(pos)));
        } else {
            RxBus2.getInstance().postSticky(new HomeAllData(getDataList(pos)));
        }
        return tab;
    }
}