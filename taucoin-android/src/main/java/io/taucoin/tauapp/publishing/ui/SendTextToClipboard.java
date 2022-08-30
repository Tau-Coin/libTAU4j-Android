package io.taucoin.tauapp.publishing.ui;

import android.content.Intent;
import android.os.Bundle;

import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.utils.CopyManager;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;

/*
 * Adds "Copy" item in share dialog.
 */

public class SendTextToClipboard extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setIsFullScreen(false);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            CopyManager.copyText(text);
            ToastUtils.showShortToast(R.string.copy_successfully);
        }

        finish();
        overridePendingTransition(0, 0);
    }
}
