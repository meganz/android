package mega.privacy.android.app.components

import android.content.Context
import android.graphics.Canvas
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.list.adapter.MeetingsAdapter

class ChatDividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    companion object {
        private const val VIEWTYPE_DEFAULT = 0
    }

    private val divider = ContextCompat.getDrawable(context, R.drawable.line_divider)
        ?: error("Divider doesn't exist")

    private val marginStart =
        context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_item_divider_margin_start)

    private val shortMarginStart =
        context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_item_divider_short_margin_start)

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) return

            val adapter = parent.adapter ?: return
            when (adapter.getItemViewType(position)) {
                VIEWTYPE_DEFAULT -> {
                    canvas.drawDivider(child, marginStart)
                }
                MeetingsAdapter.TYPE_DATA -> {
                    val nextItem = position + 1
                    if (nextItem < adapter.itemCount
                        && adapter.getItemViewType(nextItem) == MeetingsAdapter.TYPE_HEADER
                    ) {
                        canvas.drawDivider(child, shortMarginStart)
                    } else {
                        canvas.drawDivider(child, marginStart)
                    }
                }
                else -> {
                    // no divider required
                }
            }
        }
    }

    private fun Canvas.drawDivider(view: View, shortMarginLeft: Int) {
        val params = view.layoutParams as RecyclerView.LayoutParams
        val parentView = view.parent as RecyclerView
        val right = parentView.width - parentView.paddingRight
        val top = view.bottom + params.bottomMargin
        val bottom = top + divider.intrinsicHeight
        divider.setBounds(shortMarginLeft, top, right, bottom)
        divider.draw(this)
    }
}
