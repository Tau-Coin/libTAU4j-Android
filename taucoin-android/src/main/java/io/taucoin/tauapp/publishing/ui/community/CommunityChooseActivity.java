package io.taucoin.tauapp.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.databinding.ActivityCommunityChoiceBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;

/**
 * 社区选择页面
 */
public class CommunityChooseActivity extends BaseActivity {

    private ActivityCommunityChoiceBinding binding;
    private CommunityViewModel communityViewModel;
    private CommunityListAdapter adapter;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_choice);
        initLayout();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.common_choice);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new CommunityListAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
//        binding.joinedList.setOnItemClickListener((view, adapterPosition) -> {
//            // 选择社区退出返回数据
//            Member member = adapter.getCurrentList().get(adapterPosition);
//            Intent intent = new Intent();
//            intent.putExtra(IntentExtra.BALANCE, member.balance);
//            intent.putExtra(IntentExtra.CHAIN_ID, member.chainID);
//        });
        binding.joinedList.setAdapter(adapter);
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
        if (item.getItemId() == R.id.menu_done) {
            Member member = adapter.getMember();
            if (member != null) {
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, member.chainID);
                setResult(RESULT_OK, intent);
                this.finish();
            } else {
                ToastUtils.showShortToast(R.string.tx_community_select);
            }
        }
        return true;
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}
