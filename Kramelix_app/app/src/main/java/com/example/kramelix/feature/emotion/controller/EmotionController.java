package com.example.kramelix.feature.emotion.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.example.kramelix.BuildConfig;

public class EmotionController {

    private static final String BASE_URL = BuildConfig.SERVER_URL;
    private final OkHttpClient client = new OkHttpClient();

    private String answer;

    public interface EmotionCallback {
        void onSuccess(String label);
        void onError(String message);
    }

    public void predictEmotion(File wav, EmotionCallback callback) throws IOException{
        RequestBody fileBody = RequestBody.create(wav, MediaType.parse("audio/wav"));

        MultipartBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("audio", wav.getName(), fileBody).build();

        Request request = new Request.Builder().url(BASE_URL + "/predict").post(requestBody).build();

        client.newCall(request).enqueue(new Callback(){
            @Override
            public void onFailure(Call call, IOException e){
                callback.onError("Network error: " + e.getMessage());
                return;
            }

            public void onResponse(Call call, Response response) throws IOException{
                if (!response.isSuccessful()){
                    callback.onError("Server error: " + response.code());
                    return;
                }
                try {
                    String answer = response.body().string();
                    answer = answer.replaceAll("\"", "");
                    callback.onSuccess(answer);
                }
                catch(Exception e){
                    callback.onError("Error: " + e.getMessage());
                }
            }

        });
    }
}
