package mega.privacy.android.app.utils.contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.utils.LogUtil.*;

public class ContactsUtil {

    public static class LocalContact {

        private long id;

        private String name;

        private List<String> phoneNumberList = new ArrayList<>();

        private List<String> normalizedPhoneNumberList = new ArrayList<>();

        private List<String> emailList = new ArrayList<>();

        private LocalContact(long id, String name) {
            this.id = id;
            //If contact name has double quotes replace by single quotes to avoid JSON parse error of the string
            this.name = name == null ? "" : name.replaceAll("\"","''");
        }

        public long getId() {
            return id;
        }

        public void addPhoneNumber(String phoneNumber) {
            phoneNumberList.add(phoneNumber);
        }

        public void addNormalizedPhoneNumber(String normalizedPhoneNumber) {
            normalizedPhoneNumberList.add(normalizedPhoneNumber);
        }

        public void addEmail(String email) {
            emailList.add(email);
        }

        public String getFirstLetter() {
            return String.valueOf(name.charAt(0)).toUpperCase();
        }

        public String getName() {
            return name;
        }

        public List<String> getPhoneNumberList() {
            return phoneNumberList;
        }

        public List<String> getNormalizedPhoneNumberList() {
            return normalizedPhoneNumberList;
        }

        public List<String> getEmailList() {
            return emailList;
        }

        @Override
        public String toString() {
            return "\nLocalContact{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", phoneNumberList=" + phoneNumberList +
                    ", normalizedPhoneNumberList=" + normalizedPhoneNumberList +
                    ", emailList=" + emailList +
                    '}';
        }
    }

    static List<LocalContact> getLocalContactList(Context context) {
        List<LocalContact> localContacts = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();

        //get all the contacts
        Uri contactsUri = ContactsContract.Contacts.CONTENT_URI;
        Cursor cursor = resolver.query(contactsUri, new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
        LocalContact contact;
        if (cursor != null) {
            logDebug("has " + cursor.getCount() + " contacts");
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String name = cursor.getString(1);
                contact = new LocalContact(id, name);
                localContacts.add(contact);
            }
            cursor.close();
        }

        // get phone numbers
        Uri phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        Cursor phones = resolver.query(phoneUri,
                new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER},
                null, null, null);
        if (phones != null) {
            while (phones.moveToNext()) {
                //notice the index order
                long id = phones.getLong(0);
                String phone = phones.getString(1);
                String normalizedPhone = phones.getString(2);

                for (LocalContact temp : localContacts) {
                    if (temp.getId() == id) {
                        temp.addPhoneNumber(phone);
                        if (normalizedPhone == null) {
                            //If roaming, don't normalize the phone number.
                            if (!Util.isRoaming(context)) {
                                // use current country code to normalize the phone number.
                                normalizedPhone = Util.normalizePhoneNumberByNetwork(context,phone);
                            }
                        }
                        if (normalizedPhone != null) {
                            temp.addNormalizedPhoneNumber(normalizedPhone);
                        }
                    }
                }
            }
            phones.close();
        }

        // get emails
        Uri emailUri = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        Cursor emails = resolver.query(emailUri,
                new String[]{ContactsContract.CommonDataKinds.Email.CONTACT_ID, ContactsContract.CommonDataKinds.Email.ADDRESS},
                null, null, null);
        if (emails != null) {
            while (emails.moveToNext()) {
                long id = emails.getLong(0);
                String email = emails.getString(1);

                for (LocalContact temp : localContacts) {
                    if (temp.getId() == id) {
                        temp.addEmail(email);
                    }
                }
            }
            emails.close();
        }
        Collections.sort(localContacts, new Comparator<LocalContact>() {

            @Override
            public int compare(LocalContact o1, LocalContact o2) {
                return o1.name.compareToIgnoreCase(o2.name);
            }
        });
        return localContacts;
    }
}
