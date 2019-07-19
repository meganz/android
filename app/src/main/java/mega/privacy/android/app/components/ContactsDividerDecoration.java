package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;

import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_MEGA_CONTACT_HEADER;
import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_PHONE_CONTACT_HEADER;

public class ContactsDividerDecoration extends SimpleDividerItemDecoration {

    public ContactsDividerDecoration(Context context, DisplayMetrics outMetrics) {
        super(context, outMetrics);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof RecyclerView.ViewHolder) {
                int position = ((RecyclerView.ViewHolder) tag).getAdapterPosition();
                RecyclerView.Adapter adapter = parent.getAdapter();
                if(adapter != null) {
                    int type = adapter.getItemViewType(position);
                    if (type != TYPE_MEGA_CONTACT_HEADER && type != TYPE_PHONE_CONTACT_HEADER) {
                        drawDivider(c, parent, parent.getChildAt(i));
                    }
                }
            }
        }
    }
}
