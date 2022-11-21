package mega.privacy.android.app.components

import android.content.Context
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.list.adapter.MeetingsAdapter

class ChatDividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val divider = ContextCompat.getDrawable(context, R.drawable.line_divider)
        ?: error("Divider doesn't exist")

    private val fullMarginLeft =
        context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_item_divider_margin_start)

    private val shortMarginLeft =
        context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_item_divider_short_margin_start)

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            when (parent.adapter?.getItemViewType(position)) {
                MeetingsAdapter.TYPE_DATA -> {
                    if (position + 1 < (parent.adapter?.itemCount ?: 0)
                        && parent.adapter?.getItemViewType(position + 1) == MeetingsAdapter.TYPE_HEADER
                    ) {
                        val params = child.layoutParams as RecyclerView.LayoutParams
                        val top = child.bottom + params.bottomMargin
                        val bottom = top + divider.intrinsicHeight
                        divider.setBounds(shortMarginLeft, top, right, bottom)
                        divider.draw(canvas)
                    } else {
                        val params = child.layoutParams as RecyclerView.LayoutParams
                        val top = child.bottom + params.bottomMargin
                        val bottom = top + divider.intrinsicHeight
                        divider.setBounds(fullMarginLeft, top, right, bottom)
                        divider.draw(canvas)
                    }
                }
                MeetingsAdapter.TYPE_HEADER -> {
                    // do nothing
                }
                else -> {
                    val params = child.layoutParams as RecyclerView.LayoutParams
                    val top = child.bottom + params.bottomMargin
                    val bottom = top + divider.intrinsicHeight
                    divider.setBounds(fullMarginLeft, top, right, bottom)
                    divider.draw(canvas)
                }
            }
        }
    }
}
