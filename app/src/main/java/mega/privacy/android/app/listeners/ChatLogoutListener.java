package mega.privacy.android.app.listeners;

import java.lang.ref.WeakReference;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.domain.usecase.ResetSdkLoggerUseCase;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;

public class ChatLogoutListener implements MegaChatRequestListenerInterface {
    private ResetSdkLoggerUseCase resetSdkLoggerUseCase;
    private WeakReference<ChatLogoutCallback> chatLogoutCallback;

    public interface ChatLogoutCallback {
        void chatLogoutFinish();
    }

    public ChatLogoutListener(ResetSdkLoggerUseCase resetSdkLoggerUseCase) {
        this.resetSdkLoggerUseCase = resetSdkLoggerUseCase;
    }

    public ChatLogoutListener(ResetSdkLoggerUseCase resetSdkLoggerUseCase, ChatLogoutCallback chatLogoutCallback) {
        this.resetSdkLoggerUseCase = resetSdkLoggerUseCase;
        this.chatLogoutCallback = new WeakReference<>(chatLogoutCallback);
    }

    @Override
    public void onRequestStart(MegaChatApiJava megaChatApiJava, MegaChatRequest megaChatRequest) {
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava megaChatApiJava, MegaChatRequest megaChatRequest) {
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_LOGOUT) return;

        MegaApplication.getInstance().disableMegaChatApi();
        resetSdkLoggerUseCase.invoke();

        ChatLogoutCallback callback = chatLogoutCallback.get();
        if (callback != null) {
            callback.chatLogoutFinish();
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava megaChatApiJava, MegaChatRequest megaChatRequest, MegaChatError megaChatError) {
    }
}