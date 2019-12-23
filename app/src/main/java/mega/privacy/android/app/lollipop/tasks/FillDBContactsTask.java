package mega.privacy.android.app.lollipop.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.lollipop.listeners.ContactNameListener;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.LogUtil.*;

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
        ArrayList<MegaUser> contacts = megaApi.getContacts();

        for(int i=0; i<contacts.size(); i++){
            MegaContactDB megaContactDB = new MegaContactDB(String.valueOf(contacts.get(i).getHandle()), contacts.get(i).getEmail(), "", "", null);
            dbH.setContact(megaContactDB);
            logDebug("********FillDBContactsTask:: update the DB ");
            megaApi.getUserAttribute(contacts.get(i), MegaApiJava.USER_ATTR_FIRSTNAME, new ContactNameListener(context));
            megaApi.getUserAttribute(contacts.get(i), MegaApiJava.USER_ATTR_LASTNAME, new ContactNameListener(context));
            megaApi.getUserAlias(contacts.get(i).getHandle(), new ContactNameListener(context));

        }
        return null;
    }
}
