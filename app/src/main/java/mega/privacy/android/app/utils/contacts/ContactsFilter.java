package mega.privacy.android.app.utils.contacts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mega.privacy.android.app.lollipop.InvitationContactInfo;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaUser;

public class ContactsFilter {

    public static <T extends ContactWithEmail> ArrayList<T> filterOutContacts(MegaApiJava api, ArrayList<T> list) {
        for (MegaUser user : api.getContacts()) {
            Iterator<T> iterator = list.iterator();
            while (iterator.hasNext()) {
                T t = iterator.next();
                if (isContact(user, t.getEmail())) {
                    iterator.remove();
                }
            }
        }
        return list;
    }

    public static <T extends ContactWithEmail> ArrayList<T> filterOutPendingContacts(MegaApiJava api, ArrayList<T> list) {
        for (MegaContactRequest request : api.getOutgoingContactRequests()) {
            Iterator<T> iterator = list.iterator();
            while (iterator.hasNext()) {
                T t = iterator.next();
                if (isPending(request,t.getEmail())) {
                    iterator.remove();
                }
            }
        }
        return list;
    }

    public static <T extends ContactWithEmail> ArrayList<T> filterOutMyself(MegaApiJava api, ArrayList<T> list) {
        Iterator<T> iterator = list.iterator();
        while (iterator.hasNext()) {
            T t = iterator.next();
            if (isMySelf(api, t.getEmail())) {
                iterator.remove();
            }
        }
        return list;
    }

    public static void filterOutMegaUsers(List<InvitationContactInfo> megaContacts, List<InvitationContactInfo> phoneContacts) {
        for (InvitationContactInfo megaContact : megaContacts) {
            Iterator<InvitationContactInfo> iterator = phoneContacts.iterator();
            while (iterator.hasNext()) {
                InvitationContactInfo phoneContact = iterator.next();
                boolean isNotHeader =
                        megaContact.getType() != InvitationContactInfo.TYPE_MEGA_CONTACT_HEADER &&
                                megaContact.getType() != InvitationContactInfo.TYPE_PHONE_CONTACT_HEADER &&
                                phoneContact.getType() != InvitationContactInfo.TYPE_MEGA_CONTACT_HEADER &&
                                phoneContact.getType() != InvitationContactInfo.TYPE_PHONE_CONTACT_HEADER;
                if (isNotHeader) {
                    if (megaContact.getEmail().equals(phoneContact.getEmail()) ||
                            megaContact.getNormalizedNumber().equals(phoneContact.getNormalizedNumber())) {
                        iterator.remove();
                    }
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
        return api.getMyUser().getEmail().equals(email);
    }
}
