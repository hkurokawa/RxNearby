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
            this.apiClient = apiClient;
            this.child = child;
            init();
        }

        private void init() {
            child.add(this);
            child.add(new Subscription() {
                @Override
                public void unsubscribe() {
                    apiClient.unpublish(new ApiClientWrapper.ResultHandler() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(Status status) {
                            child.onError(new ApiStatusException(status));
                        }
                    });
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
                        child.onError(new ApiConnectionSuspendedException(causeCode));
                    }

                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        child.onError(new ApiConnectionFailedException(connectionResult));
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
                    child.onError(new ApiStatusException(status));
                }
            });
        }
    }
}
