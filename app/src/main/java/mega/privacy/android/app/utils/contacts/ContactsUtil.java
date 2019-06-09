package mega.privacy.android.app.utils.contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mega.privacy.android.app.utils.Util;

public class ContactsUtil {

    public static class LocalContact {

        private int id;

        private String name;

        private Set<String> phoneNumberSet = new HashSet<>();

        private Set<String> normalizedPhoneNumberSet = new HashSet<>();

        private Set<String> emailSet = new HashSet<>();

        public LocalContact(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void addPhoneNumber(String phoneNumer) {
            phoneNumberSet.add(phoneNumer);
        }

        public void addNormalizedPhoneNumber(String normalizedPhoneNumber) {
            normalizedPhoneNumberSet.add(normalizedPhoneNumber);
        }

        public void addEmail(String email) {
            emailSet.add(email);
        }

        public String getFirstLetter() {
            return String.valueOf(name.charAt(0)).toUpperCase();
        }

        public String getName() {
            return name;
        }

        public Set<String> getPhoneNumberSet() {
            return phoneNumberSet;
        }

        public Set<String> getNormalizedPhoneNumberSet() {
            return normalizedPhoneNumberSet;
        }

        public Set<String> getEmailSet() {
            return emailSet;
        }

        public String getSinglePrefferedContactDetail() {
            if (!emailSet.isEmpty()) {
                return emailSet.toArray(new String[]{})[0];
            }
            if (!normalizedPhoneNumberSet.isEmpty()) {
                return normalizedPhoneNumberSet.toArray(new String[]{})[0];
            }
            return null;
        }

        @Override
        public String toString() {
            return "\nLocalContact{" +
                    "id='" + id + '\'' +
                    "name='" + name + '\'' +
                    ", phoneNumberSet=" + phoneNumberSet +
                    ", emailSet=" + emailSet +
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
                int id = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID));
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
