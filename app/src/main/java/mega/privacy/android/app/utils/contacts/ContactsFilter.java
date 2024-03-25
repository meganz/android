package mega.privacy.android.app.utils.contacts;

import java.util.List;

import mega.privacy.android.app.main.InvitationContactInfo;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaUser;

public class ContactsFilter {

    /**
     * This function filters out the local contacts if the emails exist in the MEGA contacts.
     *
     * @param api  [MegaApiJava]
     * @param list List of local contacts' emails
     * @deprecated <p> Use {@link mega.privacy.android.domain.usecase.contact.FilterLocalContactsByEmailUseCase} instead.
     */
    @Deprecated
    public static void filterOutContacts(MegaApiJava api, List<String> list) {
        for (MegaUser user : api.getContacts()) {
            list.removeIf(email -> isContact(user, email));
        }
    }

    /**
     * This function filters out the local contacts if the email is in a pending state.
     *
     * @param api  [MegaApiJava]
     * @param list List of local contacts' emails
     * @deprecated <p> Use {@link mega.privacy.android.domain.usecase.contact.FilterPendingOrAcceptedLocalContactsByEmailUseCase} instead.
     */
    @Deprecated
    public static void filterOutPendingContacts(MegaApiJava api, List<String> list) {
        for (MegaContactRequest request : api.getOutgoingContactRequests()) {
            list.removeIf(email -> isPending(request, email));
        }
    }

    /**
     * This function determines whether the contact already exists in the MEGA contacts based on the email.
     *
     * @param user  [MegaUser]
     * @param email Email
     * @return True the email exists in the MEGA contacts, false otherwise
     * @deprecated <p> Use {@link mega.privacy.android.domain.usecase.contact.IsAMegaContactByEmailUseCase} instead.
     */
    @Deprecated
    private static boolean isContact(MegaUser user, String email) {
        boolean hasSameEmail = user.getEmail().equals(email);
        boolean isContact = user.getVisibility() == MegaUser.VISIBILITY_VISIBLE;
        return hasSameEmail && isContact;
    }

    /**
     * This function validates if the requested contact by email is in a pending state.
     *
     * @param request [MegaContactRequest]
     * @param email   Email
     * @return True if the contact's request is in a pending state, false otherwise
     * @deprecated <p> Use {@link mega.privacy.android.domain.usecase.contact.IsContactRequestByEmailInPendingOrAcceptedStateUseCase} instead.
     */
    @Deprecated
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

    public static boolean isTheSameContact(InvitationContactInfo first, InvitationContactInfo second) {
        return first.getId() == second.getId() && first.getDisplayInfo().equalsIgnoreCase(second.getDisplayInfo());
    }
}
