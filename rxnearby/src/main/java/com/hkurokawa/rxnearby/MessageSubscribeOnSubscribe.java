package com.hkurokawa.rxnearby;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

class MessageSubscribeOnSubscribe implements Observable.OnSubscribe<Message> {
    private final Context context;

    public MessageSubscribeOnSubscribe(Context context) {
        this.context = context;
    }

    @Override
    public void call(final Subscriber<? super Message> subscriber) {
        final MessageListener listener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(message);
                }
            }
        };
        final ApiClientWrapper.ResultHandler handler = new ApiClientWrapper.ResultHandler() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(Status status) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(new ApiStatusException(status));
                }
            }
        };
        final ApiClientWrapper apiClient = new ApiClientWrapper(context) {
            @Override
            public void onConnected(Bundle bundle) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onStart();
                    this.subscribe(listener, handler);
                }
            }
        };

        subscriber.add(new Subscription() {
            @Override
            public void unsubscribe() {
                apiClient.unsubscribe(listener, handler);
                apiClient.disconnect();
            }

            @Override
            public boolean isUnsubscribed() {
                return apiClient.isConnected();
            }
        });
        apiClient.connect();
    }
}
