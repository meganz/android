package mega.privacy.android.app.presentation.favourites.adapter

import android.view.animation.Animation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

class SelectAnimator : DefaultItemAnimator() {
    class SelectItemHolderInfo(val newSelectedStatus: Boolean) : ItemHolderInfo()

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder,
        preLayoutInfo: ItemHolderInfo,
        postLayoutInfo: ItemHolderInfo,
    ): Boolean {
        val holder = newHolder as? Selectable ?: return false

        if (preLayoutInfo is SelectItemHolderInfo) {
            holder.animate(getListener(holder), preLayoutInfo.newSelectedStatus)
            return true
        }
        return super.animateChange(oldHolder, newHolder, preLayoutInfo, postLayoutInfo)
    }

    private fun getListener(holder: Selectable) =
        (object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                dispatchAnimationFinished(holder)
            }

        })

    override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder) = true

    override fun recordPreLayoutInformation(
        state: RecyclerView.State,
        viewHolder: RecyclerView.ViewHolder,
        changeFlags: Int,
        payloads: MutableList<Any>,
    ): ItemHolderInfo {
        if (changeFlags == FLAG_CHANGED) {
            for (payload in payloads) {
                if (payload as? Int == SELECTED_TO_NOT_SELECTED) {
                    return SelectItemHolderInfo(false)
                } else if (payload as? Int == NOT_SELECTED_TO_SELECTED) {
                    return SelectItemHolderInfo(true)
                }
            }
        }

        return super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads)
    }
}