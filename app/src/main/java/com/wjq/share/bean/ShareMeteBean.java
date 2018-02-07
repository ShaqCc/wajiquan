package com.wjq.share.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by Administrator on 2017/9/22.
 *
 * 图片的url，分享内容
 */

public class ShareMeteBean implements Parcelable{
    private List<String> urlList;
    private String shareContent;

    public ShareMeteBean(List<String> urlList, String shareContent) {
        this.urlList = urlList;
        this.shareContent = shareContent;
    }

    protected ShareMeteBean(Parcel in) {
        urlList = in.createStringArrayList();
        shareContent = in.readString();
    }

    public static final Creator<ShareMeteBean> CREATOR = new Creator<ShareMeteBean>() {
        @Override
        public ShareMeteBean createFromParcel(Parcel in) {
            return new ShareMeteBean(in);
        }

        @Override
        public ShareMeteBean[] newArray(int size) {
            return new ShareMeteBean[size];
        }
    };

    public List<String> getUrlList() {
        return urlList;
    }

    public void setUrlList(List<String> urlList) {
        this.urlList = urlList;
    }

    public String getShareContent() {
        return shareContent;
    }

    public void setShareContent(String shareContent) {
        this.shareContent = shareContent;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(urlList);
        dest.writeString(shareContent);
    }
}
