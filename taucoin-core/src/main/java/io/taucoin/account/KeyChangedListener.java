package io.taucoin.account;

import org.libTAU4j.Pair;

/**
 * KeyChangedListener is the event listener of wallet key changed.
 * KeyChangedListener implementation can be registered by AccountManager.
 */
public interface KeyChangedListener {

    void onKeyChanged(Pair<byte[], byte[]> newKey);
}
