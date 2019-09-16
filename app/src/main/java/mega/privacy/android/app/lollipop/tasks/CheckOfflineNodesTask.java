package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;

/*
	 * Background task to verify the offline nodes
	 */
public class CheckOfflineNodesTask extends AsyncTask<String, Void, String> {
    Context context;
    DatabaseHandler dbH;

    public CheckOfflineNodesTask(Context context){
        this.context = context;
        dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
    }

    @Override
    protected String doInBackground(String... params) {
        LogUtil.logDebug("doInBackground-Async Task CheckOfflineNodesTask");

        ArrayList<MegaOffline> offlineNodes = dbH.getOfflineFiles();

        File file = getOfflineFolder(context, OFFLINE_DIR);

        if(isFileAvailable(file)){

            for(int i=0; i<offlineNodes.size();i++){
                MegaOffline mOff = offlineNodes.get(i);
                File fileToCheck = getOfflineFile(context, mOff);
                if (!isFileAvailable(fileToCheck)) {
                    int removed = dbH.deleteOfflineFile(mOff);
                    LogUtil.logDebug("File removed: " + removed);
                } else {
                    LogUtil.logDebug("The file exists!");
                }
            }
            //Check no empty folders
            offlineNodes = dbH.getOfflineFiles();
            for(int i=0; i<offlineNodes.size();i++){
                MegaOffline mOff = offlineNodes.get(i);
                //Get if its folder
                if(mOff.isFolder()){
                    ArrayList<MegaOffline> children = dbH.findByParentId(mOff.getId());
                    if(children.size()<1){
                        LogUtil.logDebug("Delete the empty folder: " + mOff.getName());
                        dbH.deleteOfflineFile(mOff);
                        File folderToDelete = getOfflineFile(context, mOff);
                        try {
                            deleteFolderAndSubfolders(context, folderToDelete);
                        } catch (IOException e) {
                            LogUtil.logError("IOException mOff", e);
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
        else{
            //Delete the DB if NOT empty
            if(offlineNodes.size()>0){
                //Delete the content
                LogUtil.logDebug("Clear Offline TABLE");
                dbH.clearOffline();
            }
        }

        return null;
    }

//		@Override
//        protected void onPostExecute(String result) {
//			log("onPostExecute -Async Task CheckOfflineNodesTask");
//			//update the content label of the Rubbish Bin Fragment
//			if(rbFLol!=null){
//					rbFLol.setContentText();
//			}
//        }
}
