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
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaStringList;
import nz.mega.sdk.MegaStringMap;
import nz.mega.sdk.MegaStringTable;
import nz.mega.sdk.MegaUser;

public class MegaContactGetter implements MegaRequestListenerInterface {

    private MegaContactUpdater updater;

    private Context context;

    private List<MegaContact> megaContacts = new ArrayList<>();

    private int currentContactIndex;

    private DatabaseHandler dbH;

    private SharedPreferences preferences;

    private long lastSyncTimestamp;

    //For testing 1 min and 2 mins.
//    public static final int DAY = 1 * 60 * 1000;
//    public static final int WEEK = 2 * 60 * 1000;

    public static final int DAY = 24 * 60 * 60 * 1000;
    public static final int WEEK = 7 * 24 * 60 * 60 * 1000;

    public static final String LAST_SYNC_TIMESTAMP_FILE = "lastsynctimestamp";
    public static final String LAST_SYNC_TIMESTAMP_KEY = "lastsyncmegacontactstimestamp";

    public MegaContactGetter(Context context) {
        this.context = context;
        dbH = DatabaseHandler.getDbHandler(context);
        preferences = context.getSharedPreferences(LAST_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE);
        lastSyncTimestamp = preferences.getLong(LAST_SYNC_TIMESTAMP_KEY, 0);
    }

    private void updateLastSyncTimestamp() {
        preferences.edit().putLong(LAST_SYNC_TIMESTAMP_KEY, System.currentTimeMillis()).apply();
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
        log("start: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() == MegaRequest.TYPE_GET_REGISTERED_CONTACTS) {
            if (e.getErrorCode() == MegaError.API_OK) {
                megaContacts.clear();
                MegaStringMap map = request.getMegaStringMap();
                MegaStringTable table = request.getMegaStringTable();

                MegaContact contact;
                for (int i = 0; i < table.size(); i++) {
                    contact = new MegaContact();
                    MegaStringList list = table.get(i);
                    contact.id = list.get(1);
                    if(api.getMyUserHandle().equals(contact.id)) {
                        log("it's myself");
                        continue;
                    }
                    contact.normalizedPhoneNumber = list.get(0);
                    contact.handle = getUserHandler(list.get(1));
                    //the normalized phone number is the key
                    contact.localName = map.get(list.get(0));

                    log("contact: " + contact);
                    megaContacts.add(contact);
                }
                if (megaContacts.size() > 0) {
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
                updateLastSyncTimestamp();

                // filter out
                List<MegaContact> list = filterOut(api, megaContacts);

                if (updater != null) {
                    updater.onFinish(list);
                }
                currentContactIndex = 0;
            } else {
                MegaContact nextContact = getCurrentContactIndex();
                if (nextContact != null) {
                    api.getUserEmail(getUserHandler(nextContact.id), this);
                }
            }
        }
    }

    private List<MegaContact> filterOut(MegaApiJava api, List<MegaContact> list) {
        List<MegaContact> conatcs = filterOutContacts(api, list);
        List<MegaContact> retrunList = filterOutPendingContacts(api, conatcs);
        Collections.sort(retrunList, new Comparator<MegaContact>() {

            @Override
            public int compare(MegaContact o1, MegaContact o2) {
                return o1.localName.compareTo(o2.localName);
            }
        });
        return retrunList;
    }

    private List<MegaContact> filterOutContacts(MegaApiJava api, List<MegaContact> list) {
        log("filterOutContacts");
        for (MegaUser user : api.getContacts()) {
            log("contact visibility: " + user.getVisibility() + " -> " + user.getEmail());
            for (MegaContact contact : list) {
                boolean hasSameEamil = user.getEmail().equals(contact.getEmail());
                boolean isContact = user.getVisibility() == MegaUser.VISIBILITY_VISIBLE;
                boolean isBlocked = user.getVisibility() == MegaUser.VISIBILITY_BLOCKED;

                if (hasSameEamil && (isContact || isBlocked)) {
                    list.remove(contact);
                    break;
                }
            }
        }
        return list;
    }

    private List<MegaContact> filterOutPendingContacts(MegaApiJava api, List<MegaContact> list) {
        log("filterOutPendingContacts");
        for (MegaContactRequest request : api.getOutgoingContactRequests()) {
            log("contact request: " + request.getStatus() + " -> " + request.getTargetEmail());
            for (MegaContact contact : list) {
                boolean hasSameEamil = request.getTargetEmail().equals(contact.getEmail());
                boolean isAccepted = request.getStatus() == MegaContactRequest.STATUS_ACCEPTED;
                boolean isPending = request.getStatus() == MegaContactRequest.STATUS_UNRESOLVED;

                if (hasSameEamil && (isAccepted || isPending)) {
                    list.remove(contact);
                    break;
                }
            }
        }
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

    public void getMegaContacts(MegaApiAndroid api, List<ContactsUtil.LocalContact> localContacts, long period) {
        if (System.currentTimeMillis() - lastSyncTimestamp > period) {
            log("getMegaContacts request from server");
            api.getRegisteredContacts(getRequestParameter(localContacts), this);
        } else {
            log("getMegaContacts load from database");
            if (updater != null) {
                List<MegaContact> list = dbH.getMegaContacts();
                list = filterOut(api, list);
                updater.onFinish(list);
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
