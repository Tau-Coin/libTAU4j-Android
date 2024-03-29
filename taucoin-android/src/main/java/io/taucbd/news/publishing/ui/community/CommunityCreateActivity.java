package io.taucbd.news.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Map;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.Constants;
import io.taucbd.news.publishing.core.utils.ActivityUtil;
import io.taucbd.news.publishing.core.utils.EditTextInhibitInput;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.core.utils.Utils;
import io.taucbd.news.publishing.core.utils.ViewUtils;
import io.taucbd.news.publishing.databinding.ActivityCommunityCreateBinding;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Community;
import io.taucbd.news.publishing.databinding.DialogCreateCommunitySuccessBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.customviews.CommonDialog;
import io.taucbd.news.publishing.ui.main.MainActivity;

/**
 * 群组/社区创建页面
 */
public class CommunityCreateActivity extends BaseActivity {
    private ActivityCommunityCreateBinding binding;
    private CommunityViewModel viewModel;
    private String chainID;
    private MembersAddFragment currentFragment;
    private CommonDialog successDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(CommunityViewModel.class);
        viewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_create);
        initLayout();
        observeAddCommunityState();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_new_community);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.tvCommunityCreateTips.setText(Html.fromHtml(getString(R.string.community_create_tips)));

        String totalCoin = getString(R.string.community_total_coins,
                FmtMicrometer.fmtBalance(Constants.TOTAL_COIN.longValue()));
        binding.tvTotalCoin.setText(totalCoin);
        // 社区名字禁止输入#特殊符号
        binding.etCommunityName.setFilters(new InputFilter[]{
                new EditTextInhibitInput(EditTextInhibitInput.COMMUNITY_NAME_REGEX)});
        binding.etCommunityName.addTextChangedListener(mTextWatcher);

        loadFragment();
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String communityName = s.toString();
            if (StringUtil.isEmpty(communityName)) {
                chainID = null;
            } else {
                chainID = viewModel.createNewChainID(communityName.trim());
            }
            String firstLetters = StringUtil.getFirstLettersOfName(s.toString());
            binding.roundButton.setText(firstLetters);
            int defaultColor = getResources().getColor(R.color.primary_light);
            int bgColor = StringUtil.isEmpty(chainID) ? defaultColor : Utils.getGroupColor(chainID);
            binding.roundButton.setBgColor(bgColor);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    /**
     * 观察添加社区的状态
     */
    private void observeAddCommunityState() {
        viewModel.getAddCommunityState().observe(this, state -> {
            if (state.isSuccess()) {
                openCommunityActivity(state.getMsg());
            } else {
                ToastUtils.showShortToast(state.getMsg());
            }
        });
    }

    /**
     * 打开社区页面
     */
    private void openCommunityActivity(String chainID) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.TYPE, 0);
        ActivityUtil.startActivity(intent, this, MainActivity.class);
    }

    @Deprecated
    private void showCreateSuccessDialog(String chainID) {
        DialogCreateCommunitySuccessBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.dialog_create_community_success, null, false);
        binding.tvTitle.setText(Html.fromHtml(getString(R.string.community_create_success)));
        binding.tvYes.setOnClickListener(v -> {
            // 进入社区页面
            openCommunityActivity(chainID);
        });
        successDialog = new CommonDialog.Builder(this)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        successDialog.show();
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 添加新社区处理事件
        if (item.getItemId() == R.id.menu_done) {
            createNewCommunity();
        }
        return true;
    }

    private void createNewCommunity() {
        String communityName = ViewUtils.getText(binding.etCommunityName);
        Community community = new Community(chainID, communityName);
        community.chainID = chainID;

        Map<String, String> selectedMap = null;
        if (currentFragment != null) {
            selectedMap = currentFragment.getSelectedMap();
        }
        if (viewModel.validateCommunity(community, selectedMap)) {
            viewModel.addCommunity(community, selectedMap);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTextWatcher != null) {
            binding.etCommunityName.removeTextChangedListener(mTextWatcher);
        }
        currentFragment = null;

        if (successDialog != null && successDialog.isShowing()) {
            successDialog.closeDialog();
        }
    }

    private void loadFragment() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isDestroyed()) {
            return;
        }
        currentFragment = new MembersAddFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(IntentExtra.TYPE, MembersAddFragment.PAGE_COMMUNITY_CREATION);
        bundle.putLong(IntentExtra.AIRDROP_COIN, Constants.CREATION_AIRDROP_COIN.longValue());
        currentFragment.setArguments(bundle);
        FragmentTransaction transaction = fm.beginTransaction();
        // Replace whatever is in the fragment container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.members_fragment, currentFragment);
        // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
        // transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }
}