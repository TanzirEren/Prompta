package com.prompta.tg.telegram;

import com.google.gson.annotations.SerializedName;

/** sendMessage API এর request body */
public class SendMessageRequest {

    @SerializedName("chat_id")
    public String chatId;

    @SerializedName("text")
    public String text;

    @SerializedName("parse_mode")
    public String parseMode = "Markdown";

    @SerializedName("disable_web_page_preview")
    public boolean disablePreview = true;

    public SendMessageRequest(String chatId, String text) {
        this.chatId = chatId;
        this.text   = text;
    }
}
