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
package io.taucbd.news.publishing.core.model.data.message;

import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.Constants;

/**
 * 交易类型
 */
public enum TxType {

    UNKNOWN(Constants.UNKNOWN_TX_TYPE, R.string.community_view_unknown),
    NOTE_TX(Constants.NOTE_TX_TYPE, R.string.community_view_note),
    NEWS_TX(Constants.NEWS_TX_TYPE, R.string.community_view_news),
    WIRING_TX(Constants.WIRING_TX_TYPE, R.string.community_view_wiring);

    private final int type;
    private final int name;
    TxType(int type, int name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public int getName() {
        return name;
    }

    public static TxType valueOf(int type) {
        TxType[] values = TxType.values();
        for (TxType value : values) {
            if (value.type == type) {
                return value;
            }
        }
        return TxType.UNKNOWN;
    }
}
