package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class MultipleRequestListenerLink implements MegaRequestListenerInterface {

    Context context;
    int counter = 0;
    int error = 0;
    int max_items = 0;
    int success = 0;
    String message;

    public MultipleRequestListenerLink( Context context, int counter, int max_items) {
        super();
        this.context = context;
        this.counter = counter;
        this.max_items = max_items;
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {}

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {}

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {}

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        int requestType = request.getType();
        switch (requestType) {
            case MegaRequest.TYPE_COPY: {
                counter --;
                if (e.getErrorCode() != MegaError.API_OK) {
                    error ++;
                    if((success == 0) && (e.getErrorCode()==MegaError.API_EOVERQUOTA)){
                        //first error is OVERQUOTA
                        counter = -1;
                        ((FolderLinkActivityLollipop) context).errorOverquota();
                    }
                }else{
                    success ++;
                }

                if(counter == 0){
                    if(error == max_items){
                        //all copies failed
                        message = context.getString(R.string.context_no_copied);
                        ((FolderLinkActivityLollipop) context).showSnackbar(message);

                    }else{
                        log("OK");
                        ((FolderLinkActivityLollipop) context).successfulCopy();
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    private static void log(String log) {
        Util.log("MultipleRequestListenerLink", log);
    }
}
