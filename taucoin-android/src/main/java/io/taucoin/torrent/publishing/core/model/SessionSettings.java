package io.taucoin.torrent.publishing.core.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileAlreadyExistsException;

import org.libTAU4j.SessionParams;
import org.libTAU4j.swig.session_params;
import org.libTAU4j.swig.settings_pack;
import org.slf4j.LoggerFactory;

class SessionSettings {

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
            //dbDir += "/libTAU/db/.tau";
            dbDir = "/storage/emulated/0/Android/data/io.taucoin.torrent.publishing/files/libTAU/db/.tau";

			/*
			String kvdbDir = dbDir + "/kvdb";
			String sqldbDir = dbDir + "/sqldb";

			Path kv = Paths.get(kvdbDir);
			Path sql = Paths.get(sqldbDir);
			try {
				Path pp = Files.createDirectories(kv);
				pp = Files.createDirectories(sql);
			} catch (FileAlreadyExistsException e){
    			// the directory already exists.
    			e.printStackTrace();
			} catch (IOException e){
    			// the directory already exists.
    			e.printStackTrace();
			}
			*/
            LoggerFactory.getLogger("SessionSetting").debug("DatabaseDir::{}", dbDir);
            sp.set_str(settings_pack.string_types.db_dir.swigValue(), dbDir);
            return this;
        }

        /**
         *  set device id
         * @param deviceId device id
         */
        SessionParamsBuilder setDeviceID(String deviceId){
            sp.set_str(settings_pack.string_types.db_dir.swigValue(), deviceId);
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
        sb.append("13.229.53.249:6881");
        return sb.toString();
    }
}
