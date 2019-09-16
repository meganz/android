package mega.privacy.android.app.lollipop.tasks;


import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.lollipop.listeners.ContactNameListener;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaUser;

/*
	 * Background task to fill the DB with the contact info the first time
	 */
public class FillDBContactsTask extends AsyncTask<String, Void, String> {
    Context context;
    DatabaseHandler dbH;
    MegaApiAndroid megaApi;

    public FillDBContactsTask(Context context){
        this.context = context;
        dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    protected String doInBackground(String... params) {
        LogUtil.logDebug("doInBackground-Async Task FillDBContactsTask");

        ArrayList<MegaUser> contacts = megaApi.getContacts();

        ContactNameListener listener = new ContactNameListener(context);

        for(int i=0; i<contacts.size(); i++){
            MegaContactDB megaContactDB = new MegaContactDB(String.valueOf(contacts.get(i).getHandle()), contacts.get(i).getEmail(), "", "");
            dbH.setContact(megaContactDB);
            megaApi.getUserAttribute(contacts.get(i), 1, listener);
            megaApi.getUserAttribute(contacts.get(i), 2, listener);
        }
        return null;
    }
}
