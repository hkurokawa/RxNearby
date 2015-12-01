package com.hkurokawa.rxnearby;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.messages.Message;

import rx.Observable;
import rx.functions.Func1;

public final class RxNearby {
    public static Observable<Message> subscribe(Context context, final Func1<Status, Observable<Void>> statusResolver) {
        return Observable
                .create(new MessageSubscribeOnSubscribe(context))
                .retryWhen(buildStatusResolutionHandler(statusResolver));
    }

    public static Observable<PublishResult> publish(Context context, final Observable<Message> messageObservable, Func1<Status, Observable<Void>> statusResolver) {
        return messageObservable
                .lift(new MessagePublishOperator(context))
                .retryWhen(buildStatusResolutionHandler(statusResolver));
    }

    private static Func1<Observable<? extends Throwable>, Observable<?>> buildStatusResolutionHandler(final Func1<Status, Observable<Void>> statusResolver) {
        return new Func1<Observable<? extends Throwable>, Observable<?>>() {
            @Override
            public Observable<Void> call(Observable<? extends Throwable> observable) {
                return observable
                        .flatMap(new Func1<Throwable, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(Throwable throwable) {
                                if (throwable instanceof ApiStatusException && ((ApiStatusException) throwable).getStatus().hasResolution()) {
                                    return statusResolver.call(((ApiStatusException) throwable).getStatus());
                                } else {
                                    return Observable.error(throwable);
                                }
                            }
                        });
            }
        };
    }
}
