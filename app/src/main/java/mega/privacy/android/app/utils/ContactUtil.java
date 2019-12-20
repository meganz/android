package mega.privacy.android.app.utils;

import mega.privacy.android.app.MegaContactDB;
import nz.mega.sdk.MegaUser;
import static mega.privacy.android.app.utils.Util.*;

public class ContactUtil {

    public static MegaContactDB getContactDB(long contactHandle) {
        return dbH.findContactByHandle(String.valueOf(contactHandle));
    }

    public static String getMegaUserNameDB(MegaUser user) {
        if (user == null) return null;

        String nameContact = getContactNameDB(user.getHandle());
        if (nameContact != null) return nameContact;

        //It isn't in the DB, ask for it to megaApi and listen for it in a broadcast. For the moment, email it will be the name.
//        ContactNameListener listener = new ContactNameListener(context);
//        megaApi.getUserAttribute(user, USER_ATTR_FIRSTNAME, listener);
//        megaApi.getUserAttribute(user, USER_ATTR_LASTNAME, listener);
//        megaApi.getUserAlias(user, listener);
        return user.getEmail();
    }

    public static String getContactNameDB(long contactHandle) {
        MegaContactDB contactDB = getContactDB(contactHandle);
        if (contactDB != null) {
            String nicknameText = contactDB.getNickname();
            if(nicknameText != null) return nicknameText;

            String firstNameText = contactDB.getName();
            String lastNameText = contactDB.getLastName();
            String emailText = contactDB.getMail();
            return buildFullName(firstNameText, lastNameText, emailText);
        }
        return null;
    }

    public static String getNicknameContact(long contactHandle){
        MegaContactDB contactDB = getContactDB(contactHandle);
        if(contactDB == null) return null;
        String nicknameText = contactDB.getNickname();
        return nicknameText;
    }

    public static String buildFullName(String name, String lastName, String mail) {

        if (name == null) {
            name = "";
        }
        if (lastName == null) {
            lastName = "";
        }
        String fullName = "";

        if (name.trim().length() <= 0) {
            fullName = lastName;
        } else {
            fullName = name + " " + lastName;
        }

        if (fullName.trim().length() <= 0) {
            if (mail == null) {
                mail = "";
            }
            if (mail.trim().length() <= 0) {
                return "";
            } else {
                return mail;
            }
        }
        return fullName;
    }

    public static String getFirstNameDB(long contactHandle) {
        MegaContactDB contactDB = getContactDB(contactHandle);
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
