package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class FileContactMultipleRequestListener implements MegaRequestListenerInterface {

    Context context;

    public FileContactMultipleRequestListener(int action, Context context) {
        super();
        this.actionListener = action;
        this.context = context;
    }

    int counter = 0;
    int error = 0;
    int max_items = 0;
    int actionListener = -1;
    String message;

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        counter++;
        if(counter>max_items){
            max_items=counter;
        }
        log("Counter on RequestStart: "+counter);
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        counter--;
        if (e.getErrorCode() != MegaError.API_OK){
            error++;
        }
        int requestType = request.getType();
        log("Counter on RequestFinish: "+counter);
        log("Error on RequestFinish: "+error);
//			MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
//			if(node!=null){
//				log("onRequestTemporaryError: "+node.getName());
//			}
        if(counter==0) {
            switch (requestType) {
                case MegaRequest.TYPE_SHARE: {
                    if (actionListener == Constants.MULTIPLE_REMOVE_CONTACT_SHARED_FOLDER) {
                        log("share request finished");
                        if (error > 0) {
                            message = context.getString(R.string.number_correctly_removed_from_shared, max_items - error) + context.getString(R.string.number_incorrectly_removed_from_shared, error);
                        } else {
                            message = context.getString(R.string.number_correctly_removed_from_shared, max_items);
                        }
                    }
                    break;
                }
            }
            ((FileContactListActivityLollipop) context).showSnackbar(message);
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        log("Counter on onRequestTemporaryError: "+counter);
    }

    private static void log(String log) {
        Util.log("FileContactMultipleRequestListener", log);
    }
}
