package mega.privacy.android.app.utils.contacts;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaStringList;
import nz.mega.sdk.MegaStringMap;
import nz.mega.sdk.MegaStringTable;

public class MegaContactGetter implements MegaRequestListenerInterface {

    private MegaContactUpdater updater;

    private Context context;

    private ArrayList<MegaContact> megaContacts = new ArrayList<>();

    private int currentContactIndex;

    private DatabaseHandler dbH;

    private SharedPreferences preferences;

    private static boolean requestInProgress;

    //different instance should share
    private static long lastSyncTimestamp;

    public static final int DAY = 24 * 60 * 60 * 1000;
    public static final int WEEK = 7 * 24 * 60 * 60 * 1000;

    public static final String LAST_SYNC_TIMESTAMP_FILE = "last_sync_timestamp";
    public static final String LAST_SYNC_TIMESTAMP_KEY = "last_sync_mega_contacts_timestamp";

    public MegaContactGetter(Context context) {
        this.context = context;
        dbH = DatabaseHandler.getDbHandler(context);
        preferences = context.getSharedPreferences(LAST_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE);
        getLastSyncTimeStamp();
    }

    private void getLastSyncTimeStamp() {
        lastSyncTimestamp = preferences.getLong(LAST_SYNC_TIMESTAMP_KEY, 0);
    }

    private void updateLastSyncTimestamp() {
        lastSyncTimestamp = System.currentTimeMillis();
        preferences.edit().putLong(LAST_SYNC_TIMESTAMP_KEY, lastSyncTimestamp).apply();
    }

    public void setMegaContactUpdater(MegaContactUpdater updater) {
        this.updater = updater;
    }

    public static class MegaContact implements ContactWithEmail {

        private String id;

        private long handle;

        private String localName;

        private String email;

        private String normalizedPhoneNumber;

        @Override
        public String toString() {
            return "\nMegaContact{" +
                    "id='" + id + '\'' +
                    ", handle=" + handle +
                    ", localName='" + localName + '\'' +
                    ", email='" + email + '\'' +
                    ", normalizedPhoneNumber='" + normalizedPhoneNumber + '\'' +
                    '}';
        }

        public String getId() {
            return id;
        }

        public long getHandle() {
            return handle;
        }

        public String getLocalName() {
            return localName;
        }

        public String getEmail() {
            return email;
        }

        public String getNormalizedPhoneNumber() {
            return normalizedPhoneNumber;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setHandle(long handle) {
            this.handle = handle;
        }

        public void setLocalName(String localName) {
            this.localName = localName;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setNormalizedPhoneNumber(String normalizedPhoneNumber) {
            this.normalizedPhoneNumber = normalizedPhoneNumber;
        }
    }

    public interface MegaContactUpdater {

        /**
         * Get registered contacts successfully.
         *
         * @param megaContacts Registerd mega contacts with all the info needed.
         */
        void onFinish(List<MegaContact> megaContacts);

        /**
         * When mega request failed.
         *
         * @param errorCode     Error code.
         * @param requestString What request.
         * @see MegaError
         * @see MegaRequest
         */
        void onException(int errorCode, String requestString);

        /**
         * When get no registered mega contacts.
         */
        void noContacts();
    }

    public List<ContactsUtil.LocalContact> getLocalContacts() {
        return ContactsUtil.getLocalContactList(context);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("start: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() == MegaRequest.TYPE_GET_REGISTERED_CONTACTS) {
            requestInProgress = false;
            if (e.getErrorCode() == MegaError.API_OK) {
                //when request is successful, update the timestamp.
                updateLastSyncTimestamp();
                megaContacts.clear();
                MegaStringMap map = request.getMegaStringMap();
                MegaStringTable table = request.getMegaStringTable();

                MegaContact contact;
                for (int i = 0; i < table.size(); i++) {
                    contact = new MegaContact();
                    MegaStringList list = table.get(i);
                    contact.id = list.get(1);
                    contact.normalizedPhoneNumber = list.get(0);
                    contact.handle = getUserHandler(list.get(1));
                    //the normalized phone number is the key
                    contact.localName = map.get(list.get(0));

                    log("contact: " + contact);
                    megaContacts.add(contact);
                }
                if (megaContacts.size() > 0) {
                    currentContactIndex = 0;
                    MegaContact firstContact = getCurrentContactIndex();
                    if (firstContact != null) {
                        api.getUserEmail(getUserHandler(firstContact.id), this);
                    }
                } else {
                    log("no mega contacts.");
                    if (updater != null) {
                        updater.noContacts();
                    }
                }
            } else {
                log("get registered contacts faild with error code: " + e.getErrorCode());
                if (updater != null) {
                    updater.onException(e.getErrorCode(), request.getRequestString());
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_GET_USER_EMAIL) {
            if (e.getErrorCode() == MegaError.API_OK) {
                String email = request.getEmail();
                if (!TextUtils.isEmpty(email)) {
                    MegaContact currentContact = getCurrentContactIndex();
                    if (currentContact != null) {
                        currentContact.email = email;
                        if (currentContact.localName == null) {
                            currentContact.localName = email;
                        }
                    }
                } else {
                    log("Contact's email is empty!");
                }
            } else {
                log("get contact's email faild with error code: " + e.getErrorCode());
                if (updater != null) {
                    updater.onException(e.getErrorCode(), request.getRequestString());
                }
            }
            //get next contact's email.
            currentContactIndex++;
            //all the emails have been gotten.
            if (currentContactIndex >= megaContacts.size()) {
                // save to db
                dbH.clearMegaContacts();
                dbH.batchInsertMegaContacts(megaContacts);

                // filter out
                List<MegaContact> list = filterOut(api, megaContacts);

                currentContactIndex = 0;
                if (updater != null) {
                    updater.onFinish(list);
                }
            } else {
                MegaContact nextContact = getCurrentContactIndex();
                if (nextContact != null) {
                    api.getUserEmail(getUserHandler(nextContact.id), this);
                }
            }
        }
    }

    private ArrayList<MegaContact> filterOut(MegaApiJava api, ArrayList<MegaContact> list) {
        ContactsFilter.filterOutContacts(api, list);
        ContactsFilter.filterOutPendingContacts(api, list);
        ContactsFilter.filterOutMyself(api, list);
        Collections.sort(list, new Comparator<MegaContact>() {

            @Override
            public int compare(MegaContact o1, MegaContact o2) {
                return o1.localName.compareTo(o2.localName);
            }
        });
        return list;
    }

    private MegaContact getCurrentContactIndex() {
        if (megaContacts.size() == 0) {
            return null;
        }
        if (currentContactIndex >= megaContacts.size()) {
            //get last contact.
            currentContactIndex = megaContacts.size() - 1;
        }
        return megaContacts.get(currentContactIndex);
    }

    public void getMegaContacts(MegaApiAndroid api, long period) {
        if(api.getRootNode() == null) {
            log("haven't logged in, return");
            return;
        }
        if (System.currentTimeMillis() - lastSyncTimestamp > period && !requestInProgress) {
            requestInProgress = true;
            log("getMegaContacts request from server");
            api.getRegisteredContacts(getRequestParameter(getLocalContacts()), this);
        } else {
            if(!requestInProgress) {
                log("getMegaContacts load from database");
                if (updater != null) {
                    ArrayList<MegaContact> list = dbH.getMegaContacts();
                    list = filterOut(api, list);
                    updater.onFinish(list);
                }
            }
        }
    }

    private long getUserHandler(String id) {
        return MegaApiAndroid.base64ToUserHandle(id);
    }

    private static MegaStringMap getRequestParameter(List<ContactsUtil.LocalContact> localContacts) {
        MegaStringMap stringMap = MegaStringMap.createInstance();
        if (stringMap == null) {
            return null;
        }

        for (ContactsUtil.LocalContact contact : localContacts) {
            String name = contact.getName();
            List<String> normalizedPhoneNumberSet = contact.getNormalizedPhoneNumberList();
            if (!normalizedPhoneNumberSet.isEmpty()) {
                for (String phoneNumber : normalizedPhoneNumberSet) {
                    stringMap.set(phoneNumber, name);
                }
            }
        }
        log("local contacts size is: " + stringMap.size());
        return stringMap;
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    private static void log(String message) {
        Util.log("MegaContactGetter", message);
    }
}
