package io.taucoin.tauapp.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.message.TxType;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.core.utils.ViewUtils;
import io.taucoin.tauapp.publishing.databinding.ActivityCommunityChoiceBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.main.MainActivity;
import io.taucoin.tauapp.publishing.ui.transaction.NewsCreateActivity;
import io.taucoin.tauapp.publishing.ui.transaction.TxViewModel;

/**
 * 社区选择页面
 */
public class CommunityChooseActivity extends BaseActivity {

    public static final int TYPE_SELECT_COMMUNITY_EXIT = 0x01;
    public static final int TYPE_CREATE_NEWS = 0x02;
    public static final int TYPE_RETWEET_NEWS = 0x03;
    private ActivityCommunityChoiceBinding binding;
    private CommunityViewModel communityViewModel;
    private TxViewModel txViewModel;
    private CommunityListAdapter adapter;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private int type = TYPE_SELECT_COMMUNITY_EXIT;
    private CharSequence msg;
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_choice);
        if (getIntent() != null) {
            type = getIntent().getIntExtra(IntentExtra.TYPE, TYPE_SELECT_COMMUNITY_EXIT);
            msg = getIntent().getCharSequenceExtra(IntentExtra.DATA);
        }
        initLayout();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_select);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new CommunityListAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
        binding.joinedList.setOnItemClickListener((view, adapterPosition) -> {
            // 选择社区退出返回数据
            Member member = adapter.getCurrentList().get(adapterPosition);
            this.chainID = member.chainID;
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, member.chainID);
            if (type == TYPE_CREATE_NEWS) {
                ActivityUtil.startActivity(intent, this, NewsCreateActivity.class);
                this.finish();
            } else if (type == TYPE_RETWEET_NEWS) {
                intent.putExtra(IntentExtra.DATA, msg);
                ActivityUtil.startActivity(intent, this, NewsCreateActivity.class);
                this.finish();
            } else {
                setResult(RESULT_OK, intent);
                this.finish();
            }
        });
        binding.joinedList.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        disposables.add(communityViewModel.observerJoinedCommunityList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(members -> {
                    if (adapter != null) {
                        adapter.submitList(members);
                    }
                }));

        txViewModel.getAddState().observe(this, result -> {
            if (StringUtil.isNotEmpty(result)) {
                ToastUtils.showShortToast(result);
            } else {
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(IntentExtra.TYPE, 0);
                ActivityUtil.startActivity(intent, this, MainActivity.class);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}
