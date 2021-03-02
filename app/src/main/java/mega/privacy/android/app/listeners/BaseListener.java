package mega.privacy.android.app.listeners;

import android.content.Context;

import androidx.annotation.NonNull;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class BaseListener implements MegaRequestListenerInterface {

    protected Context context;
    protected DatabaseHandler dBH;

    public BaseListener(Context context) {
        this.context = context;
        dBH = MegaApplication.getInstance().getDbH();
    }

    @Override
    public void onRequestStart(@NonNull MegaApiJava api, @NonNull MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(@NonNull MegaApiJava api, @NonNull MegaRequest request) {

    }

    @Override
    public void onRequestFinish(@NonNull MegaApiJava api, @NonNull MegaRequest request,
                                @NonNull MegaError e) {

    }

    @Override
    public void onRequestTemporaryError(@NonNull MegaApiJava api, @NonNull MegaRequest request,
                                        @NonNull MegaError e) {

    }
}
