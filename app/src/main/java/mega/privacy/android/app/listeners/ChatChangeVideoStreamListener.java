package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.utils.VideoCaptureUtils;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaError;

public class ChatChangeVideoStreamListener extends ChatBaseListener {

    public ChatChangeVideoStreamListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_CHANGE_VIDEO_STREAM) {
            return;
        }

        if (e.getErrorCode() == MegaError.API_OK) {
            VideoCaptureUtils.setIsVideoAllowed(true);
        }
    }
}
