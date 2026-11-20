package com.prompta.tg.telegram;

import com.google.gson.annotations.SerializedName;

/** editMessageText API এর request body */
public class EditMessageRequest {

    @SerializedName("chat_id")
    public String chatId;

    @SerializedName("message_id")
    public long messageId;

    @SerializedName("text")
    public String text;

    @SerializedName("parse_mode")
    public String parseMode = "Markdown";

    @SerializedName("disable_web_page_preview")
    public boolean disablePreview = true;

    public EditMessageRequest(String chatId, long messageId, String text) {
        this.chatId    = chatId;
        this.messageId = messageId;
        this.text      = text;
    }
}
