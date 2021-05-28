package com.example.geogram.utility;

import com.example.geogram.fcm.FirebaseCloudMessage;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

public interface FCM {


    @POST("send")
    //post requet = @POST("send") this is a requirment for retrofit
    /**
    * Call is a object and ResponseBody data type is the generalised data type
    * we don't really care with the request as long as successful*/
    Call<ResponseBody> send( //send is a name for this interface method
                             @HeaderMap Map<String, String> headers, //@HeaderMap is a collection of headers
                             @Body FirebaseCloudMessage message
    );
}
