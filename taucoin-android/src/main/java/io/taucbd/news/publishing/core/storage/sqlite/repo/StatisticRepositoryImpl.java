package io.taucbd.news.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Single;
import io.taucbd.news.publishing.core.model.data.CpuStatistics;
import io.taucbd.news.publishing.core.model.data.DataStatistics;
import io.taucbd.news.publishing.core.model.data.MemoryStatistics;
import io.taucbd.news.publishing.core.model.data.PeersAndInvoked;
import io.taucbd.news.publishing.core.storage.sqlite.AppDatabase;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Statistic;
import io.taucbd.news.publishing.core.Constants;

/**
 * StatisticRepository接口实现
 */
public class StatisticRepositoryImpl implements StatisticRepository {

    private Context appContext;
    private AppDatabase db;

    /**
     * StatisticRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public StatisticRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    @Override
    public void addStatistic(Statistic statistic) {
        db.statisticDao().addStatistic(statistic);
    }

    @Override
    public Single<List<DataStatistics>> getDataStatistics() {
        return db.statisticDao().getDataStatistics(Constants.STATISTICS_DISPLAY_PERIOD);
    }

    @Override
    public Single<List<MemoryStatistics>> getMemoryStatistics() {
        return db.statisticDao().getMemoryStatistics(Constants.STATISTICS_DISPLAY_PERIOD);
    }

    @Override
    public Single<List<PeersAndInvoked>> getPeersAndInvoked() {
        return db.statisticDao().getPeersAndInvoked(Constants.STATISTICS_DISPLAY_PERIOD);
    }

    @Override
    public Single<List<CpuStatistics>> getCpuStatistics() {
        return db.statisticDao().getCpuStatistics(Constants.STATISTICS_DISPLAY_PERIOD);
    }

    @Override
    public void deleteOldStatistics() {
        db.statisticDao().deleteOldStatistics();
    }
}
