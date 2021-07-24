package io.taucoin.torrent.publishing.ui.constant;

/**
 * QR内容
 */
public class PublicKeyQRContent extends QRContent{
    private String publicKey;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}