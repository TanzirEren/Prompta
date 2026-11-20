package com.prompta.tg.telegram;

import com.google.gson.annotations.SerializedName;

/** Telegram Bot API — sendMessage / editMessageText response এর message object */
public class TelegramMessage {

    @SerializedName("message_id")
    public long messageId;

    @SerializedName("text")
    public String text;

    @SerializedName("date")
    public long date;

    @SerializedName("chat")
    public Chat chat;

    public static class Chat {
        @SerializedName("id")
        public long id;
    }
}
