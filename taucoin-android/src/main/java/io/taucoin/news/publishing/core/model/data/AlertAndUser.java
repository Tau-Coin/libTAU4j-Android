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

package io.taucoin.news.publishing.core.model.data;

import org.libTAU4j.alerts.Alert;

import io.taucoin.news.publishing.MainApplication;

/**
 * libTAU上报的Alert
 * 再次包装了用户的公钥，防止切换私钥造成的数据错乱
 */
public class AlertAndUser {
    private Alert alert;
    private String userPk;

    public AlertAndUser(Alert alert) {
        this.alert = alert;
        this.userPk = MainApplication.getInstance().getPublicKey();
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
}
