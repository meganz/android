package mega.privacy.android.app.utils.contacts;

import android.content.Context;

import java.util.Iterator;
import java.util.List;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaUser;

public class ContactsFilter {

    public static void filterOutContacts(MegaApiJava api, List<String> list) {
        for (MegaUser user : api.getContacts()) {
            Iterator<String> iterator = list.iterator();
            while (iterator.hasNext()) {
                String email = iterator.next();
                if (isContact(user, email)) {
                    iterator.remove();
                }
            }
        }
    }

    public static void filterOutPendingContacts(MegaApiJava api, List<String> list) {
        for (MegaContactRequest request : api.getOutgoingContactRequests()) {
            Iterator<String> iterator = list.iterator();
            while (iterator.hasNext()) {
                String email = iterator.next();
                if (isPending(request, email)) {
                    iterator.remove();
                }
            }
        }
    }

    public static void filterOutMyself(MegaApiJava api, List<String> list) {
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String email = iterator.next();
            if (isMySelf(api, email)) {
                iterator.remove();
            }
        }
    }

    public static void filterOutMegaUsers(Context context , List<MegaContactGetter.MegaContact> megaContacts, List<String> contactInfos) {
        for (MegaContactGetter.MegaContact megaContact : megaContacts) {
            Iterator<String> iterator = contactInfos.iterator();
            while (iterator.hasNext()) {
                // phone number or email
                String contactInfo = iterator.next();
                String normalizedPhoneNumber = megaContact.getNormalizedPhoneNumber();
                if (megaContact.getEmail().equals(contactInfo) || (normalizedPhoneNumber != null && normalizedPhoneNumber.equals(Util.normalizePhoneNumberByNetwork(context, contactInfo)))) {
                    iterator.remove();
                }
            }
        }
    }

    private static boolean isContact(MegaUser user, String email) {
        boolean hasSameEamil = user.getEmail().equals(email);
        boolean isContact = user.getVisibility() == MegaUser.VISIBILITY_VISIBLE;
        return hasSameEamil && isContact;
    }

    private static boolean isPending(MegaContactRequest request, String email) {
        boolean hasSameEmail = request.getTargetEmail().equals(email);
        boolean isAccepted = request.getStatus() == MegaContactRequest.STATUS_ACCEPTED;
        boolean isPending = request.getStatus() == MegaContactRequest.STATUS_UNRESOLVED;
        return hasSameEmail && (isAccepted || isPending);
    }

    public static boolean isEmailInContacts(MegaApiJava api, String email) {
        for (MegaUser user : api.getContacts()) {
            if (isContact(user, email)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmailInPending(MegaApiJava api, String email) {
        for (MegaContactRequest request : api.getOutgoingContactRequests()) {
            if (isPending(request, email)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMySelf(MegaApiJava api, String email) {
        MegaUser user = api.getMyUser();
        if(user == null) {
            return false;
        }
        return user.getEmail().equals(email);
    }
}
