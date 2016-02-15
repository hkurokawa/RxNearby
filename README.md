# RxNearby
[Nearby](https://developers.google.com/nearby/) handling APIs for Android Apps using [RxJava](https://github.com/ReactiveX/RxJava)

# Download
In your app build.gradle, add

```gradle
dependencies {
    compile 'com.hkurokawa.rxnearby:rxnearby:1.0.0'
}
```

# Usage
## Nearby Messages
### Subscribe
You can watch a sequence of the received messages as an `Observable`.
```java
RxNearby.subscribe(this, statusResolver)
        .subscribe(new Action1<Message>() {
          @Override
          public void call(Message message) {
            // do something
          }
        });
```

Note you have to provide how to resolve a resolvable status (which means `status.hasResolution()` returns `true`) is returned during a sequence of Nearby API calls with the second argument. With this argument, you can specify when the retrial of the subscription should be executed. Internally, it uses `Observable.retryWhen()` method. See [ReactiveX - Retry operator](http://reactivex.io/documentation/operators/retry.html) for more information.

```java
final Func1<Status, Observable<Void>> statusResolver = new Func1<Status, Observable<Void>>() {
  @Override
  public Observable<Void> call(Status status) {
    status.startResolutionForResult(MainActivity.this, REQ_RESOLVE_MESSEAGE_API_ERROR);
    return retry;
  }
};
```

### Publish
You have to prepare an `Observable` which emits `Message` events. Everytime it emits an event, RxNearby Message Publish API is called. 
```java
RxNearby.publish(this, sendMessageSubject, statusResolver)
        .subscribe(new Action1<PublishResult>() {
          @Override
          public void call(PublishResult publishResult) {
            // do something
          }
        });
```

## Nearby Connections
T. B. D.

# Sample
A sample app. to do a simple publish/subject task is under `/rxnearby-sample`

![rxnearby-sample](https://cloud.githubusercontent.com/assets/6446183/11506865/c5dca7d8-9894-11e5-8d0c-57952e299bd7.gif)

# License
```
Copyright (C) 2015 Hiroshi Kurokawa

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
