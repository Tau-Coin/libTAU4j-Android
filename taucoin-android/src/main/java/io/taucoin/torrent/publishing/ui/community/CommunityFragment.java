package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UrlUtil;
import io.taucoin.torrent.publishing.databinding.ExternalAirdropLinkDialogBindingImpl;
import io.taucoin.torrent.publishing.databinding.FragmentCommunityBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.CommunityTabFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.transaction.QueueTabFragment;
import io.taucoin.torrent.publishing.ui.transaction.TxsTabFragment;

/**
 * 单个群组页面
 */
public class CommunityFragment extends BaseFragment implements View.OnClickListener {

    public static final int MEMBERS_REQUEST_CODE = 0x100;
    private MainActivity activity;
    private FragmentCommunityBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CommunityTabFragment currentTabFragment = null;
    private TextView selectedView = null;
    private String chainID;
    private boolean isReadOnly = true;
    private boolean isShowShareLink = false;
    private CommonDialog shareDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_community, container, false);
        binding.setListener(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (MainActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding.setListener(this);
        binding.toolbarInclude.setListener(this);
        initParameter();
        initLayout();
        onClick(binding.tvChainNote);
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if (getArguments() != null) {
            chainID = getArguments().getString(IntentExtra.ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {

        if (StringUtil.isNotEmpty(chainID)) {
            String communityName = ChainIDUtil.getName(chainID);
            binding.toolbarInclude.tvTitle.setText(Html.fromHtml(communityName));
            binding.toolbarInclude.tvSubtitle.setText(getString(R.string.community_users_stats, 0));
        }
        binding.toolbarInclude.ivBack.setOnClickListener(v -> activity.goBack());
        binding.toolbarInclude.tvSubtitle.setVisibility(View.VISIBLE);
        binding.toolbarInclude.ivAction.setVisibility(View.VISIBLE);
        binding.toolbarInclude.ivAction.setImageResource(R.mipmap.icon_community_detail);
        binding.toolbarInclude.ivAction.setOnClickListener(v -> {
            if(StringUtil.isEmpty(chainID)) {
                return;
            }
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
            intent.putExtra(IntentExtra.READ_ONLY, isReadOnly);
            ActivityUtil.startActivityForResult(intent, activity, CommunityDetailActivity.class, MEMBERS_REQUEST_CODE);
        });
    }

    /**
     * 加载Tab视图
     */
    private void loadTabView(int tab) {
        switch (tab) {
            case 0:
                // note
                currentTabFragment = new TxsTabFragment();
                Bundle bundle = new Bundle();
                bundle.putString(IntentExtra.CHAIN_ID, chainID);
                bundle.putBoolean(IntentExtra.READ_ONLY, isReadOnly);
                bundle.putInt(IntentExtra.TYPE, CommunityTabs.NOTE.getIndex());
                currentTabFragment.setArguments(bundle);
                break;
            case 1:
                // market
                currentTabFragment = new TxsTabFragment();
                bundle = new Bundle();
                bundle.putString(IntentExtra.CHAIN_ID, chainID);
                bundle.putBoolean(IntentExtra.READ_ONLY, isReadOnly);
                bundle.putInt(IntentExtra.TYPE, CommunityTabs.MARKET.getIndex());
                currentTabFragment.setArguments(bundle);
                break;
            case 2:
                // queue
                currentTabFragment = new TxsTabFragment();
                bundle = new Bundle();
                bundle.putString(IntentExtra.CHAIN_ID, chainID);
                bundle.putBoolean(IntentExtra.READ_ONLY, isReadOnly);
                bundle.putInt(IntentExtra.TYPE, CommunityTabs.CHAIN.getIndex());
                currentTabFragment.setArguments(bundle);
                break;
            case 3:
                // chain
                currentTabFragment = new QueueTabFragment();
                bundle = new Bundle();
                bundle.putString(IntentExtra.CHAIN_ID, chainID);
                bundle.putBoolean(IntentExtra.READ_ONLY, isReadOnly);
                currentTabFragment.setArguments(bundle);
                break;
        }
        replaceOrRemoveFragment(false);
    }

    private void replaceOrRemoveFragment(boolean isRemove) {
        if (currentTabFragment != null && activity != null) {
            FragmentManager fm = activity.getSupportFragmentManager();
            if (fm.isDestroyed()) {
                return;
            }
            FragmentTransaction transaction = fm.beginTransaction();
            if (!isRemove) {
                // Replace whatever is in the fragment container view with this fragment,
                // and add the transaction to the back stack
                transaction.replace(R.id.tab_fragment, currentTabFragment);
                // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
                // transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            } else {
                transaction.remove(currentTabFragment);
                transaction.commitAllowingStateLoss();
                currentTabFragment = null;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCommunityViewModel();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        replaceOrRemoveFragment(true);
        if (shareDialog != null && shareDialog.isShowing()) {
            shareDialog.closeDialog();
        }
    }

    /**
     * 订阅社区相关的被观察者
     */
    private void subscribeCommunityViewModel() {
        communityViewModel.getSetBlacklistState().observe(this, state -> {
            if(state){
                activity.goBack();
            }
        });
        disposables.add(communityViewModel.getMembersStatistics(chainID)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(statistics ->
                    binding.toolbarInclude.tvSubtitle.setText(getString(R.string.community_users_stats,
                            statistics.getMembers())))
        );

        disposables.add(communityViewModel.observerCurrentMember(chainID)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(member -> {
                    boolean isReadOnly = member.isReadOnly();
                    if (currentTabFragment != null) {
                        currentTabFragment.handleReadOnly(isReadOnly);
                        this.isReadOnly = isReadOnly;
                    }
                    binding.flJoin.setVisibility(member.isJoined() ? View.GONE : View.VISIBLE);
                }));

        // 获取3个社区成员的公钥
        if (isShowShareLink) {
            disposables.add(communityViewModel.getCommunityMembersLimit(chainID, Constants.AIRDROP_LINK_BS_LIMIT)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                    isShowShareLink = false;
                    if (StringUtil.isNotEmpty(chainID)) {
                        String airdropPeer = MainApplication.getInstance().getPublicKey();
                        String airdropLink = UrlUtil.encodeAirdropUrl(airdropPeer, chainID, list);
                        showShareLinkDialog(airdropLink);
                    }
                }));
        }
    }

    /**
     * 显示分享airdrop link dialog
     * @param airdropLink
     */
    private void showShareLinkDialog(String airdropLink) {
        CopyManager.copyText(airdropLink);
        ExternalAirdropLinkDialogBindingImpl dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.external_airdrop_link_dialog, null, false);
        dialogBinding.tvJoin.setVisibility(View.GONE);
        dialogBinding.tvPeer.setText(R.string.main_airdrop_link_share);
        dialogBinding.tvPeer.setTextColor(getResources().getColor(R.color.color_black));
        dialogBinding.tvCommunity.setText(airdropLink);
        dialogBinding.tvCommunity.setTextColor(getResources().getColor(R.color.color_blue_dark));
        dialogBinding.tvSkip.setOnClickListener(v -> {
            if (shareDialog != null) {
                shareDialog.closeDialog();
            }
        });
        shareDialog = new CommonDialog.Builder(activity)
                .setContentView(dialogBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        shareDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_chain_note:
            case R.id.tv_chain_market:
            case R.id.tv_chain_tx:
            case R.id.tv_tx_queue:
                // 避免同一页面多次刷新
                if (this.selectedView != null && selectedView.getId() == v.getId()) {
                    return;
                }
                if (this.selectedView != null) {
                    this.selectedView.setBackgroundResource(R.drawable.white_rect_round_bg_no_border);
                    this.selectedView.setTextColor(getResources().getColor(R.color.gray_dark));
                }
                TextView selectedView = (TextView) v;
                selectedView.setBackgroundResource(R.drawable.yellow_rect_round_border_small_radius);
                selectedView.setTextColor(getResources().getColor(R.color.color_yellow));
                this.selectedView = selectedView;
                loadTabView(StringUtil.getIntTag(selectedView));
                break;
            case R.id.tv_join:
                communityViewModel.joinCommunity(chainID);
                break;
        }
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onFragmentResult(requestCode, resultCode, data);
        if (currentTabFragment != null) {
            currentTabFragment.onFragmentResult(requestCode, resultCode, data);
        }

    }
}
