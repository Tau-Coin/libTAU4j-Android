package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import org.libTAU4j.Ed25519;
import org.libTAU4j.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.receiver.BootReceiver;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.core.utils.rlp.CryptoUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/*
 * General utils.
 */

public class Utils {
    public static final String HASH_PATTERN = "\\b[0-9a-fA-F]{5,40}\\b";

    public static boolean isHash(@NonNull String hash) {
        if (TextUtils.isEmpty(hash))
            return false;

        Pattern pattern = Pattern.compile(HASH_PATTERN);
        Matcher matcher = pattern.matcher(hash.trim());

        return matcher.matches();
    }

    @Nullable
    public static ClipData getClipData(@NonNull Context context) {
        ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Activity.CLIPBOARD_SERVICE);
        if (!clipboard.hasPrimaryClip())
            return null;

        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0)
            return null;

        return clip;
    }

    public static List<CharSequence> getClipboardText(@NonNull Context context)
    {
        ArrayList<CharSequence> clipboardText = new ArrayList<>();

        ClipData clip = Utils.getClipData(context);
        if (clip == null)
            return clipboardText;

        for (int i = 0; i < clip.getItemCount(); i++) {
            CharSequence item = clip.getItemAt(i).getText();
            if (item == null)
                continue;
            clipboardText.add(item);
        }

        return clipboardText;
    }

    public static int getAppTheme(@NonNull Context context) {
        return R.style.AppTheme;
    }
    /*
     * Migrate from Tray settings database to shared preferences.
     * TODO: delete after some releases
     */
    @Deprecated
    public static void migrateTray2SharedPreferences(@NonNull Context appContext)
    {
        final String TAG = "tray2shared";
        final String migrate_key = "tray2shared_migrated";
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(appContext);

        if (pref.getBoolean(migrate_key, false))
            return;

        File dbFile = appContext.getDatabasePath("tray.db");
        if (dbFile == null || !dbFile.exists()) {
            Log.w(TAG, "Database not found");
            pref.edit().putBoolean(migrate_key, true).apply();

            return;
        }
        SQLiteDatabase db;
        try {
            db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't open database: " + Log.getStackTraceString(e));
            appContext.deleteDatabase("tray");
            pref.edit().putBoolean(migrate_key, true).apply();

            return;
        }
        Cursor c = db.query("TrayPreferences",
                new String[]{"KEY", "VALUE"},
                null,
                null,
                null,
                null,
                null);
        SharedPreferences.Editor edit = pref.edit();
        Log.i(TAG, "Start migrate");
        try {
            int key_i = c.getColumnIndex("KEY");
            int value_i = c.getColumnIndex("VALUE");
            while (c.moveToNext()) {
                String key = c.getString(key_i);
                String value = c.getString(value_i);

                if (value.equalsIgnoreCase("true")) {
                    edit.putBoolean(key, true);
                } else if (value.equalsIgnoreCase("false")) {
                    edit.putBoolean(key, false);
                } else {
                    try {
                        int number = Integer.parseInt(value);
                        edit.putInt(key, number);
                    } catch (NumberFormatException e) {
                        edit.putString(key, value);
                    }
                }
            }
            Log.i(TAG, "Migrate completed");

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            c.close();
            db.close();
            appContext.deleteDatabase("tray.db");
            edit.putBoolean(migrate_key, true);
            edit.apply();
        }
    }

    /*
     * Workaround for start service in Android 8+ if app no started.
     * We have a window of time to get around to calling startForeground() before we get ANR,
     * if work is longer than a millisecond but less than a few seconds.
     */

    public static void startServiceBackground(@NonNull Context context, @NonNull Intent i)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(i);
        else
            context.startService(i);
    }

    /**
     * ??????/?????????????????????????????????
     * @param context ?????????
     * @param enable ????????????
     */
    public static void enableBootReceiver(@NonNull Context context, boolean enable) {
        int flag = !enable ?
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED :
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        ComponentName bootReceiver = new ComponentName(context, BootReceiver.class);
        context.getPackageManager()
                .setComponentEnabledSetting(bootReceiver, flag, PackageManager.DONT_KILL_APP);
    }

    /**
     * ??????/??????????????????????????????
     * @param context ?????????
     * @param cls ??????
     * @param enable ????????????
     */
    public static void enableComponent(@NonNull Context context,  @NonNull Class<?> cls, boolean enable){
        int flag = !enable ?
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED :
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        ComponentName componentName = new ComponentName(context, cls);
        context.getPackageManager()
                .setComponentEnabledSetting(componentName, flag, PackageManager.DONT_KILL_APP);
    }


    /*
     * Without additional information (e.g -DEBUG)
     */

    public static String getAppVersionNumber(@NonNull String versionName)
    {
        int index = versionName.indexOf("-");
        if (index >= 0)
            versionName = versionName.substring(0, index);

        return versionName;
    }

    /*
     * Return version components in these format: [major, minor, revision]
     */

    public static int[] getVersionComponents(@NonNull String versionName)
    {
        int[] version = new int[3];

        /* Discard additional information */
        versionName = getAppVersionNumber(versionName);

        String[] components = versionName.split("\\.");
        if (components.length < 2)
            return version;

        try {
            version[0] = Integer.parseInt(components[0]);
            version[1] = Integer.parseInt(components[1]);
            if (components.length >= 3)
                version[2] = Integer.parseInt(components[2]);

        } catch (NumberFormatException e) {
            /* Ignore */
        }

        return version;
    }

    public static String makeSha1Hash(@NonNull String s)
    {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        messageDigest.update(s.getBytes(Charset.forName("UTF-8")));
        StringBuilder sha1 = new StringBuilder();
        for (byte b : messageDigest.digest()) {
            if ((0xff & b) < 0x10)
                sha1.append("0");
            sha1.append(Integer.toHexString(0xff & b));
        }

        return sha1.toString();
    }

    public static SSLContext getSSLContext() throws GeneralSecurityException
    {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore)null);

        TrustManager[] trustManagers = tmf.getTrustManagers();
        final X509TrustManager origTrustManager = (X509TrustManager)trustManagers[0];

        TrustManager[] wrappedTrustManagers = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers()
                    {
                        return origTrustManager.getAcceptedIssuers();
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException
                    {
                        origTrustManager.checkClientTrusted(certs, authType);
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException
                    {
                        origTrustManager.checkServerTrusted(certs, authType);
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, wrappedTrustManagers, null);

        return sslContext;
    }

    public static void showActionModeStatusBar(@NonNull Activity activity, boolean mode)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return;

        int attr = (mode ? R.attr.actionModeBackground : R.attr.statusBarColor);
        activity.getWindow().setStatusBarColor(getAttributeColor(activity, attr));
    }

    public static int getAttributeColor(@NonNull Context context, int attributeId)
    {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, typedValue, true);
        int colorRes = typedValue.resourceId;
        int color = -1;
        try {
            color = context.getResources().getColor(colorRes);

        } catch (Resources.NotFoundException e) {
            return color;
        }

        return color;
    }

    /**
     * ??????groupName???????????????????????????
     * @param firstLetters ?????????????????????
     * @return ??????
     */
    public static int getGroupColor(String firstLetters) {
        Context context = MainApplication.getInstance();
        Resources res = context.getResources();
        int[] colors = res.getIntArray(R.array.group_color);
        int charCount = 0;
        if(StringUtil.isNotEmpty(firstLetters)){
            char[] chars = firstLetters.toCharArray();
            for (char aChar : chars) {
                charCount += aChar;
            }
        }
        return colors[charCount % colors.length];
    }

    /**
     * ??????URL????????????
     * @param url URL
     * @return ????????????
     */
    public static boolean validateUrl(String url) {
        String strRegex = "^"
                + "(([0-9]{1,3}.){3}[0-9]{1,3}" // IP?????????URL- 199.194.52.184
                + "|" // ??????IP???DOMAIN????????????
                + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]." // ????????????
                + "[a-z]{2,6})" // first level domain- .com or .museum
                + "(:[0-9]{1,4})" // ??????- :80
                + "$";
        Pattern pattern = Pattern.compile(strRegex);
        Matcher matcher = pattern.matcher(url);
        return matcher.find();
    }

    /**
     * ?????????????????????
     */
    public static long getMedianData(List<Long> total) {
        if(total.size() > 0){
            int size = total.size();
            if(size % 2 == 1){
                return total.get((size -1 ) / 2);
            }else {
                return (long) ((total.get(size / 2 - 1) + total.get(size / 2) + 0.0) / 2);
            }
        }
        return Constants.MIN_FEE.longValue();
    }

    /**
     * byte[]?????????UTF_8 String
     * @param bytes byte[]
     * @return UTF_8 String
     */
    public static String toUTF8String(byte[] bytes){
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static class MatcherResult{
        String link;
        int start;
        int end;
        MatcherResult(String link, int start, int end){
            this.link = link;
            this.start = start;
            this.end = end;
        }
    }

    public static boolean isTablet(FragmentActivity activity) {
        return (activity.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * ??????chrome book??????, ?????????????????????????????????????????????1/2
     * @param activity
     * @return
     */
    private static boolean isPad(FragmentActivity activity) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        DisplayMetrics dm1 = activity.getApplicationContext().getResources().getDisplayMetrics();
        return dm.widthPixels >= dm1.widthPixels / 2;
    }

    public static boolean isLandscape() {
        Context context = MainApplication.getInstance();
        Configuration mConfiguration = context.getResources().getConfiguration(); //???????????????????????????
        int ori = mConfiguration.orientation; //??????????????????
        return ori == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * ????????????
     * @param publicKey ????????????
     * @param seed ??????Seed
     * @return ??????????????????
     */
    public static byte[] keyExchange(String publicKey, String seed) {
        if (StringUtil.isNotEmpty(seed)) {
            try {
                byte[] seedBytes = ByteUtil.toByte(seed);
                Pair<byte[], byte[]> keypair = Ed25519.createKeypair(seedBytes);
                byte[] secretKey = keypair.second;
                return CryptoUtil.keyExchange(ByteUtil.toByte(publicKey), secretKey);
            } catch (Exception ignore) { }
        }
        return null;
    }

    /**
     * ????????????
     * @param seed ??????Seed
     * @return ????????????
     */
    public static boolean isKeyValid(String seed) {
        if (StringUtil.isNotEmpty(seed)) {
            try {
                byte[] seedBytes = ByteUtil.toByte(seed);
                Ed25519.createKeypair(seedBytes);
                return true;
            } catch (Exception ignore) { }
        }
        return false;
    }

    /**
     * byte[]???????????????String
     * @param msg
     * @return
     */
    public static String textBytesToString(byte[] msg) {
        if (msg != null) {
            return new String(msg, StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    /**
     * String???????????????byte[]
     * @param msg
     * @return
     */
    public static byte[] textStringToBytes(String msg) {
        if (StringUtil.isNotEmpty(msg)) {
            return msg.getBytes(StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    public static boolean isChinese(String str) {
        if (StringUtil.isEmpty(str)) {
            return false;
        }
        char[] chars = str.toCharArray();
        for (char c : chars) {
            if (c >= 0x4E00 && c <= 0x9FA5) {
                return true;
            }
        }
        return false;
    }
}