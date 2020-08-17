package mega.privacy.android.app.utils.contacts;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.utils.TextUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaStringList;
import nz.mega.sdk.MegaStringMap;
import nz.mega.sdk.MegaStringTable;

import static mega.privacy.android.app.utils.LogUtil.*;

public class MegaContactGetter implements MegaRequestListenerInterface {

    /**
     * @see MegaApiJava#getRegisteredContacts
     */
    private static final int INDEX_ID = 1, INDEX_DATA = 0;

    private MegaContactUpdater updater;

    private Context context;

    private ArrayList<MegaContact> megaContacts = new ArrayList<>();
    private ArrayList<MegaContact> megaContactsWithEmail = new ArrayList<>();

    private int currentContactIndex;

    private DatabaseHandler dbH;

    private SharedPreferences preferences;

    private boolean requestInProgress;

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

    public void clearLastSyncTimeStamp() {
        preferences.edit().clear().apply();
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

    public static class MegaContact {

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
        logDebug("start: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() == MegaRequest.TYPE_GET_REGISTERED_CONTACTS) {
            requestInProgress = false;
            resetAfterRequest();
            if (e.getErrorCode() == MegaError.API_OK) {
                // The table contains all the mathced users.
                MegaStringTable table = request.getMegaStringTable();

                // When there's no matched user, should be considered as successful
                if (table.size() == 0) {
                    updateLastSyncTimestamp();
                } else {
                    MegaStringMap map = request.getMegaStringMap();
                    Map<String, MegaContact> contacts = parseMatchedContacts(map, table);
                    fillContactsList(contacts);
                    // Need to ask for email from server.
                    if (megaContacts.size() > 0) {
                        MegaContact firstContact = getCurrentContactIndex();
                        if (firstContact != null) {
                            api.getUserEmail(firstContact.handle, this);
                        }
                    } else {
                        // All the contacts are matched by email.
                        if(megaContactsWithEmail.size() > 0) {
                            processFinished(api, megaContactsWithEmail);
                        } else {
                            logWarning("No mega contacts.");
                            if (updater != null) {
                                updater.noContacts();
                            }
                        }
                    }
                }
            } else {
                logWarning("Get registered contacts faild with error code: " + e.getErrorCode());
                //current account has requested mega contacts too many times and reached the limitation, no need to re-try.
                if (e.getErrorCode() == MegaError.API_ETOOMANY) {
                    updateLastSyncTimestamp();
                }
                if (updater != null) {
                    updater.onException(e.getErrorCode(), request.getRequestString());
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_GET_USER_EMAIL) {
            if (e.getErrorCode() == MegaError.API_OK) {
                String email = request.getEmail();
                long handle = request.getNodeHandle();

                if (!TextUtils.isEmpty(email)) {
                    MegaContact currentContact = getCurrentContactIndex();
                    // Avoid to set email to wrong user.
                    if (currentContact != null && handle == currentContact.handle) {
                        currentContact.email = email;
                        if (currentContact.localName == null) {
                            currentContact.localName = email;
                        }
                    }
                } else {
                    logWarning("Contact's email is empty!");
                }
            } else {
                logWarning("Get contact's email faild with error code: " + e.getErrorCode());
            }
            // Get next contact's email.
            currentContactIndex++;
            // All the emails have been gotten.
            if (currentContactIndex >= megaContacts.size()) {
                megaContacts.addAll(megaContactsWithEmail);
                // Remove the contact who doesn't get email successfully.
                Iterator<MegaContact> iterator = megaContacts.iterator();
                while(iterator.hasNext()) {
                    MegaContact contact = iterator.next();
                    if(contact.getEmail() == null) {
                        iterator.remove();
                    }
                }
                processFinished(api, megaContacts);
            } else {
                MegaContact nextContact = getCurrentContactIndex();
                if (nextContact != null) {
                    api.getUserEmail(getUserHandler(nextContact.id), this);
                }
            }
        }
    }

    /**
     * Fill the matched MEGA contacts into two lists:
     * megaContactsWithEmail contains the contacts with email.
     * megaContacts contains the contacts without email, need to ask for email.
     *
     * @param contacts All the unique MEGA contacts, may be empty.
     */
    private void fillContactsList(Map<String, MegaContact> contacts) {
        for (MegaContact megaContact : contacts.values()) {
            if (megaContact.email != null) {
                megaContactsWithEmail.add(megaContact);
            } else {
                megaContacts.add(megaContact);
            }
        }
    }

    private void resetAfterRequest() {
        megaContacts.clear();
        megaContactsWithEmail.clear();
        currentContactIndex = 0;
    }

    /**
     * Parse server reponse to get matched and unique MEGA users.
     *
     * @see this#getRequestParameter
     * @see MegaApiJava#getRegisteredContacts
     *
     * @param map The submited data, Key is phone number or email of the local contact, Value is the local name.
     *            Here is used to get local name.
     * @param table Server returned infomation, contains matched MEGA users.
     * @return MEGA users. Key is user handle in Base64, value is corresponding MegaContact object.
     */
    private Map<String, MegaContact> parseMatchedContacts(MegaStringMap map,MegaStringTable table) {
        Map<String, MegaContact> temp = new HashMap<>();
        for (int i = 0; i < table.size(); i++) {
            // Each MegaStringList is a matched MEGA user.
            MegaStringList list = table.get(i);

            // User handle in Base64.
            String id = list.get(INDEX_ID);
            // Phone number or email.
            String data = list.get(INDEX_DATA);

            // Check if there's already a MegaContact with same id.
            MegaContact ex = temp.get(id);
            if (ex != null) {
                if(ex.email == null) {
                    ex.email = data;
                }
                if(ex.normalizedPhoneNumber == null) {
                    ex.normalizedPhoneNumber = data;
                }
            } else {
                MegaContact contact = new MegaContact();
                contact.id = id;
                if (TextUtil.isEmail(data)) {
                    contact.email = data;
                } else {
                    contact.normalizedPhoneNumber = data;
                }
                contact.handle = getUserHandler(contact.id);
                // The data(phone number or email) is the key.
                contact.localName = map.get(data);
                temp.put(id, contact);
            }
        }
        return temp;
    }

    private void processFinished(MegaApiJava api, ArrayList<MegaContact> list) {
        // save to db
        dbH.clearMegaContacts();
        dbH.batchInsertMegaContacts(list);

        // filter out
        list = filterOut(api, list);
        //when request is successful, update the timestamp.
        updateLastSyncTimestamp();
        if (updater != null) {
            updater.onFinish(list);
        }
    }

    private ArrayList<MegaContact> filterOut(MegaApiJava api, ArrayList<MegaContact> list) {
        List<String> emails = new ArrayList<>();
        for (MegaContact megaContact : list) {
            emails.add(megaContact.getEmail());
        }

        ContactsFilter.filterOutContacts(api, emails);
        ContactsFilter.filterOutPendingContacts(api, emails);
        ContactsFilter.filterOutMyself(api, emails);
        Iterator<MegaContact> iterator = list.iterator();
        while (iterator.hasNext()) {
            if (!emails.contains(iterator.next().email)) {
                iterator.remove();
            }
        }

        Collections.sort(list, (o1, o2) -> o1.localName.compareTo(o2.localName));
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
        if (api.getRootNode() == null) {
            logDebug("haven't logged in, return");
            return;
        }
        if (System.currentTimeMillis() - lastSyncTimestamp > period && !requestInProgress) {
            requestInProgress = true;
            logDebug("getMegaContacts request from server");
            api.getRegisteredContacts(getRequestParameter(getLocalContacts()), this);
        } else {
            if (!requestInProgress) {
                logDebug("getMegaContacts load from database");
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
            List<String> emailSet = contact.getEmailList();
            for (String phoneNumber : normalizedPhoneNumberSet) {
                stringMap.set(phoneNumber, name);
            }

            for (String email : emailSet) {
                stringMap.set(email, name);
            }
        }
        logDebug("local contacts size is: " + stringMap.size());
        return stringMap;
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
