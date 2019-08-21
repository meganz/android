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
    
    public interface RequestCompletedCallback{
        void onRequestCompleted(String message);
    }
    
    private Context context;
    private int counter, error, max_items, actionType;
    private String message;
    private RequestCompletedCallback callback;
    
    
    public FileContactMultipleRequestListener(int action,Context context,RequestCompletedCallback callback) {
        super();
        this.actionType = action;
        this.context = context;
        this.callback = callback;
    }
    
    @Override
    public void onRequestStart(MegaApiJava api,MegaRequest request) {
        counter++;
        if (counter > max_items) {
            max_items = counter;
        }
        log("Counter on RequestStart: " + counter);
    }
    
    @Override
    public void onRequestUpdate(MegaApiJava api,MegaRequest request) {
    
    }
    
    @Override
    public void onRequestFinish(MegaApiJava api,MegaRequest request,MegaError e) {
        counter--;
        if (e.getErrorCode() != MegaError.API_OK) {
            error++;
        }
        int requestType = request.getType();
        log("Counter on RequestFinish: " + counter + ", Error on RequestFinish: " + error );
        if (counter == 0 && requestType == MegaRequest.TYPE_SHARE) {
            if (actionType == Constants.MULTIPLE_REMOVE_CONTACT_SHARED_FOLDER) {
                log("share request finished");
                if (error > 0) {
                    message = context.getString(R.string.number_correctly_removed_from_shared,max_items - error) + context.getString(R.string.number_incorrectly_removed_from_shared,error);
                } else {
                    message = context.getString(R.string.number_correctly_removed_from_shared,max_items);
                }
            } else if (actionType == Constants.MULTIPLE_CHANGE_PERMISSION) {
                if (error > 0) {
                    message = context.getString(R.string.number_permission_correctly_changed_from_shared,max_items - error) + context.getString(R.string.number_permission_incorrectly_changed_from_shared,error);
                } else {
                    message = context.getString(R.string.number_permission_correctly_changed_from_shared,max_items);
                }
            } else {
                message = context.getString(R.string.context_correctly_shared);
            }
            if(callback != null){
                callback.onRequestCompleted(message);
            }
        }
    }
    
    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {
        log("Counter on onRequestTemporaryError: " + counter);
    }
    
    private static void log(String log) {
        Util.log("FileContactMultipleRequestListener",log);
    }
}
