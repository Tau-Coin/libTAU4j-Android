package io.taucoin.tauapp.publishing;

import org.junit.Test;

import java.util.Arrays;

import io.taucoin.tauapp.publishing.core.model.data.message.DataKey;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class DataKeyTest {
    @Test
    public void TestDataKey() {
        byte[] keySrc = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        System.out.println("keySrc::" + Arrays.toString(keySrc));
        byte[] key0 = DataKey.getKey(keySrc, DataKey.Suffix.INFO);
        System.out.println("key0::" + Arrays.toString(key0));

        DataKey.Suffix suffix0 = DataKey.getSuffix(key0);
        System.out.println("suffix0::" + suffix0.getValue());


        byte[] key1 = DataKey.getKey(keySrc, DataKey.Suffix.PIC);
        System.out.println("key1::" + Arrays.toString(key1));

        DataKey.Suffix suffix1 = DataKey.getSuffix(key1);
        System.out.println("suffix1::" + suffix1.getValue());
    }
}