package mega.privacy.android.app.utils.contacts;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    private List<MegaContact> megaContacts = new ArrayList<>();

    private int currentContactIndex;

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

    public List<ContactsUtil.LocalContact> getLocalContacts(Context context) {
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
                MegaStringMap map = request.getMegaStringMap();
                MegaStringTable table = request.getMegaStringTable();

                MegaContact contact;
                for (int i = 0; i < table.size(); i++) {
                    contact = new MegaContact();
                    MegaStringList list = table.get(i);
                    contact.normalizedPhoneNumber = list.get(0);
                    contact.id = list.get(1);
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
                // filter out
                filterOutContacts(api);
                filterOutPendingContacts(api);
                Collections.sort(megaContacts, new Comparator<MegaContact>() {

                    @Override
                    public int compare(MegaContact o1, MegaContact o2) {
                        return o1.localName.compareTo(o2.localName);
                    }
                });

                if (updater != null) {
                    updater.onFinish(megaContacts);
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

    private void filterOutContacts(MegaApiJava api) {
        for(MegaUser user : api.getContacts()) {
            log(user.getVisibility() + " -> " + user.getEmail());
            for(MegaContact contact : megaContacts) {
                boolean hasSameEamil = user.getEmail().equals(contact.getEmail());
                boolean isContact = user.getVisibility() == MegaUser.VISIBILITY_VISIBLE;
                boolean isBlocked = user.getVisibility() == MegaUser.VISIBILITY_BLOCKED;

                if (hasSameEamil && (isContact || isBlocked)) {
                    megaContacts.remove(contact);
                    break;
                }
            }
        }
    }

    private void filterOutPendingContacts(MegaApiJava api) {
        for(MegaContactRequest request : api.getOutgoingContactRequests()) {
            log(request.getStatus() + " -> " + request.getTargetEmail());
            for(MegaContact contact : megaContacts) {
                boolean hasSameEamil = request.getTargetEmail().equals(contact.getEmail());
                boolean isAccepted = request.getStatus() == MegaContactRequest.STATUS_ACCEPTED;
                boolean isPending = request.getStatus() == MegaContactRequest.STATUS_UNRESOLVED;

                if(hasSameEamil && (isAccepted || isPending)) {
                    megaContacts.remove(contact);
                    break;
                }
            }
        }
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

    public void getMegaContacts(MegaApiAndroid api,List<ContactsUtil.LocalContact> localContacts) {
        api.getRegisteredContacts(getRequestParameter(localContacts), this);
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
        log("start: " + stringMap.size());
        return stringMap;
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    private static void log(String message) {
        Util.log("MegaContactGetter", message);
    }
}
