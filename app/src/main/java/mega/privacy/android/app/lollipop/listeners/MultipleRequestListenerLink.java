package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class MultipleRequestListenerLink implements MegaRequestListenerInterface {

    Context context;
    int counter = 0;
    int error = 0;
    int max_items = 0;
    int success = 0;
    String message;
    int elementToImport = 0;
    public static final int FILE_LINK = 1;
    public static final int FOLDER_LINK = 2;

    public MultipleRequestListenerLink( Context context, int counter, int max_items, int elementToImport) {
        super();
        this.context = context;
        this.counter = counter;
        this.max_items = max_items;
        this.elementToImport = elementToImport;
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
                    if(success == 0){
                        counter = -1;
                        if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
                            //first error is OVERQUOTA
                            if(elementToImport == FOLDER_LINK){
                                ((FolderLinkActivityLollipop) context).errorOverquota();
                            }else if(elementToImport == FILE_LINK){
                                ((FileLinkActivityLollipop) context).errorOverquota();
                            }
                        }
                        else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){

                            if(elementToImport == FOLDER_LINK){
                                ((FolderLinkActivityLollipop) context).errorPreOverquota();
                            }else if(elementToImport == FILE_LINK){
                                ((FileLinkActivityLollipop) context).errorPreOverquota();
                            }
                        }
                    }

                }else{
                    success ++;
                }

                if(counter == 0){
                    if(error == max_items){
                        //all copies failed
                        message = context.getString(R.string.context_no_copied);

                        if(elementToImport == FOLDER_LINK){
                            ((FolderLinkActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, message);
                        }else if(elementToImport == FILE_LINK){
                            ((FileLinkActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, message);
                        }
                    }else{
                        logDebug("OK");
                        if(elementToImport == FOLDER_LINK){
                            ((FolderLinkActivityLollipop) context).successfulCopy();
                        }else if(elementToImport == FILE_LINK){
                            ((FileLinkActivityLollipop) context).successfulCopy();
                        }
                    }
                }
                break;
            }
            default:
                break;
        }
    }
}
