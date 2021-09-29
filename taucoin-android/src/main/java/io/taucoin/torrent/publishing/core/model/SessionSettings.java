package io.taucoin.torrent.publishing.core.model;

import org.libTAU4j.SessionParams;
import org.libTAU4j.swig.session_params;
import org.libTAU4j.swig.settings_pack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SessionSettings {
    private static final Logger logger = LoggerFactory.getLogger("SessionSetting");
    static SessionParamsBuilder getSessionParamsBuilder() {
        return new SessionParamsBuilder();
    }

    static class SessionParamsBuilder {
        private settings_pack sp;

        SessionParamsBuilder() {
            sp = new settings_pack();
            // set bootstrap nodes
            sp.set_str(settings_pack.string_types.dht_bootstrap_nodes.swigValue(), dhtBootstrapNodes());
            // set listen network interfaces
            // sp.set_str(settings_pack.string_types.listen_interfaces.swigValue(), listenInterfaces());
        }

        /**
         * set account seed
         * @param seed account seed
         */
        SessionParamsBuilder setAccountSeed(String seed) {
            sp.set_str(settings_pack.string_types.account_seed.swigValue(), seed);
            return this;
        }

        /**
         * set database dir
         * @param dbDir database dir
         */
        SessionParamsBuilder setDatabaseDir(String dbDir) {
            dbDir += "/libTAU/db";
            logger.debug("DatabaseDir::{}", dbDir);
            sp.set_str(settings_pack.string_types.db_dir.swigValue(), dbDir);
            return this;
        }

        /**
         *  set device id
         * @param deviceId device id
         */
        SessionParamsBuilder setDeviceID(String deviceId) {
            logger.debug("DeviceID::{}", deviceId);
            sp.set_str(settings_pack.string_types.device_id.swigValue(), deviceId);
            return this;
        }

        /**
         * set network interface
         * @param networkInterface network interface
         */
        SessionParamsBuilder setNetworkInterface(String networkInterface) {
            logger.debug("NetworkInterface::{}", networkInterface);
            sp.set_str(settings_pack.string_types.listen_interfaces.swigValue(), networkInterface);
            return this;
        }

        SessionParams build() {
            session_params params = new session_params(sp);
            return new SessionParams(params);
        }
    }

    /**
     * 获取dhtBootstrapNodes配置
     * 字符串以","分割
     */
    private static String dhtBootstrapNodes() {
        StringBuilder sb = new StringBuilder();
        // sb.append("dht.libtorrent.org:25401").append(",");
//        sb.append("13.229.53.249:6882");
        sb.append("tau://83024767468B8BF8DB868F336596C63561265D553833E5C0BF3E4767659B826B@13.229.53.249:6882");
        return sb.toString();
    }
}
