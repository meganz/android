package mega.privacy.android.app.utils;

import android.content.Context;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaContactDB;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaUser;

public class ContactUtil {

    public static MegaContactDB getContactDB(Context context, long contactHandle) {
        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        return dbH.findContactByHandle(String.valueOf(contactHandle));
    }

    public static String getMegaUserNameDB(MegaApiAndroid megaApi, Context context, MegaUser user) {
        if (user == null) return null;
        String nameContact = getContactNameDB(megaApi, context, user.getHandle());
        if (nameContact != null) return nameContact;
        return user.getEmail();
    }

    public static String getContactNameDB(MegaApiAndroid megaApi, Context context, MegaContactDB contactDB){
        String nicknameText = contactDB.getNickname();
        if (nicknameText != null) {
            return nicknameText;
        }

        String firstNameText = contactDB.getName();
        String lastNameText = contactDB.getLastName();

        String emailText = contactDB.getMail();
        String nameResult = buildFullName(firstNameText, lastNameText, emailText);
        return nameResult;
    }

    public static String getContactNameDB(MegaApiAndroid megaApi, Context context, long contactHandle) {
        MegaContactDB contactDB = getContactDB(context, contactHandle);
        if (contactDB != null) return getContactNameDB(megaApi, context, contactDB);
        return null;
    }

    public static String getNicknameContact(Context context, long contactHandle) {
        MegaContactDB contactDB = getContactDB(context, contactHandle);
        if (contactDB == null) return null;
        String nicknameText = contactDB.getNickname();
        return nicknameText;
    }

    public static String buildFullName(String name, String lastName, String mail) {
        if (name == null) name = "";

        if (lastName == null) lastName = "";

        String fullName;

        if (name.trim().length() <= 0) {
            fullName = lastName;
        } else {
            fullName = name + " " + lastName;
        }

        if (fullName.trim().length() <= 0) {
            if (mail == null || mail.trim().length() <= 0) mail = "";
            return mail;
        }
        return fullName;
    }

    public static String getFirstNameDB(Context context, long contactHandle) {
        MegaContactDB contactDB = getContactDB(context, contactHandle);
        if (contactDB != null) {
            String nicknameText = contactDB.getNickname();
            if (nicknameText != null) return nicknameText;

            String firstNameText = contactDB.getName();
            if (firstNameText == null) {
                firstNameText = "";
            }
            if (firstNameText.trim().length() > 0) return firstNameText;

            String lastNameText = contactDB.getLastName();
            if (lastNameText == null) {
                lastNameText = "";
            }
            if (lastNameText.trim().length() > 0) return lastNameText;

            String emailText = contactDB.getMail();
            if (emailText == null) {
                emailText = "";
            }
            if (emailText.trim().length() > 0) return emailText;
        }
        return "";
    }
}
