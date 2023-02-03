/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucbd.news.publishing.core.utils;


import android.text.InputFilter;
import android.text.Spanned;

import io.taucbd.news.publishing.R;

/**
 * Bytes Limit Filter
 *
 */
public class BytesLimitFilter implements InputFilter {
//    static final Logger logger = LoggerFactory.getLogger("BytesLimitFilter");
    private int maxBytes = 0;
    public BytesLimitFilter(int maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dStart, int dEnd) {
//        logger.debug("source::{}, start::{}, end::{}, len::{}", source.toString(), start, end, end - start);
//        logger.debug("dest::{}, dStart::{}, dEnd::{}, len::{}", dest.toString(), dStart, dEnd, dEnd - dStart);
        int len = end - start;
        // if deleting, source is empty
        // and deleting can't break anything
        if (len == 0) {
            return source;
        }
        byte[] destBytes = Utils.textStringToBytes(dest.toString());
        int destLength = destBytes != null ? destBytes.length : 0;
        if (destLength >= maxBytes) {
//            logger.debug("1**** maxBytes::{}, destLength::{}", maxBytes, destLength);
            return "";
        }
        byte[] sourceBytes = Utils.textStringToBytes(source.toString());
        int sourceLength = sourceBytes != null ? sourceBytes.length : 0;
        if (destLength + sourceLength <= maxBytes) {
//            logger.debug("2**** maxBytes::{}, destLength::{}", maxBytes, destLength);
            return null;
        }
        int needBytes = maxBytes - destLength;
//        logger.debug("maxBytes::{}, destLength::{}, needBytes::{}", maxBytes, destLength, needBytes);
        if (source.toString().length() > 0) {
            CharSequence sourceSplitCopy = "";
            for ( int i = 1; i <= source.toString().length(); i++) {
                CharSequence sourceSplit = source.subSequence(0, i);
                byte[] sourceSplitBytes = Utils.textStringToBytes(sourceSplit.toString());
                if (sourceSplitBytes != null && sourceSplitBytes.length > needBytes) {
                    return sourceSplitCopy;
                }
                sourceSplitCopy = sourceSplit;
            }
        }
        return null;
    }
}