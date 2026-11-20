package com.prompta.tg.telegram;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit interface for Telegram Bot API.
 * Base URL: https://api.telegram.org/bot{token}/
 */
public interface TelegramApi {

    /** নতুন message পাঠাও channel এ */
    @POST("bot{token}/sendMessage")
    Call<TelegramResponse> sendMessage(
            @Path("token") String token,
            @Body SendMessageRequest body
    );

    /** বিদ্যমান message edit করো */
    @POST("bot{token}/editMessageText")
    Call<TelegramResponse> editMessage(
            @Path("token") String token,
            @Body EditMessageRequest body
    );

    /** message delete করো */
    @POST("bot{token}/deleteMessage")
    Call<TelegramResponse> deleteMessage(
            @Path("token") String token,
            @Body DeleteMessageRequest body
    );

    /** Connection test — bot info নিয়ে আসো */
    @POST("bot{token}/getMe")
    Call<TelegramResponse> getMe(
            @Path("token") String token
    );
}
