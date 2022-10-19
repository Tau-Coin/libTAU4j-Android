package io.taucoin.tauapp.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.CommunityAndMember;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.databinding.FragmentTxsTransactionsTabBinding;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;

/**
 * Transactions Tab页
 */
public class TransactionsTabFragment extends CommunityTabFragment {

    private FragmentTxsTransactionsTabBinding binding;
    private CommunityTabFragment currentFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_txs_transactions_tab, container, false);
        binding.setListener(this);
        return binding.getRoot();
    }

    /**
     * 初始化视图
     */
    @Override
    public void initView() {
        currentTab = TAB_CHAIN;
        initSpinner();
    }


    private void initSpinner() {
        binding.rlBottom.setVisibility(View.VISIBLE);
        int verticalOffset = -getResources().getDimensionPixelSize(R.dimen.widget_size_5);
        binding.viewSpinner.setDropDownVerticalOffset(verticalOffset);

        int[] spinnerItems = new int[] {R.string.community_view_blocks,
                R.string.community_view_own_txs,
                R.string.community_view_tx_history};

        SpinnerAdapter adapter = new SpinnerAdapter(activity, spinnerItems);
        binding.viewSpinner.setSelection(0);
        binding.viewSpinner.setAdapter(adapter);

        binding.viewSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showProgressDialog();
                loadSpinnerView(spinnerItems[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        loadSpinnerView(spinnerItems[0]);

//        binding.rlPayPeople.setVisibility(isJoined ? View.VISIBLE : View.GONE);
        binding.rlPayPeople.setVisibility(View.GONE);
        binding.rlPayPeople.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
            ActivityUtil.startActivityForResult(intent, activity, TransactionCreateActivity.class,
                    TX_REQUEST_CODE);
        });

        if (getArguments() != null) {
            boolean isEnterSentTransactions = getArguments().getBoolean(IntentExtra.IS_ENTER_SENT_TRANSACTIONS, false);
            if (isEnterSentTransactions) {
                binding.viewSpinner.setSelection(1);
            }
        }
    }

    private void loadSpinnerView(int item) {
        if (item == R.string.community_view_own_txs) {
            currentFragment = new QueueTabFragment();
        } else if (item == R.string.community_view_tx_history) {
            currentFragment = new ChainTabFragment();
        } else {
            currentFragment = new BlocksTabFragment();
        }
        Bundle bundle = new Bundle();
        bundle.putString(IntentExtra.CHAIN_ID, chainID);
        bundle.putBoolean(IntentExtra.IS_JOINED, isJoined);
        currentFragment.setArguments(bundle);

        replaceOrRemoveFragment(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        replaceOrRemoveFragment(true);
    }

    private void replaceOrRemoveFragment(boolean isRemove) {
        if (currentFragment != null && activity != null) {
            FragmentManager fm = getChildFragmentManager();
            if (fm.isDestroyed()) {
                return;
            }
            FragmentTransaction transaction = fm.beginTransaction();
            if (!isRemove) {
                // Replace whatever is in the fragment container view with this fragment,
                // and add the transaction to the back stack
                transaction.replace(R.id.tab_fragment, currentFragment);
                // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
                // transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            } else {
                transaction.remove(currentFragment);
                transaction.commitAllowingStateLoss();
                currentFragment = null;
            }
        }
    }

    @Override
    public void handleMember(CommunityAndMember member) {
        super.handleMember(member);
        if (null == binding) {
            return;
        }
        if (currentFragment != null) {
            currentFragment.handleMember(member);
        }
//        binding.rlPayPeople.setVisibility(isJoined ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onFragmentResult(requestCode, resultCode, data);
        if (currentFragment != null) {
            currentFragment.onFragmentResult(requestCode, resultCode, data);
        }
    }
}