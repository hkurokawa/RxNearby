package com.hkurokawa.rxnearby;

import com.google.android.gms.common.api.GoogleApiClient;

public class ApiConnectionSuspendedException extends Exception {
    public static final int CAUSE_SERVICE_DISCONNECTED = GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED;
    public static final int CAUSE_NETWORK_LOST = GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST;
    private final int causeCode;

    public ApiConnectionSuspendedException(int causeCode) {
        super("Connection suspended. cause=" + causeCode);
        this.causeCode = causeCode;
    }

    public int getCauseCode() {
        return causeCode;
    }
}