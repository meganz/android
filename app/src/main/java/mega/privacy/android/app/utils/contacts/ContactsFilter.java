package mega.privacy.android.app.utils.contacts;

import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaUser;

public class ContactsFilter {

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

    /**
     * This function determines whether the given email exists in the visible contacts..
     *
     * @param api   [MegaApiJava].
     * @param email The email that needs to be checked.
     * @return Boolean. Whether the email exists.
     * @deprecated <p> Use {@link mega.privacy.android.domain.usecase.contact.IsEmailInContactsUseCase} instead.
     */
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
