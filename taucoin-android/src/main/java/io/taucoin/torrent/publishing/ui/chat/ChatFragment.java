package io.taucoin.torrent.publishing.ui.chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.noober.menu.FloatMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.BuildConfig;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndLog;
import io.taucoin.torrent.publishing.core.model.data.FriendStatus;
import io.taucoin.torrent.publishing.core.model.data.OperationMenuItem;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.GeoUtils;
import io.taucoin.torrent.publishing.core.utils.KeyboardUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.FragmentChatBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.TauNotifier;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.torrent.publishing.ui.customviews.AutoLinkTextView;
import io.taucoin.torrent.publishing.ui.customviews.MsgLogsDialog;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.qrcode.UserQRCodeActivity;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;
import io.taucoin.torrent.publishing.core.model.data.message.MessageType;

/**
 * ????????????????????????
 */
public class ChatFragment extends BaseFragment implements View.OnClickListener,
    ChatListAdapter.ClickListener {

    private MainActivity activity;
    private static final Logger logger = LoggerFactory.getLogger("ChatFragment");
    private FragmentChatBinding binding;
    private ChatViewModel chatViewModel;
    private UserViewModel userViewModel;
    private ChatListAdapter adapter;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Disposable logsDisposable;
    private String friendPK;
    private Handler handler = new Handler();
    private MsgLogsDialog msgLogsDialog;
    private FloatMenu operationsMenu;

    private int currentPos = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false);
        binding.setListener(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (MainActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        chatViewModel = provider.get(ChatViewModel.class);
        chatViewModel.observeNeedStartDaemon();
        initParameter();
        initLayout();
        userViewModel.requestFriendInfo(friendPK);
        userViewModel.focusFriend(friendPK);
    }

    /**
     * ???????????????
     */
    private void initParameter() {
        if(getArguments() != null){
            friendPK = getArguments().getString(IntentExtra.ID);
        }
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean isEmpty = StringUtil.isEmpty(s);
            binding.tvSend.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            if (BuildConfig.DEBUG || BuildConfig.DISPLAY_DEBUG_VIEW) {
                binding.ivAdd.setVisibility(!isEmpty ? View.GONE : View.VISIBLE);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    /**
     * ???????????????
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initLayout() {
        binding.toolbarInclude.ivBack.setOnClickListener(v -> {
            KeyboardUtils.hideSoftInput(activity);
            activity.goBack();
        });
        binding.toolbarInclude.ivAction.setVisibility(View.INVISIBLE);
        binding.ivAdd.setVisibility(BuildConfig.DEBUG || BuildConfig.DISPLAY_DEBUG_VIEW ? View.VISIBLE : View.GONE);
        binding.etMessage.addTextChangedListener(textWatcher);
        binding.etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            showOrHideChatAddView(false);
            handler.postDelayed(handleUpdateAdapter, 200);
        });

        binding.etMessage.setOnClickListener(v -> {
            showOrHideChatAddView(false);
            handler.postDelayed(handleUpdateAdapter, 200);
        });

        binding.chatAdd.setVisibility(View.GONE);
        binding.chatAdd.setListener((title, icon) -> {
            if (R.string.common_debug_digit1 == title) {
                chatViewModel.sendBatchDebugDigitMessage(friendPK, 100);
            } else if (R.string.common_debug_digit2 == title) {
                chatViewModel.sendBatchDebugDigitMessage(friendPK, 1000);
            } else if (R.string.common_debug_str1== title) {
                chatViewModel.sendBatchDebugMessage(friendPK, 100, 10 * 1024);
            } else if (R.string.common_debug_str2 == title) {
                chatViewModel.sendBatchDebugMessage(friendPK, 1000, 1024);
            }
        });

        binding.refreshLayout.setOnRefreshListener(this);

        adapter = new ChatListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
//        layoutManager.setStackFromEnd(true);
        binding.msgList.setLayoutManager(layoutManager);
        binding.msgList.setItemAnimator(null);
        binding.msgList.setAdapter(adapter);
        binding.msgList.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                onClick(binding.msgList);
            }
            return false;
        });

    }

    private void updateFriendInfo(User friend) {
        if (friend != null) {
            String friendNickName = UsersUtil.getShowName(friend);
            binding.toolbarInclude.tvTitle.setText(friendNickName);
            adapter.setFriend(friend);
        }
        User currentUser = MainApplication.getInstance().getCurrentUser();
        String distance = null;
        if (currentUser != null) {
            if (friend != null && friend.longitude != 0 && friend.latitude != 0 &&
                    currentUser.longitude != 0 && currentUser.latitude != 0) {
                distance = GeoUtils.getDistanceStr(friend.longitude, friend.latitude,
                        currentUser.longitude, currentUser.latitude);
            }
        }
        boolean isShowSubtitle = StringUtil.isNotEmpty(distance);
        binding.toolbarInclude.tvSubtitle.setVisibility(isShowSubtitle ? View.VISIBLE : View.GONE);
        if (isShowSubtitle) {
            binding.toolbarInclude.tvSubtitle.setText(distance);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        showProgressDialog();
        loadData(0);
        subscribeChatViewModel();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
        // ?????????????????????????????????
        binding.etMessage.clearFocus();
        binding.chatAdd.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.etMessage.removeTextChangedListener(textWatcher);
        if (msgLogsDialog != null) {
            msgLogsDialog.closeDialog();
        }
        if (logsDisposable != null && !logsDisposable.isDisposed()) {
            logsDisposable.dispose();
        }
        handler.removeCallbacks(handlePullAdapter);
        handler.removeCallbacks(handleUpdateAdapter);

        chatViewModel.onCleared();
        userViewModel.onCleared();
        adapter.recycle();

        if (operationsMenu != null) {
            operationsMenu.setOnItemClickListener(null);
            operationsMenu.dismiss();
        }
    }

    private final Runnable handleUpdateAdapter = () -> {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.msgList.getLayoutManager();
        if (layoutManager != null) {
            int bottomPosition = adapter.getItemCount() - 1;
            // ???????????????
            logger.debug("handleUpdateAdapter scrollToPosition::{}", bottomPosition);
            layoutManager.scrollToPositionWithOffset(bottomPosition, Integer.MIN_VALUE);
        }
    };

    private final Runnable handlePullAdapter = () -> {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.msgList.getLayoutManager();
        if (layoutManager != null) {
            int bottomPosition = adapter.getItemCount() - 1;
            int position = bottomPosition - currentPos;
            layoutManager.scrollToPositionWithOffset(position, 0);
        }
    };

    /**
     * ?????????????????????????????????
     */
    private void subscribeChatViewModel() {
        disposables.add(userViewModel.observeFriend(friendPK)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(friend -> {
                    updateFriendInfo(friend.user);
                    binding.llBottomInput.setVisibility(friend.status != FriendStatus.DISCOVERED.getStatus()
                            ? View.VISIBLE : View.GONE);
                    binding.llShareQr.setVisibility(friend.status != FriendStatus.CONNECTED.getStatus()
                            ? View.VISIBLE : View.GONE);
                }));

        disposables.add(chatViewModel.observeDataSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    // ??????????????????????????????????????????
                    if (result != null && StringUtil.isNotEmpty(result.getMsg())
                            && result.getMsg().contains(friendPK)) {
                        binding.refreshLayout.setRefreshing(false);
                        binding.refreshLayout.setEnabled(false);
                        // ??????????????????
                        loadData(0);
                    }
                }));

        chatViewModel.observerChatMessages().observe(this, messages -> {
            List<ChatMsgAndLog> currentList = new ArrayList<>(messages);
            if (currentPos == 0) {
                adapter.submitList(currentList, handleUpdateAdapter);
            } else {
                currentList.addAll(adapter.getCurrentList());
                adapter.submitList(currentList, handlePullAdapter);
            }
            binding.refreshLayout.setRefreshing(false);
            binding.refreshLayout.setEnabled(messages.size() != 0 && messages.size() % Page.PAGE_SIZE == 0);

            userViewModel.clearMsgUnread(friendPK);
            logger.debug("messages.size::{}", messages.size());
            closeProgressDialog();
            TauNotifier.getInstance().cancelNotify(friendPK);
        });

        chatViewModel.getChatResult().observe(this, result -> {
            if (!result.isSuccess()) {
                ToastUtils.showShortToast(result.getMsg());
            }
        });

        chatViewModel.getResentResult().observe(this, result -> {
            if (!result.isSuccess()) {
                ToastUtils.showShortToast(R.string.chatting_resend_failed);
            } else {
                ToastUtils.showShortToast(R.string.tx_resend_successful);
                int pos = StringUtil.getIntString(result.getMsg());
                if (pos >= 0 && pos < adapter.getCurrentList().size()) {
                    adapter.notifyItemChanged(pos);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.msg_list:
                KeyboardUtils.hideSoftInput(activity);
                binding.chatAdd.setVisibility(View.GONE);
                break;
            case R.id.iv_add:
                showOrHideChatAddView(true);
                break;
            case R.id.tv_send:
                sendMessage();
                break;
            case R.id.ll_share_qr:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, UserQRCodeActivity.TYPE_QR_SHARE);
                ActivityUtil.startActivity(intent, activity, UserQRCodeActivity.class);
                break;
            default:
                break;
        }
    }

    /**
     * ????????????????????????
     */
    private void showOrHideChatAddView(boolean isShow) {
        if (isShow) {
            KeyboardUtils.hideSoftInput(activity);
        }
        disposables.add(Observable.timer(10, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(aLong -> {
                    binding.chatAdd.setVisibility(isShow ? View.VISIBLE : View.GONE);
                    handler.post(handleUpdateAdapter);
                }));
    }

    /**
     * ??????chat??????
     */
    private void sendMessage() {
        String message = ViewUtils.getText(binding.etMessage);
        chatViewModel.sendMessage(friendPK, message, MessageType.TEXT.getType());
        binding.etMessage.getText().clear();
    }

    @Override
    public void onItemClicked(ChatMsg msg) {
        KeyboardUtils.hideSoftInput(activity);
    }

    @Override
    public void onMsgLogsClicked(ChatMsgAndLog msg) {
        KeyboardUtils.hideSoftInput(activity);
        if (logsDisposable != null) {
            disposables.remove(logsDisposable);
        }
        logsDisposable = chatViewModel.observerMsgLogs(msg.hash)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(logs -> showMsgLogsDialog(logs, msg));
        disposables.add(logsDisposable);
    }

    @Override
    public void onUserClicked(ChatMsg msg) {
        KeyboardUtils.hideSoftInput(activity);
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, msg.senderPk);
        intent.putExtra(IntentExtra.TYPE, UserDetailActivity.TYPE_CHAT_PAGE);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

    @Override
    public void onLongClick(AutoLinkTextView view) {
        KeyboardUtils.hideSoftInput(activity);
        List<OperationMenuItem> menuList = new ArrayList<>();
        menuList.add(new OperationMenuItem(R.string.tx_operation_copy));
        final URLSpan[] urls = view.getUrls();
        if (urls != null && urls.length > 0) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_copy_link));
        }
        operationsMenu = new FloatMenu(activity);
        operationsMenu.items(menuList);
        operationsMenu.setOnItemClickListener((v, position) -> {
            OperationMenuItem item = menuList.get(position);
            int resId = item.getResId();
            switch (resId) {
                case R.string.tx_operation_copy:
                    CopyManager.copyText(view.getText());
                    ToastUtils.showShortToast(R.string.copy_successfully);
                    break;
                case R.string.tx_operation_copy_link:
                    if (urls != null && urls.length > 0) {
                        String link = urls[0].getURL();
                        CopyManager.copyText(link);
                        ToastUtils.showShortToast(R.string.copy_link_successfully);
                    }
                    break;
            }
        });
        operationsMenu.show(activity.getPoint());
    }

    @Override
    public void onLinkClick(String link) {
        KeyboardUtils.hideSoftInput(activity);
        ActivityUtil.openUri(activity, link);
    }

    /**
     * ?????????????????????
     */
    private void showMsgLogsDialog(List<ChatMsgLog> logs, final ChatMsgAndLog msg) {
        if (msgLogsDialog != null && msgLogsDialog.isShowing()) {
            msgLogsDialog.submitList(logs);
            return;
        }
        msgLogsDialog = new MsgLogsDialog.Builder(activity)
                .setYourself(StringUtil.isEquals(msg.senderPk, msg.receiverPk))
                .setMsgLogsListener(new MsgLogsDialog.MsgLogsListener() {
                    @Override
                    public void onRetry() {
                        chatViewModel.resendMessage(msg, adapter.getCurrentList().indexOf(msg));
                    }

                    @Override
                    public void onCancel() {
                        if (logsDisposable != null) {
                            disposables.remove(logsDisposable);
                        }
                    }
                }).create();
        msgLogsDialog.submitList(logs);
        msgLogsDialog.show();
    }

    @Override
    public void onRefresh() {
        loadData(adapter.getItemCount());
    }

    private void loadData(int pos) {
        currentPos = pos;
        chatViewModel.loadMessagesData(friendPK, pos);
    }
}