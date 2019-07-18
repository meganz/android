package mega.privacy.android.app.utils.contacts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mega.privacy.android.app.lollipop.InvitationContactInfo;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaUser;

public class ContactsFilter {

    public static <T extends ContactWithEmail> ArrayList<T> filterOutContacts(MegaApiJava api, ArrayList<T> list) {
        log("filterOutContacts");
        for (MegaUser user : api.getContacts()) {
            log("contact visibility: " + user.getVisibility() + " -> " + user.getEmail());
            Iterator<T> iterator = list.iterator();
            while (iterator.hasNext()) {
                T t = iterator.next();
                boolean hasSameEamil = user.getEmail().equals(t.getEmail());
                boolean isContact = user.getVisibility() == MegaUser.VISIBILITY_VISIBLE;
                boolean isBlocked = user.getVisibility() == MegaUser.VISIBILITY_BLOCKED;

                if (hasSameEamil && (isContact || isBlocked)) {
                    log("filter out: " + t);
                    iterator.remove();
                }
            }
        }
        return list;
    }

    public static <T extends ContactWithEmail> ArrayList<T> filterOutPendingContacts(MegaApiJava api, ArrayList<T> list) {
        log("filterOutPendingContacts");
        for (MegaContactRequest request : api.getOutgoingContactRequests()) {
            log("contact request: " + request.getStatus() + " -> " + request.getTargetEmail());
            Iterator<T> iterator = list.iterator();
            while (iterator.hasNext()) {
                T t = iterator.next();
                boolean hasSameEmail = request.getTargetEmail().equals(t.getEmail());
                boolean isAccepted = request.getStatus() == MegaContactRequest.STATUS_ACCEPTED;
                boolean isPending = request.getStatus() == MegaContactRequest.STATUS_UNRESOLVED;

                if (hasSameEmail && (isAccepted || isPending)) {
                    log("filter out: " + t);
                    iterator.remove();
                }
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
                        log("filter out: " + phoneContact);
                        iterator.remove();
                    }
                }
            }
        }
    }

    public static ArrayList<String> getContact(MegaApiJava api, ArrayList<String> emails) {
        ArrayList<String> contacts = new ArrayList<>();
        for (MegaUser user : api.getContacts()) {
            log("contact visibility: " + user.getVisibility() + " -> " + user.getEmail());
            Iterator<String> iterator = emails.iterator();
            while (iterator.hasNext()) {
                String email = iterator.next();
                boolean hasSameEamil = user.getEmail().equals(email);
                boolean isContact = user.getVisibility() == MegaUser.VISIBILITY_VISIBLE;

                if (hasSameEamil && isContact) {
                    iterator.remove();
                    contacts.add(email);
                    break;
                }
            }
        }
        return contacts;
    }

    public static ArrayList<String> getPendingRequest(MegaApiJava api, ArrayList<String> emails) {
        ArrayList<String> pendings = new ArrayList<>();
        for (MegaContactRequest request : api.getOutgoingContactRequests()) {
            log("contact request: " + request.getStatus() + " -> " + request.getTargetEmail());
            Iterator<String> iterator = emails.iterator();
            while (iterator.hasNext()) {
                String email = iterator.next();
                boolean hasSameEmail = request.getTargetEmail().equals(email);
                boolean isAccepted = request.getStatus() == MegaContactRequest.STATUS_ACCEPTED;
                boolean isPending = request.getStatus() == MegaContactRequest.STATUS_UNRESOLVED;

                if (hasSameEmail && (isAccepted || isPending)) {
                    iterator.remove();
                    pendings.add(email);
                    break;
                }
            }
        }
        return pendings;
    }

    private static void log(String message) {
        Util.log("ContactsFilter", message);
    }
}
