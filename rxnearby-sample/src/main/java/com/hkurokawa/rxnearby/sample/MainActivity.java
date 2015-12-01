package com.hkurokawa.rxnearby.sample;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.messages.Message;
import com.hkurokawa.rxnearby.PublishResult;
import com.hkurokawa.rxnearby.RxNearby;

import java.io.UnsupportedEncodingException;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final int REQ_RESOLVE_MESSEAGE_API_ERROR = 1024;
    private BehaviorSubject<Message> sendMessageSubject;
    private PublishSubject<Void> retry = PublishSubject.create();
    private Subscription subSubscription;
    private Subscription pubSubscription;
    private ViewGroup contentView;
    private int messageID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    final String msg = "Hi! This is #" + (messageID++) + " message.";
                    sendMessageSubject.onNext(new Message(msg.getBytes("UTF-8")));
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Failed to serialize message.", e);
                }
            }
        });

        contentView = (ViewGroup) findViewById(R.id.content);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final Func1<Status, Observable<Void>> statusResolver = new Func1<Status, Observable<Void>>() {
            @Override
            public Observable<Void> call(Status status) {
                try {
                    Log.i(TAG, "Trying to resolve an error: " + status);
                    status.startResolutionForResult(MainActivity.this, REQ_RESOLVE_MESSEAGE_API_ERROR);
                    return retry;
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Failed to resolve API status", e);
                    throw new RuntimeException(e);
                }
            }
        };

        sendMessageSubject = BehaviorSubject.create();
        pubSubscription = RxNearby
                .publish(this, sendMessageSubject, statusResolver)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<PublishResult>() {
                    @Override
                    public void call(PublishResult publishResult) {
                        try {
                            final TextView textView = new TextView(MainActivity.this);
                            textView.setText("Sent: [" + new String(publishResult.message.getContent(), "UTF-8") + "]");
                            contentView.addView(textView);
                        } catch (UnsupportedEncodingException e) {
                            Log.e(MainActivity.class.getName(), "Failed to unserialize the sent message.", e);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "Failed to publish a message: " + throwable.getMessage(), throwable);
                    }
                });

        subSubscription = RxNearby.subscribe(this, statusResolver)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Message>() {
                    @Override
                    public void onNext(Message message) {
                        try {
                            TextView textView = new TextView(MainActivity.this);
                            textView.setText("Received: [" + new String(message.getContent(), "UTF-8") + "]");
                            contentView.addView(textView);
                        } catch (UnsupportedEncodingException e) {
                            Log.e(MainActivity.class.getName(), "Failed to unserialize the received message.", e);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Failed to subscribe: " + e.getMessage(), e);
                    }

                    @Override
                    public void onCompleted() {
                        Log.i(MainActivity.class.getName(), "Subscribe completed.");
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();

        pubSubscription.unsubscribe();
        if (subSubscription != null) {
            subSubscription.unsubscribe();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_RESOLVE_MESSEAGE_API_ERROR:
                if (resultCode == RESULT_OK) {
                    // Permission granted or error resolved successfully then we proceed
                    // with publish and subscribe..
                    if (retry != null && !retry.hasCompleted()) {
                        retry.onNext(null);
                        retry.onCompleted();
                    }
                } else {
                    // This may mean that user had rejected to grant nearby permission.
                    Log.e(TAG, "Failed to resolve error with code: " + resultCode);
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
