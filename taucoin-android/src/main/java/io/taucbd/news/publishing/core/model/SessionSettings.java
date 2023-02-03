package io.taucbd.news.publishing.core.model;

import org.libTAU4j.SessionParams;
import org.libTAU4j.swig.session_params;
import org.libTAU4j.swig.settings_pack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucbd.news.publishing.BuildConfig;

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
         * set dump dir
         * @param dumpFileDir dump file dir
         */
        SessionParamsBuilder setDumpfileDir(String dumpFileDir) {
            logger.debug("Dumpfile::{}", dumpFileDir);
            sp.set_str(settings_pack.string_types.dump_dir.swigValue(), dumpFileDir);
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

        /**
         * set dht read only
         * @param readOnly read only
         */
        SessionParamsBuilder setReadOnly(boolean readOnly) {
            logger.debug("setReadOnly::{}", readOnly);
            sp.set_bool(settings_pack.bool_types.dht_read_only.swigValue(), readOnly);
            return this;
        }

        /**
         * set dht bootstrap interval
         * @param interval 时间间隔 单位：s
         */
        SessionParamsBuilder setDhtBootstrapInterval(int interval) {
            logger.debug("setDhtBootstrapInterval::{}", interval);
            sp.set_int(settings_pack.int_types.dht_bootstrap_interval.swigValue(), interval);
            return this;
        }

        /**
         * set dht non referable
         * @param nonReferable non referable
         */
        SessionParamsBuilder setDhtNonReferable(boolean nonReferable) {
            logger.debug("setDhtNonReferable::{}", nonReferable);
            sp.set_bool(settings_pack.bool_types.dht_non_referrable.swigValue(), nonReferable);
            return this;
        }

        /**
         * set dht auto relay
         * @param autoRelay auto relay
         */
        SessionParamsBuilder setDhtAutoRelay(boolean autoRelay) {
            logger.debug("setDhtAutoRelay::{}", autoRelay);
            sp.set_bool(settings_pack.bool_types.auto_relay.swigValue(), autoRelay);
            return this;
        }

        /**
         * set dht ping interval
         * @param interval 时间间隔 单位：s
         */
        SessionParamsBuilder setDhtPingInterval(int interval) {
            logger.debug("setDhtPingInterval::{}", interval);
            sp.set_int(settings_pack.int_types.dht_ping_interval.swigValue(), interval);
            return this;
        }

        /**
         * set log level
         * @param level 日志level
         */
        SessionParamsBuilder setLogLevel(int level) {
            logger.debug("setLogLevel::{}", level);
            sp.set_int(settings_pack.int_types.log_level.swigValue(), level);
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
        return BuildConfig.DHT_BOOTSTRAP_NODES;
    }
}
