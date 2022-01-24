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
package io.taucoin.torrent.publishing.core.utils;


import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.DigitsKeyListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.torrent.publishing.R;

/**
 * Chinese Filter
 *
 */
public class ChineseFilter implements InputFilter {
//    static final Logger logger = LoggerFactory.getLogger("ChineseFilter");
    public ChineseFilter() {
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
        if (Utils.isChinese(source.toString())) {
            ToastUtils.showShortToast(R.string.common_chinese_not_supported);
            return "";
        }
        return null;
    }
}