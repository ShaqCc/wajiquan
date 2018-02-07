package com.wjq.share.adapter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.wjq.share.Constants;
import com.wjq.share.MyGridView;
import com.wjq.share.activity.MainActivity;
import com.wjq.share.bean.CarDetailBean;
import com.wjq.share.bean.CarInfoBean;
import com.wjq.share.bean.ShareMeteBean;
import com.wjq.share.core.DownPicService;
import com.wjq.share.http.HttpUtils;
import com.wjq.share.http.PackageUtils;
import com.wjq.share.service.CarDetailService;
import com.wjq.share.utils.DensityUtils;
import com.wjq.share.utils.PhoneUtils;
import com.wjq.share.utils.SpUtils;

import com.ccq.share.R;
import com.previewlibrary.PhotoActivity;
import com.previewlibrary.ThumbViewInfo;
import com.wizchen.topmessage.TopMessageManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Administrator on 2017/8/25.
 */

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {

    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 0;
    private List<CarInfoBean> mDataList;
    private MainActivity context;
    private ArrayList<ThumbViewInfo> mThumbList;
    private ProgressDialog progressDialog;

    public ProductAdapter(List<CarInfoBean> beanList) {
        this.mDataList = beanList;
    }

    public void refresh(List<CarInfoBean> beanList) {
        this.mDataList = beanList;
        notifyDataSetChanged();
    }

    public void loadMore(List<CarInfoBean> beanList) {
        this.mDataList.addAll(beanList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position + 1 == getItemCount()) {
            return TYPE_FOOTER;
        } else {
            return TYPE_ITEM;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = (MainActivity) parent.getContext();
        LayoutInflater from = LayoutInflater.from(context);
        if (viewType == TYPE_FOOTER) {
            View footer = from.inflate(R.layout.load_more_layout, parent, false);
            return new FooterHolder(footer);
        } else if (viewType == TYPE_ITEM) {
            View item = from.inflate(R.layout.item_product_layout, parent, false);
            return new ItemHolder(item);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemHolder) {
            final ItemHolder itemHolder = (ItemHolder) holder;
            CarInfoBean bean = mDataList.get(position);
            //set data
            Glide.with(context).load(bean.getUserInfo().getHeadimgurl()).into(itemHolder.ivUserHeaderImg);
            itemHolder.tvUserNmae.setText(bean.getUserInfo().getNickname());
            itemHolder.tvCarName.setText(bean.getName() + bean.getYear() + "年");
            itemHolder.tvPrice.setText(bean.getPrice() + "万");
            if ((TextUtils.isEmpty(bean.getContent()))) {
                itemHolder.tvInfo.setVisibility(View.GONE);
            } else {
                itemHolder.tvInfo.setVisibility(View.VISIBLE);
                itemHolder.tvInfo.setText(bean.getContent());
            }

            itemHolder.tvAddress.setText(bean.getProvinceName() + "·" + bean.getCityName());
            itemHolder.tvTime.setText(bean.getAddtime_format());
            //pic
            ViewGroup.LayoutParams layoutParams = itemHolder.gridView.getLayoutParams();
            layoutParams.width = DensityUtils.dp2px(context, 90 * 3 + 8);
            itemHolder.gridView.setLayoutParams(layoutParams);
            if (bean.getPic_img()==null || bean.getPic_img().size()==0) {
                itemHolder.gridView.setVisibility(View.GONE);
            }else {
                itemHolder.gridView.setVisibility(View.VISIBLE);
                final CarPicAdapter adapter = new CarPicAdapter(bean.getPic_img(), bean.getPic_img_count());
                itemHolder.gridView.setAdapter(adapter);
                //准备图片数据
                itemHolder.gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        assembleDataList(itemHolder.gridView);
                        PhotoActivity.startActivity(context, adapter.getThumbList(), position);
                    }
                });
            }


            itemHolder.tvCall.setTag(position);
            itemHolder.tvShare.setTag(position);
            itemHolder.tvCall.setOnClickListener(this);
            itemHolder.tvShare.setOnClickListener(this);
        }
    }

    /**
     * 从第一个完整可见item逆序遍历，如果初始位置为0，则不执行方法内循环
     */
    private void computeBoundsBackward(MyGridView gridView) {
        CarPicAdapter adapter = (CarPicAdapter) gridView.getAdapter();
        for (int i = gridView.getFirstVisiblePosition(); i < adapter.getCount(); i++) {
            View itemView = gridView.getChildAt(i);
            Rect bounds = new Rect();
            if (itemView != null) {
                ImageView thumbView = (ImageView) itemView.findViewById(R.id.imageview);
                thumbView.getGlobalVisibleRect(bounds);
            }
            adapter.getThumbList().get(i).setBounds(bounds);
//            mThumbList.get(i).setBounds(bounds);
        }

    }

    private void assembleDataList(MyGridView gridView) {
        computeBoundsBackward(gridView);
    }

    @Override
    public int getItemCount() {
        if (mDataList != null && mDataList.size() > 0) {
            return mDataList.size() + 1;
        }
        return 0;
    }

    @Override
    public void onClick(View v) {
        int positon = (int) v.getTag();
        CarInfoBean carInfoBean = mDataList.get(positon);
        switch (v.getId()) {
            case R.id.item_tv_call:
                PhoneUtils.call(context, carInfoBean.getPhone());
                break;
            case R.id.item_tv_share:
                if (PackageUtils.isWeixinAvilible(context)) {
//                    MomentBean momentBean = new MomentBean();
//                    momentBean.setInformation(getInformation(mDataList.get(positon)));
//                    DownLoadUtils.getInstance().addObserver(context);
//                    ArrayList<String> list = new ArrayList<>();
//                    for (int i = 0; i < mDataList.get(positon).getPic_img().size(); i++) {
//                        list.add(mDataList.get(positon).getPic_img().get(i).getSavename());
//                    }
//                    DownLoadUtils.getInstance().downLoadPic(context, list, momentBean);
                    showProgress();
                    queryCarInfo(context, String.valueOf(carInfoBean.getId()),
                            String.valueOf(carInfoBean.getUserInfo().getUserid()),
                            getInformation(mDataList.get(positon)));

                } else {
                    TopMessageManager.showError("未安装微信，不能分享！");
                }
                break;
        }
    }

    private void showProgress() {

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("正在查询信息...");
        }

        progressDialog.show();
    }

    private void dissmissProgress() {
        if (progressDialog != null) progressDialog.dismiss();
    }

    /**
     * 查询产品信息
     *
     * @param carid
     * @param userid
     */
    private void queryCarInfo(final Context context, String carid, String userid, final String info) {
        String auth = HttpUtils.getMd5(Constants.USER, Constants.PASS, Constants.TIME);

        CarDetailService service = HttpUtils.getInstance().getRetrofit().create(CarDetailService.class);
        service.getCarInfo(carid, userid, Constants.USER,
                Constants.PASS, Constants.TIME, auth)
                .enqueue(new Callback<CarDetailBean>() {
                    @Override
                    public void onResponse(@NonNull Call<CarDetailBean> call, @NonNull Response<CarDetailBean> response) {
                        CarDetailBean body = response.body();
                        if (body != null) {
                            if (body.getCode() == 0) {
                                Log.w("onResponse", body.getData().getContent());
                                //获取图片url
                                ArrayList<String> urlList = new ArrayList<>();
                                List<CarDetailBean.DataBean.CImagesBean> cImages = body.getData().getCImages();

                                for (CarDetailBean.DataBean.CImagesBean bean :
                                        cImages) {
                                    urlList.add(bean.getSavename());
                                }

                                //获取分享文字内容
                                Intent intent = new Intent();

                                intent.setClass(context, DownPicService.class);
                                intent.putExtra(Constants.KEY_SHARE_METE_DATA, new ShareMeteBean(urlList, info));
                                //开启下载服务
                                context.startService(intent);
                            }
                        } else {
                            Log.d("xxx", "查询车辆信息错误，请手动点击分享！");
                        }
                        dissmissProgress();
                    }

                    @Override
                    public void onFailure(Call<CarDetailBean> call, Throwable t) {
                        Log.w("onFailure", "查询产品信息报错" + t.toString());
                        dissmissProgress();
                    }
                });
    }

    /**
     * 组装要分享的文字内容
     *
     * @param data
     * @return
     */
    private String getInformation(CarInfoBean data) {
        StringBuilder sb = new StringBuilder();
        String content = (String) SpUtils.get(context, Constants.KEY_WECHAT_CONTENT, "");

        synchronized (MainActivity.class) {

            String price = data.getPrice();
            String strPrice = "价格" + data.getPrice() + "万，";
            try {
                if (Double.parseDouble(price) <= 0) {
                    strPrice = "价格面议，";
                }
            } catch (Exception e) {
                strPrice = "价格面议，";
            }

            sb.append("【")
                    .append(data.getName()).append("】")
                    .append(data.getYear()).append("年，")
                    .append(strPrice).append("车辆位于").append(data.getProvinceName().replace("省", "")).append(data.getCityName().replace("市", "")).append("，")
                    .append(TextUtils.isEmpty(data.getContent()) ? "" : data.getContent() + "，")
                    .append("电话：").append(data.getPhone())
                    .append(TextUtils.isEmpty(content) ? "如需分享信息请将你的车辆发布至铲车圈" : content);
        }
        return sb.toString();
    }
//    private String getInformation(CarInfoBean carInfoBean) {
//        StringBuilder sb = new StringBuilder();
//        String content = (String) SpUtils.get(context, Constants.KEY_WECHAT_CONTENT, "");
//
//        sb.append(carInfoBean.getName()).append("，")
//                .append(carInfoBean.getYear()).append("年，价格")
//                .append(carInfoBean.getPrice()).append("万，").append(carInfoBean.getContent())
//                .append("车辆位于").append(carInfoBean.getProvinceName().replace("省", "")).append(carInfoBean.getCityName().replace("市", ""))
//                .append("，电话：").append(carInfoBean.getPhone())
//                .append("，").append(TextUtils.isEmpty(content) ? "如需分享信息请将你的车辆发布至铲车圈" : content);
//        return sb.toString();
//    }

    static class ItemHolder extends RecyclerView.ViewHolder {

        private final ImageView ivUserHeaderImg;
        private final TextView tvUserNmae;
        private final TextView tvCarName;
        private final TextView tvPrice;
        private final MyGridView gridView;
        private final TextView tvAddress;
        private final TextView tvInfo;
        private final TextView tvTime;
        private final View tvCall;
        private final View tvShare;

        public ItemHolder(View itemView) {
            super(itemView);
            ivUserHeaderImg = (ImageView) findViewById(R.id.item_iv_user_header);
            tvUserNmae = (TextView) findViewById(R.id.item_tv_user_name);
            tvCarName = (TextView) findViewById(R.id.item_tv_car_name);
            tvPrice = (TextView) findViewById(R.id.item_tv_car_price);
            gridView = (MyGridView) findViewById(R.id.item_gridview);
            tvAddress = (TextView) findViewById(R.id.item_tv_car_location);
            tvInfo = (TextView) findViewById(R.id.item_tv_car_info);
            tvTime = (TextView) findViewById(R.id.item_tv_publish_time);
            tvCall = findViewById(R.id.item_tv_call);
            tvShare = findViewById(R.id.item_tv_share);
        }

        private View findViewById(int id) {
            return itemView.findViewById(id);
        }
    }

    static class FooterHolder extends RecyclerView.ViewHolder {

        public FooterHolder(View itemView) {
            super(itemView);
        }
    }
}
