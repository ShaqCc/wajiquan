package com.wjq.share.core;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.wjq.share.Constants;
import com.wjq.share.bean.ShareMeteBean;
import com.wjq.share.http.DownLoadUtils;
import com.wjq.share.http.HttpUtils;
import com.wjq.share.service.DownloadService;
import com.wjq.share.utils.ScreenLockUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/9/1.
 ****************************************/

public class DownPicService extends Service implements Observer {
    private static final int COMMON_LOOPER = 1;//一条推送消息的图片下载完毕，开始下载另一条

    ArrayList<Uri> sListUri;//已经下载好的图片uri
    private List<ShareMeteBean> mDataSourceList = new ArrayList<>();//要下载的图片地址，分享内容数据源

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case COMMON_LOOPER:
                    if (mDataSourceList.size() > 0) {
                        downLoadPics(mDataSourceList.remove(0));
                    } else {
                        isDownLoading = false;
                    }
                    break;
            }
        }
    };

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.w("DownPicService", "onStart.....");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("DownPicService", "onStartCommand.....");
        ShareMeteBean metaDataBean = intent.getParcelableExtra(Constants.KEY_SHARE_METE_DATA);
        //判断是否下载图片中
//        if (isDownloading) {
//            //将数据添加到数据池
//            mDataSourceList.add(metaDataBean);
//        } else {
//            downLoadPics(metaDataBean);
//        }
        if (isDownLoading) {
            mDataSourceList.add(metaDataBean);
        } else {
            downLoadPics(metaDataBean);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private boolean isDownLoading = false;

    /**
     * 下载图片
     *
     * @param bean
     */
    private synchronized void downLoadPics(ShareMeteBean bean) {
        //开始下载
        isDownLoading = true;
        List<String> list = bean.getUrlList();
        //为空，不下载
        if (list == null || list.size() == 0) {
            initLooper();
            return;
        }

        final String content = bean.getShareContent();
        while (list.size() > 9) {
            list.remove(list.size() - 1);
        }

        sListUri = new ArrayList<>();
        final DownloadService service = HttpUtils.getInstance().getPicRetrofit().create(DownloadService.class);

        Observable.from(list)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<String, Observable<ResponseBody>>() {
                    @Override
                    public Observable<ResponseBody> call(String url) {
                        String fileUrl = DownLoadUtils.getUrl(url);
                        if (TextUtils.isEmpty(fileUrl)) {
                            return null;
                        } else {
                            Log.w("开始下载...", fileUrl);
                            return service.downloadPic(fileUrl);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {
                        Log.w("全部完成！", "数量：" + sListUri.size());
//                        initLooper();
                        //发送消息
                        share(sListUri, content);
                    }

                    @Override
                    public void onError(Throwable e) {
                        initLooper();
                        Log.e("xxxx下载出错！", e.toString());
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            File file = DownLoadUtils.writeToFile(responseBody.bytes());
                            sListUri.add(Uri.fromFile(file));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    /**
     * 下载任务结束，初始化下一次循环
     */
    private void initLooper() {
        mHandler.sendEmptyMessage(COMMON_LOOPER);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void share(ArrayList<Uri> uris, String content) {
        if (uris.size() == 0) {
            Log.e("xxxxxxxxxxx", "资源为零！！！！！");
            return;
        }
        ScreenLockUtils.getInstance(this).unLockScreen();
        Intent intent = new Intent();
        ComponentName comp = new ComponentName("com.tencent.mm",
                Constants.WECHAT_SHAREUI_NAME);
        intent.setComponent(comp);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.putExtra("Kdescription", content);
        startActivity(intent);
        //分享完，进行下一个
        Log.d("share", "执行下一个任务！");
        initLooper();
    }

    @Override
    public void update(java.util.Observable o, Object arg) {
//        Log.d("update", "收到通知，执行下一个任务！");
//        initLooper();
    }
}
