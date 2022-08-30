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
package io.taucoin.tauapp.publishing.core.model.data.message;

/**
 * 交易类型
 */
public enum TxType {
    UNKNOWN(0),
    NOTE_TX(1),
    WIRING_TX(2),
    SELL_TX(3),
    TRUST_TX(4),
    AIRDROP_TX(5),
    ANNOUNCEMENT(6);

    private int type;
    TxType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
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