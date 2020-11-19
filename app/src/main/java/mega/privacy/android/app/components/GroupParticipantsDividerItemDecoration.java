package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import static mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaParticipantsChatLollipopAdapter.ITEM_VIEW_TYPE_HEADER;

public class GroupParticipantsDividerItemDecoration extends HeaderItemDecoration {

    public GroupParticipantsDividerItemDecoration(Context context) {
        super(context);
    }

    @Override
    protected void drawDivider(Canvas c, RecyclerView parent, View child, int viewType) {
        if (viewType != ITEM_VIEW_TYPE_HEADER) {
            drawDivider(c, parent, child);
        }
    }
}
