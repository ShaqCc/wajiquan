package com.wjq.share.service;

import com.wjq.share.bean.CarDetailBean;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/****************************************
 * 功能说明:  查询车辆信息
 *
 * Author: Created by bayin on 2017/8/31.
 ****************************************/

public interface CarDetailService {
    @GET("car/info")
    Call<CarDetailBean> getCarInfo(@Query("carid") String carid, @Query("userid") String userid,
                                   @Header("user") String user, @Header("pass") String pass,
                                   @Header("time") String time, @Header("auth") String auth);
}
