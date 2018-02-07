# CCQ-share
铲车圈分享朋友圈新版

自动分享到微信朋友圈，集成了友盟推送，接收到推送消息时解析消息内容，使用AccessibilityService将信息自动分享到朋友圈

### 前言

#### AccessibilityService辅助功能，可以帮我们自动去做一些事情，比如抢红包，自动安装应用，自动点击等等。    
#### 本demo是自动分享到朋友圈，不是集成微信的sdk，而是直接分享到朋友圈（图文）。    
#### AccessibilityService会自动关闭，只要宿主应用退出，辅助功能就会被关闭。而且只有在非息屏状态下，才有效。

---------------------------------------------

AccessibilityService使用步骤：

-    #### 在包名根目录新建一个service继承自AccessibilityService，实现抽象方法（service如果不在根目录创建可能会出现问题，华为nova调用不起来辅助功能）
-    在AndroidManifest文件中注册这个service，代码如下：
```xml
  <service
            android:name=".AutoShareService"
            android:enabled="true"
            android:exported="true"
            android:label="@string/app_name"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService"/>
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility"/>
   </service>
```

### 注释：
1.name就填service的名称

2.lable随便填，就是显示在辅助功能设置页面的标题

3.permission不要改动，直接复制这一条

4.intent-filter也不要改

5.meta-data里的name不要改

6.resource是service的配置，需要在res目录下新建文件夹，名称：xml ，在xml文件夹下新建xml文件，文件内容如下：


```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
                       android:accessibilityEventTypes="typeAllMask"
                       android:accessibilityFeedbackType="feedbackAllMask"
                       android:accessibilityFlags="flagDefault"
                       android:canRetrieveWindowContent="true"
                       android:description="@string/desc"
                       android:notificationTimeout="10"
                       android:packageNames="com.tencent.mm"
                       android:settingsActivity="com.ccq.share.activity.MainSettingsActivity"/>
```

### 注释：
1. accessibilityEventTypes 表示service要拦截，监听的动作类型，typeAllMask表示全部监听，比如点击，滑动，窗口焦点改变，通知栏改变等等     
2. accessibilityFeedbackType 相当于回调类型，收到监听动作的回调，feedbackAllMask代表所有回调类型     
3. accessibilityFlags不用改          
4. #### canRetrieveWindowContent 很重要，必须设置为true，不然获取不到当前活动窗口的内的组件        
5. description 这个服务的描述信息，随便写           
6. notificationTimeout 监听的时间间隔 10毫秒，看自己的选择了            
7. packageNames 要监听应用的包名，不写就是全部监听，监听多个：“包名,包名,包名”中间用逗号隔开             
8. settingsActivity 就写你设置打开辅助功能的那个页面        


- ### 完善service代码   
   监听到自己注册的事件在哪里相应呢？需要复写AccessibilityService的onAccessibilityEvent(AccessibilityEvent event)方法，如下：
   
   ```java
   @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.w(TAG, "接收事件：" + event.getEventType());
        int eventType = event.getEventType();//获取事件类型
        switch (eventType) {//对不同类型作出处理
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED://窗口状态改变
                CharSequence className = event.getClassName();//当前页面activity的类名
                Log.w(TAG, "当前类名：" + event.getClassName());
                //com.tencent.mm.plugin.sns.ui.En_c4f742e5
                //com.tencent.mm.plugin.sns.ui.En_424b8e16
                if (!TextUtils.isEmpty(className) && className.toString().contains("com.tencent.mm.plugin.sns.ui"))
                {
                    Log.w(TAG,"------分享开始------");
                    sendWeChat();
                }
                break;
        }
    }
   ```


- ### 如何开启这个服务？    
   不用startservice，也不用bindservice，需要手动设置，进入设置 -> 辅助功能 找到自己注册的服务，开启即可。代码进入设置页面如下：
   
   ```java
   Intent access = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
   startActivity(access);
   ```













