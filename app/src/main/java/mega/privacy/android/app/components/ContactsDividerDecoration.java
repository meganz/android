package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_MEGA_CONTACT_HEADER;
import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_PHONE_CONTACT_HEADER;

public class ContactsDividerDecoration extends HeaderItemDecoration {

    public ContactsDividerDecoration(Context context) {
        super(context);
    }

    @Override
    protected void drawDivider(Canvas c, RecyclerView parent, View child, int viewType) {
        if (viewType != TYPE_MEGA_CONTACT_HEADER && viewType != TYPE_PHONE_CONTACT_HEADER) {
            drawDivider(c, parent, child);
        }
    }
}
