package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import mega.privacy.android.app.R;

import static mega.privacy.android.app.main.megachat.chatAdapters.MegaListChatAdapter.*;

public class ChatDividerItemDecoration extends RecyclerView.ItemDecoration {
    Context context;
    private Drawable mDivider;

    public static final int ITEM_VIEW_TYPE_NODE= 0;
    public static final int ITEM_VIEW_TYPE_MONTH = 1;

    public ChatDividerItemDecoration(Context context) {
        mDivider = context.getResources().getDrawable(R.drawable.line_divider);
        this.context = context;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {

        int left = (int) context.getResources().getDimension(R.dimen.bottom_sheet_item_divider_margin_start);
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            int position = parent.getChildAdapterPosition(child);
            int viewType = parent.getAdapter().getItemViewType(position);

            if (viewType == ITEM_VIEW_TYPE_NORMAL_CHATS) {
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }
}
