package com.hkurokawa.rxnearby;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.messages.Message;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

class MessagePublishOperator implements Observable.Operator<PublishResult, Message> {
    private final Context context;

    public MessagePublishOperator(Context context) {
        this.context = context;
    }

    @Override
    public Subscriber<? super Message> call(Subscriber<? super PublishResult> child) {
        final ApiClientWrapper apiClient = new ApiClientWrapper(this.context);
        MessagePublishSubscriber parent = new MessagePublishSubscriber(child, apiClient);
        return parent;
    }

    private class MessagePublishSubscriber extends Subscriber<Message> {
        private final Subscriber<? super PublishResult> child;
        private final ApiClientWrapper apiClient;

        public MessagePublishSubscriber(Subscriber<? super PublishResult> child, final ApiClientWrapper apiClient) {
            super(child);
            this.apiClient = apiClient;
            this.child = child;
            init();
        }

        private void init() {
            add(new Subscription() {
                @Override
                public void unsubscribe() {
                    apiClient.unpublish();
                    apiClient.disconnect();
                }

                @Override
                public boolean isUnsubscribed() {
                    return apiClient.isConnected();
                }
            });
        }

        @Override
        public void onNext(final Message message) {
            if (apiClient.isConnected()) {
                publish(message);
            } else {
                apiClient.setOnConnectedListener(new ApiClientWrapper.ConnectionListener() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        publish(message);
                    }

                    @Override
                    public void onConnectionSuspended(int causeCode) {
                        Exceptions.throwOrReport(new ApiConnectionSuspendedException(causeCode), MessagePublishSubscriber.this);
                    }

                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Exceptions.throwOrReport(new ApiConnectionFailedException(connectionResult), MessagePublishSubscriber.this);
                    }
                });
                apiClient.connect();
            }
        }

        @Override
        public void onCompleted() {
            child.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        private void publish(final Message message) {
            apiClient.publish(message, new ApiClientWrapper.ResultHandler() {
                @Override
                public void onSuccess() {
                    child.onNext(new PublishResult(message));
                }

                @Override
                public void onError(Status status) {
                    Exceptions.throwOrReport(new ApiStatusException(status), MessagePublishSubscriber.this, message);
                }
            });
        }
    }
}
