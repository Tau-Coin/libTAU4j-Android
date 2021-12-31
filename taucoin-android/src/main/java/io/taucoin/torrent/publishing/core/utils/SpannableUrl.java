package io.taucoin.torrent.publishing.core.utils;

import android.text.SpannableStringBuilder;
import android.util.Patterns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;

public class SpannableUrl {
    /**
     * Valid UCS characters defined in RFC 3987. Excludes space characters.
     */
    private static final String UCS_CHAR = "[" +
            "\u00A0-\uD7FF" +
            "\uF900-\uFDCF" +
            "\uFDF0-\uFFEF" +
            "\uD800\uDC00-\uD83F\uDFFD" +
            "\uD840\uDC00-\uD87F\uDFFD" +
            "\uD880\uDC00-\uD8BF\uDFFD" +
            "\uD8C0\uDC00-\uD8FF\uDFFD" +
            "\uD900\uDC00-\uD93F\uDFFD" +
            "\uD940\uDC00-\uD97F\uDFFD" +
            "\uD980\uDC00-\uD9BF\uDFFD" +
            "\uD9C0\uDC00-\uD9FF\uDFFD" +
            "\uDA00\uDC00-\uDA3F\uDFFD" +
            "\uDA40\uDC00-\uDA7F\uDFFD" +
            "\uDA80\uDC00-\uDABF\uDFFD" +
            "\uDAC0\uDC00-\uDAFF\uDFFD" +
            "\uDB00\uDC00-\uDB3F\uDFFD" +
            "\uDB44\uDC00-\uDB7F\uDFFD" +
            "&&[^\u00A0[\u2000-\u200A]\u2028\u2029\u202F\u3000]]";

    /**
     * Valid characters for IRI label defined in RFC 3987.
     */
    private static final String LABEL_CHAR = "a-zA-Z0-9" + UCS_CHAR;
    private static final String PATH_AND_QUERY = "[/\\?](?:(?:[" + LABEL_CHAR
            + ";/\\?:@&=#~"  // plus optional query params
            + "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*";

    /* A word boundary or end of input.  This is to stop foo.sure from matching as foo.su */
    private static final String WORD_BOUNDARY = "(?:\\b|$|^)";
    public static final Pattern MAGNET_URL = Pattern.compile("("
            + "(?i:magnet):\\?"
            + "("
            + "(?:(?:[" + LABEL_CHAR
            + ";/\\?:@&=#~"  // plus optional query params
            + "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*"
            + ")?"
            + WORD_BOUNDARY
            + ")");
    /**
     * 解析字符串中的link，加下划线和改成蓝色
     */
    public static SpannableStringBuilder generateSpannableUrl(String msg) {
        SpanUtils spanUtils = new SpanUtils();
        if (StringUtil.isNotEmpty(msg)) {
            generateSpannableUrl(spanUtils, msg);
        }
        return spanUtils.create();
    }

    private static void generateSpannableUrl(SpanUtils spanUtils, String msg) {
        if(StringUtil.isNotEmpty(msg)){
            Utils.MatcherResult result = parseMatcherFormStr(msg);
            if(result != null){
                String link = result.link;
                int linkStart = result.start;
                int linkEnd = result.end;
                spanUtils.append(msg.substring(0, linkStart));
                spanUtils.append(link);
                int blueColor = MainApplication.getInstance().getResources().getColor(R.color.color_blue_light);
                spanUtils.setForegroundColor(blueColor);
                spanUtils.append(msg.substring(linkEnd));
                return;
            }
        }
        spanUtils.append(msg);
    }

    public static String parseUrlFormStr(String msg){
        Utils.MatcherResult result = parseMatcherFormStr(msg);
        if(result != null){
            return result.link;
        }
        return null;
    }

    private static Utils.MatcherResult parseMatcherFormStr(String msg){
        if (null == msg) {
            return null;
        }
        Matcher magnet = MAGNET_URL.matcher(msg);
        if(magnet.find()){
            String magnetUrl = magnet.group();
            int magnetStart = magnet.start();
            int magnetEnd = magnet.end();
            if(magnetStart < magnetEnd){
                return new Utils.MatcherResult(magnetUrl, magnetStart, magnetEnd);
            }
        }
        Matcher web = Patterns.WEB_URL.matcher(msg);
        if(web.find()){
            String webLink = web.group();
            int webStart = web.start();
            int webEnd = web.end();

            if(webStart < webEnd){
                return new Utils.MatcherResult(webLink, webStart, webEnd);
            }
        }
        return null;
    }
}
