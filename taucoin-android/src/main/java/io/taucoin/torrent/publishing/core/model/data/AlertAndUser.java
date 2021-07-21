/*
 * Copyright (C) 2016 Yaroslav Pronin <proninyaroslav@mail.ru>
 *
 * This file is part of LibreTorrent.
 *
 * LibreTorrent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibreTorrent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibreTorrent.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.taucoin.torrent.publishing.core.model.data;

import org.libTAU4j.Ed25519;
import org.libTAU4j.Pair;
import org.libTAU4j.alerts.Alert;

import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.util.ByteUtil;

/**
 * libTAU上报的Alert
 * 再次包装了用户的公钥，防止切换私钥造成的数据错乱
 */
public class AlertAndUser {
    private Alert alert;
    private String userPk;
    private String message;

    public AlertAndUser(Alert alert, String seed) {
        this.alert = alert;
        this.message = alert.message();

        if (StringUtil.isNotEmpty(seed)) {
            byte[] seedBytes = ByteUtil.toByte(seed);
            Pair<byte[], byte[]> keypair = Ed25519.createKeypair(seedBytes);
            this.userPk = ByteUtil.toHexString(keypair.first);
        }
    }

    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public String getUserPk() {
        return userPk;
    }

    public void setUserPk(String userPk) {
        this.userPk = userPk;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
