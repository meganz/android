package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static nz.mega.sdk.MegaApiJava.*;

public class GetAttrUserListener extends BaseListener {

    public GetAttrUserListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_GET_ATTR_USER) return;

        switch (request.getParamType()) {
            case USER_ATTR_MY_CHAT_FILES_FOLDER:
                if (context instanceof FileExplorerActivityLollipop) {
                    FileExplorerActivityLollipop fileExplorerActivityLollipop = (FileExplorerActivityLollipop) context;

                    if (e.getErrorCode() == MegaError.API_OK) {
                        fileExplorerActivityLollipop.setMyChatFilesNode(api.getNodeByHandle(request.getNodeHandle()));
                        fileExplorerActivityLollipop.checkIfFilesExistsInMEGA();
                    } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                        api.createFolder(context.getString(R.string.my_chat_files_folder), api.getRootNode(), new CreateFolderListener(fileExplorerActivityLollipop, true));
                    }
                }
            break;
        }
    }
}
