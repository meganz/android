package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.utils.Util;

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
        log("doInBackground-Async Task CheckOfflineNodesTask");

        ArrayList<MegaOffline> offlineNodes = dbH.getOfflineFiles();

        File file=new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR);

        if(file.exists()){

            for(int i=0; i<offlineNodes.size();i++){
                MegaOffline mOff = offlineNodes.get(i);
                if(mOff.getOrigin()==MegaOffline.INCOMING){
                    File fileToCheck=new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR+ "/" + mOff.getHandleIncoming() + mOff.getPath()+ mOff.getName());
                    log("Check the INCOMING file: "+fileToCheck.getAbsolutePath());
                    if(!fileToCheck.exists()){
                        log("The INCOMING file NOT exists!");
                        //Remove from the DB
                        int removed = dbH.deleteOfflineFile(mOff);
                        log("INCOMING File removed: "+removed);
                    }
                    else{
                        log("The INCOMING file exists!");
                    }
                }
                else if(mOff.getOrigin()==MegaOffline.INBOX){
                    File fileToCheck=new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR+ "/in" + mOff.getPath()+ mOff.getName());
                    log("Check the INCOMING file: "+fileToCheck.getAbsolutePath());
                    if(!fileToCheck.exists()){
                        log("The INCOMING file NOT exists!");
                        //Remove from the DB
                        int removed = dbH.deleteOfflineFile(mOff);
                        log("INCOMING File removed: "+removed);
                    }
                    else{
                        log("The INCOMING file exists!");
                    }
                }
                else{
                    File fileToCheck=new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR+ mOff.getPath()+ mOff.getName());
                    log("Check the file: "+fileToCheck.getAbsolutePath());
                    if(!fileToCheck.exists()){
                        log("The file NOT exists!");
                        //Remove from the DB
                        int removed = dbH.deleteOfflineFile(mOff);
                        log("File removed: "+removed);
                    }
                    else{
                        log("The file exists!");
                    }
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
                        log("Delete the empty folder: "+mOff.getName());
                        dbH.deleteOfflineFile(mOff);
                        if(mOff.getOrigin()==MegaOffline.INCOMING){
                            File folderToDelete = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR+ "/" + mOff.getHandleIncoming() + mOff.getPath()+ mOff.getName());
                            try {
                                Util.deleteFolderAndSubfolders(context, folderToDelete);
                            } catch (IOException e) {
                                log("IOException incoming mOff");
                                e.printStackTrace();
                            }
                        }
                        if(mOff.getOrigin()==MegaOffline.INBOX){
                            File folderToDelete = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR+ "/in" + mOff.getPath()+ mOff.getName());
                            try {
                                Util.deleteFolderAndSubfolders(context, folderToDelete);
                            } catch (IOException e) {
                                log("IOException incoming mOff");
                                e.printStackTrace();
                            }
                        }
                        else{
                            File folderToDelete = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR+ mOff.getPath()+ mOff.getName());
                            try {
                                Util.deleteFolderAndSubfolders(context, folderToDelete);
                            } catch (IOException e) {
                                log("IOException NOT incoming mOff");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

        }
        else{
            //Delete the DB if NOT empty
            if(offlineNodes.size()>0){
                //Delete the content
                log("Clear Offline TABLE");
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
    public static void log(String message) {
    Util.log("CheckOfflineNodesTask", message);
}
}
