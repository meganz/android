package mega.privacy.android.app.utils;

import android.content.Context;

import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.lollipop.listeners.ContactNameListener;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaUser;
import static mega.privacy.android.app.utils.Util.*;

public class ContactUtil {

    public static MegaContactDB getContactDB(long contactHandle) {
        return dbH.findContactByHandle(String.valueOf(contactHandle));
    }

    public static String getMegaUserNameDB(MegaApiAndroid megaApi, Context context, MegaUser user) {
        if (user == null) return null;

        String nameContact = getContactNameDB(megaApi, context, user.getHandle());
        if (nameContact != null) return nameContact;
        return user.getEmail();
    }

    public static String getContactNameDB(MegaApiAndroid megaApi, Context context, long contactHandle) {
        MegaContactDB contactDB = getContactDB(contactHandle);
        if (contactDB != null) {
            String nicknameText = contactDB.getNickname();
            if(nicknameText != null){
                return nicknameText;
            }

            String firstNameText = contactDB.getName();
            if (firstNameText == null || firstNameText.trim().length()<=0) {
                MegaUser user = megaApi.getContact(contactDB.getMail());
                megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_FIRSTNAME, new ContactNameListener(context));
            }

            String lastNameText = contactDB.getLastName();
            if (lastNameText == null || lastNameText.trim().length()<=0) {
                MegaUser user = megaApi.getContact(contactDB.getMail());
                megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_LASTNAME, new ContactNameListener(context));
            }

            String emailText = contactDB.getMail();
            String nameResult = buildFullName(firstNameText, lastNameText, emailText);
            return nameResult;
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
        if (name == null || name.trim().length()<=0) {
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
