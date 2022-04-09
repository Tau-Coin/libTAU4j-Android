package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.EditTextInhibitInput;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityCreateBinding;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.databinding.DialogCreateCommunitySuccessBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.friends.FriendsActivity;
import io.taucoin.torrent.publishing.ui.main.MainActivity;

/**
 * 群组/社区创建页面
 */
public class CommunityCreateActivity extends BaseActivity implements View.OnClickListener {
    private static int ADD_MEMBERS_CODE = 0x01;
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
        binding.setListener(this);
        initLayout();
        observeAddCommunityState();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.main_new_community);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String totalCoin = getString(R.string.community_total_coins,
                FmtMicrometer.fmtBalance(Constants.TOTAL_COIN.longValue()));
        binding.tvTotalCoin.setText(totalCoin);
        // 社区名字禁止输入#特殊符号
        binding.etCommunityName.setFilters(new InputFilter[]{
                new EditTextInhibitInput(EditTextInhibitInput.NAME_REGEX)});
        binding.etCommunityName.addTextChangedListener(mTextWatcher);
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
                showCreateSuccessDialog(state.getMsg());
            } else {
                ToastUtils.showShortToast(state.getMsg());
            }
        });
    }

    private void showCreateSuccessDialog(String chainID) {
        DialogCreateCommunitySuccessBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.dialog_create_community_success, null, false);
        binding.tvTitle.setText(Html.fromHtml(getString(R.string.community_create_success)));
        binding.tvYes.setOnClickListener(v -> {
            // 进入社区页面
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(IntentExtra.TYPE, 0);
            ActivityUtil.startActivity(intent, this, MainActivity.class);
        });
        successDialog = new CommonDialog.Builder(this)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
                .setButtonWidth(R.dimen.widget_size_240)
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_add_members) {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.TYPE, FriendsActivity.PAGE_CREATION_ADD_MEMBERS);
            ActivityUtil.startActivityForResult(intent, this, FriendsActivity.class,
                    ADD_MEMBERS_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == ADD_MEMBERS_CODE && data != null) {
            loadFragment(data.getParcelableArrayListExtra(IntentExtra.BEAN));
        }
    }

    private void loadFragment(ArrayList<User> friends) {
        if (null == currentFragment) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.isDestroyed()) {
                return;
            }
            currentFragment = new MembersAddFragment();
            Bundle bundle = new Bundle();
            bundle.putLong(IntentExtra.AIRDROP_COIN, Constants.CREATION_AIRDROP_COIN.longValue());
            bundle.putParcelableArrayList(IntentExtra.BEAN, friends);
            currentFragment.setArguments(bundle);
            FragmentTransaction transaction = fm.beginTransaction();
            // Replace whatever is in the fragment container view with this fragment,
            // and add the transaction to the back stack
            transaction.replace(R.id.members_fragment, currentFragment);
            // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
            // transaction.addToBackStack(null);
            transaction.commitAllowingStateLoss();
        } else {
            currentFragment.updateData(friends);
        }
    }
}