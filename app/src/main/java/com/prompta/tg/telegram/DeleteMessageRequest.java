package com.prompta.tg.telegram;

import com.google.gson.annotations.SerializedName;

/** deleteMessage API এর request body */
public class DeleteMessageRequest {

    @SerializedName("chat_id")
    public String chatId;

    @SerializedName("message_id")
    public long messageId;

    public DeleteMessageRequest(String chatId, long messageId) {
        this.chatId    = chatId;
        this.messageId = messageId;
    }
}
