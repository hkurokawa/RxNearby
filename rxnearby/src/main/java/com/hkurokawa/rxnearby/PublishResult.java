package com.hkurokawa.rxnearby;

import com.google.android.gms.nearby.messages.Message;

public class PublishResult {
    public final Message message;

    public PublishResult(Message message) {
        this.message = message;
    }
}
