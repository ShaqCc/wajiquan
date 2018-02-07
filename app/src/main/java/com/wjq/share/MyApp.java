package com.wjq.share;

import android.app.Application;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.wjq.share.bean.CarDetailBean;
import com.wjq.share.bean.PushBean;
import com.wjq.share.http.HttpUtils;
import com.wjq.share.service.CarDetailService;
import com.wjq.share.service.DownloadService;
import com.wjq.share.utils.SpUtils;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;
import com.wizchen.topmessage.util.TopActivityManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/8/28.
 ****************************************/

public class MyApp extends Application {

    private Retrofit mRetrofit;
    public static List<PushBean> sShareDataSource;//推送来的数据
    public static boolean isLocked = false;//是否在分享过程中

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(TopActivityManager.getInstance());
        sShareDataSource = new ArrayList<>();

        PushAgent mPushAgent = PushAgent.getInstance(this);
        //注册推送服务，每次调用register方法都会回调该接口
        mPushAgent.register(new IUmengRegisterCallback() {

            @Override
            public void onSuccess(String deviceToken) {
                //注册成功会返回device token
                Log.d("mytoken", deviceToken);
                SpUtils.put(getApplicationContext(), Constants.key_TOKEN, deviceToken);
            }

            @Override
            public void onFailure(String s, String s1) {
                Log.d("token Failuer",s+"---"+s1);
            }
        });

        mRetrofit = HttpUtils.getInstance().getRetrofit();
    }


//    UmengMessageHandler messageHandler = new UmengMessageHandler() {
//
//        @Override
//        public Notification getNotification(Context context, UMessage uMessage) {
//            Log.d("xxx", "ssssssss收到消息：" + uMessage.text);
//            String mCarid = "";
//            String mUserid = "";
//            String[] split = uMessage.text.split(",");
//            if (split.length > 0) {
//                for (String str : split) {
//                    Log.w("字符串分割", str);
//                    if (str.contains("carid")) {
//                        mCarid = str.replace("carid:", "");
//                    }
//                    if (str.contains("userid")) {
//                        mUserid = str.replace("userid:", "");
//                    }
//                }
//            }
//            Log.d("参数：", "userid:" + mUserid + "   carid:" + mCarid);
//            //查询产品信息
//            if (!TextUtils.isEmpty(mCarid) && !TextUtils.isEmpty(mUserid)) {
//                Log.d("开始查询",".......");
//                queryCarInfo(mCarid, mUserid);
//            }
//            switch (uMessage.builder_id) {
//                case 1:
//                    Notification.Builder builder = new Notification.Builder(context);
//                    RemoteViews myNotificationView = new RemoteViews(context.getPackageName(), R.layout.notification_view);
//                    myNotificationView.setTextViewText(R.id.notification_title, uMessage.title);
//                    myNotificationView.setTextViewText(R.id.notification_text, uMessage.text);
//                    myNotificationView.setImageViewBitmap(R.id.notification_large_icon, getLargeIcon(context, uMessage));
//                    myNotificationView.setImageViewResource(R.id.notification_small_icon, getSmallIconId(context, uMessage));
//                    builder.setContent(myNotificationView)
//                            .setSmallIcon(getSmallIconId(context, uMessage))
//                            .setTicker(uMessage.ticker)
//                            .setAutoCancel(true);
//
//                    return builder.getNotification();
//                default:
//                    //默认为0，若填写的builder_id并不存在，也使用默认。
//                    return super.getNotification(context, uMessage);
//            }
//        }
//
//
//        @Override
//        public void dealWithCustomMessage(final Context context, final UMessage msg) {
//            new Handler(getMainLooper()).post(new Runnable() {
//
//                @Override
//                public void run() {
//                    // 对于自定义消息，PushSDK默认只统计送达。若开发者需要统计点击和忽略，则需手动调用统计方法。
//                    boolean isClickOrDismissed = true;
//                    if (isClickOrDismissed) {
//                        //自定义消息的点击统计
//                        UTrack.getInstance(getApplicationContext()).trackMsgClick(msg);
//                    } else {
//                        //自定义消息的忽略统计
//                        UTrack.getInstance(getApplicationContext()).trackMsgDismissed(msg);
//                    }
//                    for (Map.Entry<String, String> entry : msg.extra.entrySet()) {
//                        Log.d("收到消息", "key:" + entry.getKey() + "--value:" + entry.getValue());
//                    }
//
//                    Map<String, String> extra = msg.extra;
//                    String carid = extra.get("carid");
//                    String userid = extra.get("userid");
//
//                    queryCarInfo(carid, userid);
//
//                    Toast.makeText(context, msg.custom, Toast.LENGTH_LONG).show();
//                }
//            });
//        }
//    };

    /**
     * 查询产品信息
     *
     * @param carid
     * @param userid
     */
    private void queryCarInfo(String carid, String userid) {
        String auth = HttpUtils.getMd5(Constants.USER, Constants.PASS, Constants.TIME);
        CarDetailService service = mRetrofit.create(CarDetailService.class);
        service.getCarInfo(carid, userid, Constants.USER,
                Constants.PASS, Constants.TIME, auth)
                .enqueue(new Callback<CarDetailBean>() {
                    @Override
                    public void onResponse(Call<CarDetailBean> call, Response<CarDetailBean> response) {
                        Log.w("onResponse", response.body().getData().getContent());
                        //获取图片url
                        ArrayList<String> urlList = new ArrayList<>();
                        for (CarDetailBean.DataBean.CImagesBean bean :
                                response.body().getData().getCImages()) {
                            urlList.add(bean.getSavename());
                        }

                        //获取分享文字内容
                        Intent intent = new Intent();
                        intent.setClass(getApplicationContext(), DownloadService.class);
                        intent.putExtra(Constants.KEY_WECHAT_CONTENT, getInformation(response.body()));
                        intent.putExtra(Constants.KEY_PICS_URL, urlList);
                        //开启下载服务
                        startService(intent);
                    }

                    @Override
                    public void onFailure(Call<CarDetailBean> call, Throwable t) {
                        Log.w("onFailure", "回调查询报错" + t.toString());
                    }
                });
    }


    /**
     * 组装要分享的文字内容
     *
     * @param detailBean
     * @return
     */
    private String getInformation(CarDetailBean detailBean) {
        StringBuilder sb = new StringBuilder();
        String content = (String) SpUtils.get(this, Constants.KEY_WECHAT_CONTENT, "");

        CarDetailBean.DataBean data = detailBean.getData();
        sb.append(data.getName()).append("，")
                .append(data.getYear()).append("年，价格")
                .append(data.getPrice()).append("万，").append(data.getContent())
                .append("车辆位于").append(data.getProvinceName().replace("省", "")).append(data.getCityName().replace("市", ""))
                .append("，电话：").append(data.getPhone())
                .append("，").append(TextUtils.isEmpty(content) ? "如需分享信息请将你的车辆发布至铲车圈" : content);
        return sb.toString();
    }
}
