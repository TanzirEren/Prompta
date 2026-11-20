package com.prompta.tg.telegram;

import com.google.gson.annotations.SerializedName;

/** Telegram Bot API এর সব response এর wrapper */
public class TelegramResponse {

    @SerializedName("ok")
    public boolean ok;

    @SerializedName("result")
    public TelegramMessage result;

    @SerializedName("description")
    public String description; // error message যদি ok=false হয়
}
