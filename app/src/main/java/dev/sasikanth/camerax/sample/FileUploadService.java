package dev.sasikanth.camerax.sample;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface FileUploadService {
    @Multipart
    @POST("/im_size")
    Call<ResponseBody> upload(
            @Part MultipartBody.Part file
//            @Part("lat") RequestBody lat,
//            @Part("lon") RequestBody lon
    );
}