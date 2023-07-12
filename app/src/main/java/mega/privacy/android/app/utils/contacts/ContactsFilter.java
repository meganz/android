package mega.privacy.android.app.utils.contacts;

import java.util.List;

import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaUser;

public class ContactsFilter {

    public static void filterOutContacts(MegaApiJava api, List<String> list) {
        for (MegaUser user : api.getContacts()) {
            list.removeIf(email -> isContact(user, email));
        }
    }

    public static void filterOutPendingContacts(MegaApiJava api, List<String> list) {
        for (MegaContactRequest request : api.getOutgoingContactRequests()) {
            list.removeIf(email -> isPending(request, email));
        }
    }

    private static boolean isContact(MegaUser user, String email) {
        boolean hasSameEmail = user.getEmail().equals(email);
        boolean isContact = user.getVisibility() == MegaUser.VISIBILITY_VISIBLE;
        return hasSameEmail && isContact;
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
        if (user == null) {
            return false;
        }
        return user.getEmail().equals(email);
    }
}
