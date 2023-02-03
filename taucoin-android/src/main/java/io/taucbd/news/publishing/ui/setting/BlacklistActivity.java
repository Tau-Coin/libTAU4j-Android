package io.taucbd.news.publishing.ui.setting;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Community;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.databinding.ActivityBlacklistBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.community.CommunityViewModel;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.user.UserViewModel;

/**
 * 设置页面
 */
public class BlacklistActivity extends BaseActivity implements BlackListAdapter.ClickListener {

    static final String TYPE_USERS = "users";
    static final String TYPE_COMMUNITIES = "communities";
    static final String TYPE_COMMUNITY_USERS = "communityUser";
    private ActivityBlacklistBinding binding;
    private BlackListAdapter adapter;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private String currentType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        communityViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_blacklist);
        initParameter();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            currentType = getIntent().getStringExtra(IntentExtra.TYPE);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        int titleRes = R.string.setting_blacklist_users;
        if (StringUtil.isEquals(currentType, TYPE_COMMUNITIES)) {
            titleRes = R.string.setting_blacklist_communities;
            communityViewModel.getCommunitiesInBlacklist();
            communityViewModel.getBlackList().observe(this, list -> {
                adapter.setCommunityList(list);
            });
        } else if (StringUtil.isEquals(currentType, TYPE_COMMUNITY_USERS)) {
            titleRes = R.string.setting_blacklist_community_users;
            userViewModel.getCommunityUsersInBlacklist();
            userViewModel.getBlackList().observe(this, list -> {
                adapter.setUserList(list);
            });
        } else {
            userViewModel.getUsersInBlacklist();
            userViewModel.getBlackList().observe(this, list -> {
                adapter.setUserList(list);
            });
        }
        binding.toolbarInclude.toolbar.setTitle(titleRes);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new BlackListAdapter(this, currentType);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.blacklist.setLayoutManager(layoutManager);
        // 设置adapter
        binding.blacklist.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onUnblock(int pos) {
        Parcelable item = adapter.getItemKey(pos);
        if (item instanceof Community) {
            communityViewModel.setCommunityBlacklist(((Community) item).chainID, false);
        } else if(item instanceof User) {
            if (StringUtil.isEquals(currentType, TYPE_COMMUNITY_USERS)) {
                userViewModel.setCommunityUserBlacklist(((User) item).publicKey, false);
            } else {
                userViewModel.setUserBlacklist(((User) item).publicKey, false);
            }
        }
        adapter.deleteItem(pos);
    }
}