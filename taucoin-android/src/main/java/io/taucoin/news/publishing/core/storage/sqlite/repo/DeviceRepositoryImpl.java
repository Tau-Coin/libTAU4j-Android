package io.taucoin.news.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.taucoin.news.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Device;

/**
 * DeviceRepository接口实现
 */
public class DeviceRepositoryImpl implements DeviceRepository{

    private Context appContext;
    private AppDatabase db;

    /**
     * DeviceRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public DeviceRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 添加用户设备信息
     * @param device
     */
    @Override
    public void addDevice(Device device) {
        db.deviceDao().addDevice(device);
    }

    /**
     * 更新用户设备信息
     * @param device
     */
    @Override
    public void updateDevice(Device device) {
        db.deviceDao().updateDevice(device);
    }

    /**
     * 观察用户所用的设备变化
     * @param userPK
     * @return
     */
    @Override
    public Observable<List<Device>> observerDevices(String userPK) {
        return db.deviceDao().observerDevices(userPK);
    }
}
