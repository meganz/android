package mega.privacy.android.app.listeners;

import android.content.Context;

import androidx.annotation.NonNull;

import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;

public class ChatBaseListener implements MegaChatRequestListenerInterface {

    protected Context context;

    public ChatBaseListener(Context context) {
        this.context = context;
    }

    @Override
    public void onRequestStart(@NonNull MegaChatApiJava api, @NonNull MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(@NonNull MegaChatApiJava api, @NonNull MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(@NonNull MegaChatApiJava api, @NonNull MegaChatRequest request, @NonNull MegaChatError e) {

    }

    @Override
    public void onRequestTemporaryError(@NonNull MegaChatApiJava api, @NonNull MegaChatRequest request, @NonNull MegaChatError e) {

    }
}
