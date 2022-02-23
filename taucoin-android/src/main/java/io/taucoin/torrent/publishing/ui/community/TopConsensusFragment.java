package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.libTAU4j.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.ConsensusInfo;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.databinding.FragmentMemberBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;

/**
 * 社区排名页面
 */
public class TopConsensusFragment extends BaseFragment implements TopConsensusAdapter.ClickListener {

    static final int TOP_CONSENSUS = 0x03;
    static final int TOP_TIP = 0x04;
    private static final Logger logger = LoggerFactory.getLogger("ChainTopFragment");
    private BaseActivity activity;
    private FragmentMemberBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private TopConsensusAdapter adapter;
    private TauDaemon tauDaemon;

    private String chainID;
    private int type;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_member, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (BaseActivity) getActivity();
        assert activity != null;
        ViewModelProvider provider = new ViewModelProvider(activity);
        communityViewModel = provider.get(CommunityViewModel.class);
        tauDaemon = TauDaemon.getInstance(activity);
        initParameter();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            chainID = getArguments().getString(IntentExtra.CHAIN_ID);
            type = getArguments().getInt(IntentExtra.TYPE, TOP_CONSENSUS);
        }
    }

    /**
     * 初始化视图
     */
    private void initView() {
        logger.debug("chainID::{}, type::{}", chainID, type);
        adapter = new TopConsensusAdapter(this, type);
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.recyclerView.setLayoutManager(layoutManager);

        binding.recyclerView.setItemAnimator(animator);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BaseActivity)
            activity = (BaseActivity)context;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (type == TOP_CONSENSUS) {
            disposables.add(communityViewModel.observerCommunityByChainID(chainID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(community -> {
                        Type type = new TypeToken<List<ConsensusInfo>>(){}.getType();
                        List<ConsensusInfo> list = new Gson().fromJson(community.topConsensus, type);
                        if (list != null) {
                            Collections.sort(list);
                            adapter.submitList(list);
                        }
                    }));
        } else {
            disposables.add(getTopTipBlock(chainID, 3)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(list -> adapter.submitList(list)));
        }
    }

    /**
     * 获取tip block列表
     * @param chainID 社区ID
     * @param topNum 返回的数目
     * @return Observable<List<Block>>
     */
    Observable<List<ConsensusInfo>> getTopTipBlock(String chainID, int topNum) {
        return Observable.create(emitter -> {
            try {
                List<Block> blocks = tauDaemon.getTopTipBlock(chainID, topNum);
                List<ConsensusInfo> list = new ArrayList<>();
                if (blocks != null && blocks.size() > 0) {
                    for (Block block : blocks) {
                        ConsensusInfo info = new ConsensusInfo(block.Hash(), block.getBlockNumber(),
                                block.getBlockNumber());
                        list.add(info);
                        if (list.size() == 3) {
                            break;
                        }
                    }
                }
                emitter.onNext(list);
            } catch (Exception e) {
                logger.error("getBlockByNumber error ", e);
            }
            emitter.onComplete();
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onItemClicked(String publicKey) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, publicKey);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }
}
