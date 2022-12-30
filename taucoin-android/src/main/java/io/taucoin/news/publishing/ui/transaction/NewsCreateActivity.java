package io.taucoin.news.publishing.ui.transaction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.Constants;
import io.taucoin.news.publishing.core.model.TauDaemon;
import io.taucoin.news.publishing.core.model.data.message.NewsContent;
import io.taucoin.news.publishing.core.model.data.message.TxType;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.news.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.news.publishing.core.utils.ChainIDUtil;
import io.taucoin.news.publishing.core.utils.FmtMicrometer;
import io.taucoin.news.publishing.core.utils.ObservableUtil;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.Utils;
import io.taucoin.news.publishing.core.utils.ViewUtils;
import io.taucoin.news.publishing.databinding.ActivityNewsBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.community.CommunityListAdapter;
import io.taucoin.news.publishing.ui.community.CommunityViewModel;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.customviews.CommunitiesPopUpDialog;

/**
 * 发布News页面
 */
public class NewsCreateActivity extends BaseActivity implements View.OnClickListener {

    private ActivityNewsBinding binding;
    private TxViewModel txViewModel;
    private CommunityListAdapter adapter;
    private CommunityViewModel communityViewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final List<Member> communityList = new ArrayList<>();
    private CommunitiesPopUpDialog popUpDialog;
    private String chainID;
    private String repliedHash;
    private String repliedKey;
    private TxQueue txQueue;
    private CharSequence msg;
    private String link;
    private int onlinePeers = -1;
    private boolean isReteitt = false;
    private boolean isShowCommunities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_news);
        binding.setListener(this);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if (getIntent() != null) {
            // 回复news
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
            repliedHash = getIntent().getStringExtra(IntentExtra.HASH);
            repliedKey = getIntent().getStringExtra(IntentExtra.PUBLIC_KEY);
            txQueue = getIntent().getParcelableExtra(IntentExtra.BEAN);
            msg = getIntent().getCharSequenceExtra(IntentExtra.DATA);
            link = getIntent().getStringExtra(IntentExtra.LINK);
        }
    }

    /**
     * 初始化布局
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initLayout() {
        binding.tvBytesCount.setText("0/" + Constants.NEWS_TX_MAX_BYTE_SIZE);
        binding.tvBytesCount.setTextColor(getResources().getColor(R.color.color_gray_dark));
        binding.etNews.addTextChangedListener(newsWatcher);
        binding.etLink.addTextChangedListener(linkWatcher);

        binding.ivCancel.setOnClickListener(v -> onBackPressed());

        if (txQueue != null) {
            this.chainID = txQueue.chainID;
            NewsContent txContent = new NewsContent(txQueue.content);
            binding.etNews.setText(txContent.getMemo());
            binding.etNews.setSelection(binding.etNews.getText().length());
            binding.etNews.setEnabled(false);

            binding.etLink.setText(txContent.getLinkStr());
            binding.etLink.setSelection(binding.etLink.getText().length());
            binding.etLink.setEnabled(false);
            isReteitt = txQueue.queueType == 3;
        } else if (StringUtil.isNotEmpty(msg)) {
            binding.etLink.setText(link);
            binding.etNews.setText(msg.toString());
            binding.etNews.setSelection(binding.etNews.getText().length());
            isReteitt = true;
        }
        if (isReteitt) {
            binding.tvPost.setText(R.string.common_retweet);
        }

        adapter = new CommunityListAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
        binding.joinedList.setOnItemClickListener((view, adapterPosition) -> {
            Member member = adapter.getCurrentList().get(adapterPosition);
            this.chainID = member.chainID;
            enabledOrDisabledPost();
            adapter.setChainID(member.chainID);
            List<Member> list = new ArrayList<>(adapter.getCurrentList());
            list.remove(member);
            list.add(0, member);
            adapter.submitList(list);
            adapter.notifyDataSetChanged();
            loadAverageTxFee();
        });
        binding.joinedList.setAdapter(adapter);

        isShowCommunities = StringUtil.isEmpty(chainID);
        binding.llCommunities.setVisibility(isShowCommunities ? View.VISIBLE : View.GONE);
        binding.tvInterimBalance.setVisibility(!isShowCommunities ? View.VISIBLE : View.GONE);
        if (StringUtil.isNotEmpty(repliedHash)) {
            binding.etNews.setHint(R.string.tx_your_reply);
        }
        loadInterimBalanceView(0);
    }

    private final TextWatcher newsWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            enabledOrDisabledPost();
            loadBytesCount();
        }

        @Override
        public void afterTextChanged(Editable s) {
            int lines = binding.etNews.getLineCount();
            // 限制最大输入行数
            if (lines > 20) {
                String str = s.toString();
                int cursorStart = binding.etNews.getSelectionStart();
                int cursorEnd = binding.etNews.getSelectionEnd();
                if (cursorStart == cursorEnd && cursorStart < str.length() && cursorStart >= 1) {
                    str = str.substring(0, cursorStart - 1) + str.substring(cursorStart);
                } else {
                    str = str.substring(0, s.length() - 1);
                }
                // setText会触发afterTextChanged的递归
                binding.etNews.setText(str);
                // setSelection用的索引不能使用str.length()否则会越界
                binding.etNews.setSelection(binding.etNews.getText().length());
                ToastUtils.showShortToast(R.string.tx_news_lines_limit);
            }
        }
    };

    private void enabledOrDisabledPost() {
        boolean isEmpty = StringUtil.isEmpty(ViewUtils.getText(binding.etNews));
        binding.tvPost.setEnabled(!isEmpty && StringUtil.isNotEmpty(chainID));
    }

    private final TextWatcher linkWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            loadBytesCount();
        }

        @Override
        public void afterTextChanged(Editable s) {
            int lines = binding.etLink.getLineCount();
            // 限制最大输入行数
            if (lines > 5) {
                String str = s.toString();
                int cursorStart = binding.etLink.getSelectionStart();
                int cursorEnd = binding.etLink.getSelectionEnd();
                if (cursorStart == cursorEnd && cursorStart < str.length() && cursorStart >= 1) {
                    str = str.substring(0, cursorStart - 1) + str.substring(cursorStart);
                } else {
                    str = str.substring(0, s.length() - 1);
                }
                // setText会触发afterTextChanged的递归
                binding.etLink.setText(str);
                // setSelection用的索引不能使用str.length()否则会越界
                binding.etLink.setSelection(binding.etLink.getText().length());
                ToastUtils.showShortToast(R.string.tx_link_lines_limit);
            }
        }
    };

    private void loadBytesCount() {
        String news = ViewUtils.getText(binding.etNews);
        String link = ViewUtils.getText(binding.etLink);
        byte[] newsBytes = Utils.textStringToBytes(news);
        byte[] linkBytes = Utils.textStringToBytes(link);
        int bytesCount = newsBytes != null ? newsBytes.length : 0;
        bytesCount += linkBytes != null ? linkBytes.length : 0;
        binding.tvBytesCount.setText(bytesCount + "/" + Constants.NEWS_TX_MAX_BYTE_SIZE);
        binding.tvBytesCount.setTextColor(getResources().getColor(bytesCount > 0 ? R.color.color_black :
                R.color.color_gray_dark));
    }

    private void loadFeeView(long averageTxFee) {
        long txFee = 0L;
        if (txQueue != null) {
            txFee = txQueue.fee;
        }
        String txFeeStr = FmtMicrometer.fmtFeeValue(txFee > 0 ? txFee : averageTxFee);
        binding.tvFee.setTag(R.id.median_fee, averageTxFee);

        String txFreeHtml = getString(R.string.tx_median_fee, txFeeStr,
                ChainIDUtil.getCoinName(chainID));
        binding.tvFee.setText(Html.fromHtml(txFreeHtml));
        binding.tvFee.setTag(txFeeStr);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (StringUtil.isNotEmpty(chainID)) {
            TauDaemon tauDaemon = TauDaemon.getInstance(getApplicationContext());
            disposables.add(ObservableUtil.intervalSeconds(2, true)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(l -> {
                        List<String> activeList = tauDaemon.getActiveList(chainID);
                        int activeListSize = activeList != null ? activeList.size() : 0;
                        if (onlinePeers != activeListSize) {
                            onlinePeers = activeListSize;
                            showOrHideLowLinkedView(activeListSize < 3);
                        }
                    }));
        }

        txViewModel.getAddState().observe(this, result -> {
            if (StringUtil.isNotEmpty(result)) {
                ToastUtils.showShortToast(result);
            } else {
                setResult(RESULT_OK);
                onBackPressed();
            }
        });

        if (StringUtil.isEmpty(chainID)) {
            disposables.add(communityViewModel.observerJoinedCommunityList()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(members -> {
                        if (communityList.size() > 0) {
                            return;
                        }
                        if (members != null) {
                            communityList.addAll(members);
                            List<Member> list = new ArrayList<>();
                            for (Member member : members) {
                                if (list.size() < 2) {
                                    list.add(member);
                                }
                            }
                            if (adapter != null) {
                                adapter.submitList(list);
                            }
                            binding.tvMore.setVisibility(communityList.size() > 2 ? View.VISIBLE : View.GONE);
                            binding.joinedList.setVisibility(communityList.size() > 0 ? View.VISIBLE : View.GONE);
                        }
                    }));
        } else {
            disposables.add(communityViewModel.observerCurrentMember(chainID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(member -> {
                        long balance = ViewUtils.getLongTag(binding.tvInterimBalance);
                        if (member != null && member.getInterimBalance() != balance) {
                            loadInterimBalanceView(member.getInterimBalance());
                        }
                    }, it -> {
                        loadInterimBalanceView(0);
                    }));
        }
        loadAverageTxFee();
    }

    private void loadAverageTxFee() {
        boolean isReply = StringUtil.isNotEmpty(repliedHash);
        disposables.add(txViewModel.observeAverageTxFee(chainID, TxType.NEWS_TX, isReteitt, isReply)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadFeeView));
    }

    public void showOrHideLowLinkedView(boolean show) {
        if (null == binding) {
            return;
        }
        binding.llLowLinked.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void loadInterimBalanceView(long interimBalance) {
        binding.tvInterimBalance.setText(getString(R.string.tx_interim_balance,
                FmtMicrometer.fmtLong(interimBalance),
                ChainIDUtil.getCoinName(chainID)));
        binding.tvInterimBalance.setTag(interimBalance);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.etNews.removeTextChangedListener(newsWatcher);
        binding.etLink.removeTextChangedListener(linkWatcher);
        if (popUpDialog != null && popUpDialog.isShowing()) {
            popUpDialog.closeDialog();
        }
    }

    /**
     * 构建交易数据
     * @return Tx
     */
    private TxQueue buildTx() {
        String senderPk = MainApplication.getInstance().getPublicKey();
        String fee = ViewUtils.getStringTag(binding.tvFee);
        String news = ViewUtils.getText(binding.etNews);
        String link = ViewUtils.getText(binding.etLink);
        NewsContent content = new NewsContent(news, link, repliedHash, repliedKey);
        TxQueue tx = new TxQueue(chainID, senderPk, senderPk, 0L,
                FmtMicrometer.fmtTxLongValue(fee), TxType.NEWS_TX, content.getEncoded());
        tx.queueType = isReteitt ? 3 : 0;
        if (txQueue != null) {
            tx.queueID = txQueue.queueID;
            tx.queueTime = txQueue.queueTime;
        }
        return tx;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_fee:
                txViewModel.showEditFeeDialog(this, binding.tvFee, chainID);
                break;
            case R.id.tv_post:
                TxQueue tx = buildTx();
                long interimBalance = 0;
                if (isShowCommunities) {
                    List<Member> currentList = adapter.getCurrentList();
                    if (currentList.size() > 0) {
                        interimBalance = currentList.get(0).getInterimBalance();
                    }
                } else {
                    interimBalance = ViewUtils.getLongTag(binding.tvInterimBalance);
                }
                if (txViewModel.validateTx(tx, interimBalance)) {
                    txViewModel.addTransaction(tx, txQueue);
                }
                break;
            case R.id.tv_more:
                showCommunityPopWindow();
                break;
        }
    }

    private void showCommunityPopWindow() {
        if (popUpDialog != null && popUpDialog.isShowing()) {
            return;
        }
        popUpDialog = new CommunitiesPopUpDialog.Builder(this)
                .addItems(communityList)
                .setOnItemClickListener((dialog, member) -> {
                    dialog.dismiss();
                    List<Member> currentList = adapter.getCurrentList();
                    if (currentList.size() > 0) {
                        Member firstMember = currentList.get(0);
                        if (StringUtil.isNotEquals(member.chainID, firstMember.chainID)) {
                            List<Member> list = new ArrayList<>();
                            list.add(member);
                            list.add(firstMember);
                            adapter.submitList(list);
                        }
                    }
                    this.chainID = member.chainID;
                    enabledOrDisabledPost();
                    adapter.setChainID(member.chainID);
                    adapter.notifyDataSetChanged();
                    loadAverageTxFee();
                }).create();
        popUpDialog.show();
    }
}
