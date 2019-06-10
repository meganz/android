package mega.privacy.android.app.utils.contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.utils.Util;

public class ContactsUtil {

    public static class LocalContact {

        private long id;

        private String name;

        private List<String> phoneNumberList = new ArrayList<>();

        private List<String> normalizedPhoneNumberList = new ArrayList<>();

        private List<String> emailList = new ArrayList<>();

        public LocalContact(long id, String name) {
            this.id = id;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public void addPhoneNumber(String phoneNumer) {
            phoneNumberList.add(phoneNumer);
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

        public String getSinglePrefferedContactDetail() {
            if (!emailList.isEmpty()) {
                return emailList.get(0);
            }
            if (!normalizedPhoneNumberList.isEmpty()) {
                return normalizedPhoneNumberList.get(0);
            }
            return null;
        }

        @Override
        public String toString() {
            return "LocalContact{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", phoneNumberList=" + phoneNumberList +
                    ", normalizedPhoneNumberList=" + normalizedPhoneNumberList +
                    ", emailList=" + emailList +
                    '}';
        }
    }

    public static List<LocalContact> getLocalContactList(Context context) {
        // use current default country code to normalize phonenumber.
        String countryCode = Util.getCountryCodeByNetwork(context);
        log("coutry code is: " + countryCode);

        List<LocalContact> localContacts = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();

        //contacts uri
        Uri contactsUri = ContactsContract.Contacts.CONTENT_URI;
        Cursor cursor = resolver.query(contactsUri, null, null, null, null);

        LocalContact contact;
        if (cursor != null) {
            log("has " + cursor.getCount() + " contacts");
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                contact = new LocalContact(id, name);

                // get phone numbers
                Uri phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                Cursor phones = resolver.query(phoneUri, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id,
                        null, null);
                if (phones != null) {
                    while (phones.moveToNext()) {
                        // try to get normalized phone number from system first.
                        String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        contact.addPhoneNumber(phoneNumber);
                        String normalizedPhoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));

                        if (normalizedPhoneNumber == null) {
                            // use current country code to normalize the phone number.
                            normalizedPhoneNumber = PhoneNumberUtils.formatNumberToE164(phoneNumber, countryCode);
                        }
                        if (normalizedPhoneNumber != null) {
                            contact.addNormalizedPhoneNumber(normalizedPhoneNumber);
                        }
                    }
                    phones.close();
                }

                // get emails
                Uri emailUri = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
                Cursor emails = resolver.query(emailUri, null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=" + id,
                        null, null);
                if (emails != null) {
                    while (emails.moveToNext()) {
                        String email = emails.getString(emails.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                        contact.addEmail(email);
                    }
                    emails.close();
                }
                localContacts.add(contact);
            }
            cursor.close();
        }
        return localContacts;
    }

    private static void log(String message) {
        Util.log("ContactsUtil", message);
    }
}
