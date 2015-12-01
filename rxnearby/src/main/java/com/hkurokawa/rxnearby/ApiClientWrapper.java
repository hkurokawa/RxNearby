package com.hkurokawa.rxnearby;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import java.util.Iterator;

class ApiClientWrapper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private Message publishingMessage;
    private final GoogleApiClient apiClient;
    private ConnectionListener connectionListener;

    public ApiClientWrapper(Context context) {
        apiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.MESSAGES_API)
                .build();
    }

    public void connect() {
        apiClient.connect();
    }

    public void disconnect() {
        if (apiClient.isConnected()) {
            apiClient.disconnect();
        }
    }

    public void subscribe(final MessageListener messageListener, final ResultHandler handler) {
        final ResultCallbackAdapter callbackAdapter = new ResultCallbackAdapter(handler);
        Nearby.Messages.getPermissionStatus(apiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Nearby.Messages.subscribe(apiClient, messageListener).setResultCallback(callbackAdapter);
                } else {
                    handler.onError(status);
                }
            }
        });
    }

    public void unsubscribe(MessageListener messageListener, final ResultHandler handler) {
        Nearby.Messages.unsubscribe(apiClient, messageListener).setResultCallback(new ResultCallbackAdapter(handler));
    }

    public void publish(final Message message, final ResultHandler handler) {
        Nearby.Messages.getPermissionStatus(apiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    if (publishingMessage != null) {
                        unpublish(null);
                    }
                    publishingMessage = message;
                    Nearby.Messages.publish(apiClient, message).setResultCallback(new ResultCallbackAdapter(handler));
                } else {
                    handler.onError(status);
                }
            }
        });
    }

    public void unpublish(final ResultHandler handler) {
        if (publishingMessage != null) {
            Nearby.Messages.unpublish(apiClient, publishingMessage).setResultCallback(new ResultCallbackAdapter(handler));
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (connectionListener != null) {
            connectionListener.onConnected(bundle);
        }
    }

    @Override
    public void onConnectionSuspended(int varl) {
        if (connectionListener != null) {
            connectionListener.onConnectionSuspended(varl);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionListener != null) {
            connectionListener.onConnectionFailed(connectionResult);
        }
    }

    public boolean isConnected() {
        return apiClient.isConnected();
    }

    public void setOnConnectedListener(ConnectionListener listener) {
        connectionListener = listener;
    }

    interface ConnectionListener {
        void onConnected(Bundle bundle);
        void onConnectionSuspended(int varl);
        void onConnectionFailed(ConnectionResult connectionResult);
    }

    interface ResultHandler {
        void onSuccess();
        void onError(Status status);
    }

    private static class ResultCallbackAdapter implements ResultCallback<Status> {
        private final ResultHandler handler;

        public ResultCallbackAdapter(ResultHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onResult(Status status) {
            if (status.isSuccess()) {
                if (handler != null) {
                    handler.onSuccess();
                }
            } else {
                if (handler != null) {
                    handler.onError(status);
                }
            }
        }
    }
}
