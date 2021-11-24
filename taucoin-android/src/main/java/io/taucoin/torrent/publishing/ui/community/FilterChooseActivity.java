package io.taucoin.torrent.publishing.ui.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityChooseBinding;
import io.taucoin.torrent.publishing.databinding.ItemFilterChooseBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * 群组过滤选择页面
 */
public class FilterChooseActivity extends BaseActivity implements FilterListAdapter.Callback {

    private ActivityCommunityChooseBinding binding;
    private ItemFilterChooseBinding filterBinding;
    private FilterListAdapter adapter;
    private SettingsRepository settingsRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_choose);
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        initLayout();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_filter_by);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        Set<String> selectedSet = settingsRepo.getFiltersSelected();
        Set<String> filters = CommunityTabs.getIndexSet();
        if (null == selectedSet) {
            selectedSet = new HashSet<>(filters);
        }
        adapter = new FilterListAdapter(this, selectedSet);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
        binding.joinedList.setAdapter(adapter);
        adapter.submitList(new ArrayList<>(filters));

        LayoutInflater inflater = LayoutInflater.from(this);
        filterBinding = DataBindingUtil.inflate(inflater,
                R.layout.item_filter_choose,
                null,
                false);
        filterBinding.cbSelect.setText(R.string.community_filter_all);
        onCheckedChangeListener();
        Set<String> finalSelectedSet = selectedSet;
        filterBinding.cbSelect.setOnClickListener(v -> {
            if (filterBinding.cbSelect.isChecked()) {
                finalSelectedSet.addAll(new ArrayList<>(filters));
            } else {
                finalSelectedSet.clear();
            }
            adapter.notifyDataSetChanged();
        });
        binding.joinedList.addHeaderView(filterBinding.getRoot());
    }


    @Override
    public void onCheckedChangeListener() {
        if (null == filterBinding) {
            return;
        }
        boolean isAll = adapter.getSelectedSet().size() == adapter.getCurrentList().size();
        filterBinding.cbSelect.setChecked(isAll);
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
           settingsRepo.setFiltersSelected(adapter.getSelectedSet());
           finish();
        }
        return true;
    }
}