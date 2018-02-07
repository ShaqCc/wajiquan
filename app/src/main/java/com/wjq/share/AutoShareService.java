package com.wjq.share;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.wjq.share.core.DownPicService;
import com.wjq.share.utils.ScreenLockUtils;

import java.lang.ref.WeakReference;
import java.util.List;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/8/31.
 ****************************************/

public class AutoShareService extends AccessibilityService {
    private String TAG = "AutoShareService";
    public static final int BACK = 333;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BACK:
                    back();
//                    home();
                    break;
            }
        }
    };
    private KeyguardManager.KeyguardLock kl;
    private boolean locked;
    private boolean background;
    private ScreenLockUtils instance;
    private ActivityManager atyManager;
    private TaskObservable taskObservable;
    private WeakReference<AutoShareService> weakReference = new WeakReference<AutoShareService>(this);

    /**
     * 回到桌面
     */
    private void home() {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
//        ScreenLockUtils.getInstance(this).lockScreen();
        instance.lockScreen();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.w(TAG, "服务启动...");
        instance = ScreenLockUtils.getInstance(this);
        DownPicService downPicService = new DownPicService();
        taskObservable = new TaskObservable();
        taskObservable.addObserver(downPicService);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.w(TAG, "接收事件：" + event.getEventType());
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                CharSequence className = event.getClassName();
                Log.w(TAG, "TYPE_WINDOW_STATE_CHANGED：" + event.getClassName());

                if (!TextUtils.isEmpty(className) && className.toString().contains("SnsUploadUI")) {
                    //发送朋友圈
                    Log.w(TAG, "------分享开始------");
                    sendWeChat();
                }

                //com.tencent.mm.plugin.sns.ui.En_c4f742e5 发朋友圈页面
                //com.tencent.mm.plugin.sns.ui.En_424b8e16 朋友圈页面

                break;
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                Log.w(TAG, "TYPE_WINDOWS_CHANGED：" + event.getClassName());
                break;

        }
    }

    /**
     * 在朋友圈页面，返回
     */
    private void back() {
        performGlobalAction(GLOBAL_ACTION_BACK);
        Log.w(TAG, "-------返回成功---------");
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (weakReference.get() != null) {
                    instance.lockScreen();
                    //发送消息，下载下一个
                    taskObservable.notifyShareFinish();
                }
            }
        });
    }

    /**
     * 发送朋友圈
     */
    private void sendWeChat() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByText("发送");
            for (AccessibilityNodeInfo n : list) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.w(TAG, "------分享成功------");
                n.recycle();
                //返回一步
                Log.w(TAG, "-------要返回------");
//                handler.sendEmptyMessage(BACK);
                handler.sendEmptyMessageDelayed(BACK, 2000);
            }
            nodeInfo.recycle();
        }
    }

    @Override
    public void onInterrupt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("警告！");
        builder.setMessage("onInterrupt");
        builder.show();
        Log.e("onAccessibilityEvent", "---onInterrupt----");
    }

    /**
     * 判断指定的应用是否在前台运行
     *
     * @param packageName
     * @return
     */
    private boolean isAppForeground(String packageName) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if (!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(packageName)) {
            return true;
        }

        return false;
    }


    /**
     * 回到系统桌面
     */
    private void back2Home() {
        Intent home = new Intent(Intent.ACTION_MAIN);

        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        home.addCategory(Intent.CATEGORY_HOME);

        startActivity(home);
    }


    /**
     * 系统是否在锁屏状态
     *
     * @return
     */
    private boolean isScreenLocked() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.inKeyguardRestrictedInputMode();
    }


    private void wakeAndUnlock() {
        //获取电源管理器对象
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");

        //点亮屏幕
        wl.acquire(1000);

        //得到键盘锁管理器对象
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        kl = km.newKeyguardLock("unLock");

        //解锁
        kl.disableKeyguard();

    }

    private void release() {

        if (locked && kl != null) {
            android.util.Log.d("maptrix", "release the lock");
            //得到键盘锁管理器对象
            kl.reenableKeyguard();
            locked = false;
        }
    }
}
