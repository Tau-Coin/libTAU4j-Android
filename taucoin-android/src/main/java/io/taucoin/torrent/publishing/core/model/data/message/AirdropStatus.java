/**
Copyright 2020 taucoin developer

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
OR OTHER DEALINGS IN THE SOFTWARE.
*/
package io.taucoin.torrent.publishing.core.model.data.message;

import io.taucoin.torrent.publishing.R;

/**
 * 发币开关状态
 */
public enum AirdropStatus {
    SETUP(0, R.string.common_setup),
    ON(1, R.string.common_on);

    private int status;
    private int name;
    AirdropStatus(int status, int name) {
        this.status = status;
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public int getName() {
        return name;
    }

    public static AirdropStatus valueOf(int status) {
        AirdropStatus[] values = AirdropStatus.values();
        for (AirdropStatus value : values) {
            if (value.status == status) {
                return value;
            }
        }
        return AirdropStatus.SETUP;
    }
}