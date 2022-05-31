package io.taucoin.torrent.publishing.ui.crash;

import android.app.Application;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.utils.FileUtil;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.HttpUtil;
import io.taucoin.torrent.publishing.core.utils.ZipUtil;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import okhttp3.Response;

/**
 * Crash相关的ViewModel
 */
public class CrashViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("CrashViewModel");
    private static final String BASE_URL = "https://taucoin.io/";
    private static final String DUMP_FILE_UPLOAD = BASE_URL + "upload";
    private AlertDialog mDialog;

    private Disposable uploadAsyncDisposable;
    private Disposable uploadDisposable;
    private MutableLiveData<Result> uploadResult = new MutableLiveData<>();
    public CrashViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (uploadDisposable != null && !uploadDisposable.isDisposed()) {
            uploadDisposable.dispose();
        }
        if (uploadAsyncDisposable != null && !uploadAsyncDisposable.isDisposed()) {
            uploadAsyncDisposable.dispose();
        }
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.cancel();
        }
    }

    public MutableLiveData<Result> getUploadResult() {
        return uploadResult;
    }

    /**
     * 上传Dump文件
     */
    public void uploadDumpFile(BaseActivity activity, boolean isPromptUser) {
        if (uploadDisposable != null && !uploadDisposable.isDisposed()) {
            return;
        }
        uploadDisposable = Observable.create((ObservableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            result.setSuccess(false); // 修改默认值
            try {
                File file = checkDumpFile();
                if (file != null) {
                    result.setExist(true);
                    result.setMsg(file.getAbsolutePath());
                    if (!isPromptUser) {
                        uploadDumpFileSync(file, result);
                    }
                } else {
                    result.setExist(false);
                }
            } catch (Exception e) {
                result.setFailMsg(e.getMessage());
            }
            logger.info("uploadDumpFile exist::{}, success::{}, msg::{}, isPromptUser::{}",
                    result.isExist(), result.isSuccess(), result.getMsg(), isPromptUser);
            emitter.onNext(result);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.isExist() && isPromptUser) {
                        showDumpDialog(activity, result);
                    } else {
                        uploadResult.postValue(result);
                    }
                });
    }

    private void uploadDumpFileSync(File file, Result result) throws Exception {
        Response response;
        String filePath = file.getParentFile().getAbsolutePath();
        String fileName = file.getName();
        fileName = fileName.substring(0, fileName.indexOf("."));
        String destZipFile = filePath + File.separator + fileName + ".tar.gz";
        ZipUtil.compress(file.getAbsolutePath(), destZipFile);
        File zipFile = new File(destZipFile);
        if (zipFile.exists() && zipFile.length() > 0) {
            result.setMsg(destZipFile);
            logger.debug("DumpFile gzip::{}, size::{}", destZipFile,
                    Formatter.formatFileSize(getApplication(), zipFile.length()));
            response = HttpUtil.httpPostFile(DUMP_FILE_UPLOAD, zipFile);
        } else {
            logger.debug("uploadDumpFileSync file size::{}",
                    Formatter.formatFileSize(getApplication(), file.length()));
            response = HttpUtil.httpPostFile(DUMP_FILE_UPLOAD, file);
        }
        if (response.isSuccessful()) {
            result.setSuccess(response.code() == 200);
            FileUtil.deleteFile(file.getParentFile());
        } else {
            result.setSuccess(false);
        }
    }

    private void uploadDumpFileAsync(File file, Result result) {
        if (uploadAsyncDisposable != null && !uploadAsyncDisposable.isDisposed()) {
            return;
        }
        uploadAsyncDisposable = Observable.create((ObservableOnSubscribe<Result>) emitter -> {
            try {
                uploadDumpFileSync(file, result);
            } catch (Exception e) {
                result.setFailMsg(e.getMessage());
            }
            logger.info("uploadDumpFileAsync exist::{}, success::{}, msg::{}", result.isExist(),
                    result.isSuccess(), result.getMsg());
            emitter.onNext(result);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> {
                    uploadResult.postValue(value);
                });
    }


    private File checkDumpFile() {
        File latestFile = null;
        File dir = new File(FileUtil.getDumpfileDir());
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (null == latestFile) {
                        latestFile = file;
                    }
                    if (file.lastModified() > latestFile.lastModified()) {
                        latestFile = file;
                    }
                }
            }
        }
        logger.info("checkDumpFile exists::{}, dumpPath::{}",
                latestFile != null && latestFile.exists(),
                latestFile != null ? latestFile.getAbsolutePath() : "");
        return latestFile;
    }

    private void showDumpDialog(BaseActivity activity, Result result) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.cancel();
        }
        // 最后通过构造函数将样式传进去
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomAlertDialog)
                .setCancelable(false)
                .setMessage(R.string.dump_report)
                .setNegativeButton(R.string.common_report, null)
                .setPositiveButton(R.string.common_proceed, null);

        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.show();

        setMessageStyle(mDialog);

        File file = new File(result.getMsg());
        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            mDialog.cancel();
            activity.showProgressDialog(getApplication().getString(R.string.dump_uploading));
            uploadDumpFileAsync(file, result);
        });

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            mDialog.cancel();
            FileUtil.deleteFile(file.getParentFile());
            result.setExist(false);
            uploadResult.postValue(result);
        });
    }

    private void setMessageStyle(AlertDialog mDialog) {
        try {
            Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
            mAlert.setAccessible(true);
            Object mAlertController = mAlert.get(mDialog);
            Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
            mMessage.setAccessible(true);
            TextView mMessageView = (TextView) mMessage.get(mAlertController);
            mMessageView.setLineSpacing(getApplication().getResources().getDimensionPixelSize(R.dimen.widget_size_5), 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}