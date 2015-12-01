package com.hkurokawa.rxnearby;

import com.google.android.gms.common.ConnectionResult;

public class ApiConnectionFailedException extends Exception {
    private final ConnectionResult result;

    public ApiConnectionFailedException(ConnectionResult result) {
        super("Failed to connect with Google API: " + result);
        this.result = result;
    }

    public ConnectionResult getResult() {
        return result;
    }
}
