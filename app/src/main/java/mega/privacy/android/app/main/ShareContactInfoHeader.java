package mega.privacy.android.app.main;

import com.brandongogetap.stickyheaders.exposed.StickyHeader;

/**
 * Created by mega on 17/07/18.
 */

public class ShareContactInfoHeader extends ShareContactInfo implements StickyHeader{

    public ShareContactInfoHeader(boolean isHeader, boolean isMegaContact, boolean isPhoneContact) {
        super(isHeader, isMegaContact, isPhoneContact);
    }
}
