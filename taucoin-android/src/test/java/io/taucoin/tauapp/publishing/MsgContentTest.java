package io.taucoin.tauapp.publishing;

import org.junit.Test;

import io.taucoin.tauapp.publishing.core.model.data.message.MsgContent;

public class MsgContentTest {

    @Test
    public void test() {
        MsgContent content = MsgContent.createTextContent("122222222122222222122222222122222222",
                "test".getBytes(), "");
        byte[] encoded = content.getEncoded();
        System.out.println("encoded : " + encoded + ", length : " + encoded.length);

        MsgContent content1 = new MsgContent(encoded);
        System.out.println("getVersion : " + content1.getVersion());
        System.out.println("getLogicHash : " + content1.getLogicHash());
        System.out.println("getType : " + content1.getType());
        System.out.println("getContent : " + new String(content.getContent()));
    }
}