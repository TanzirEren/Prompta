package com.prompta.tg.telegram;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * TelegramClient — Retrofit singleton।
 * reset() call করলে নতুন instance তৈরি হয়।
 */
public class TelegramClient {

    private static final String BASE_URL = "https://api.telegram.org/";

    private static TelegramApi instance;

    public static TelegramApi get() {
        if (instance == null) {
            instance = buildInstance();
        }
        return instance;
    }

    /** Token পরিবর্তন হলে singleton reset করুন */
    public static void reset() {
        instance = null;
    }

    private static TelegramApi buildInstance() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(TelegramApi.class);
    }

    private TelegramClient() {}
}
