package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.OpenLinkActivity;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;

public class ConnectListener extends ChatBaseListener {

    public ConnectListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_CONNECT) return;

        if (context instanceof OpenLinkActivity) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                ((OpenLinkActivity) context).finishAfterConnect();
            } else {
                ((OpenLinkActivity) context).setError(context.getString(R.string.error_chat_link_init_error));
            }
        }
    }
}
