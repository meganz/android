package mega.privacy.android.app.presentation.favourites.adapter

import android.view.View
import android.view.animation.Animation
import androidx.recyclerview.widget.RecyclerView

/**
 * Selectable
 *
 */
abstract class Selectable(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun animate(listener: Animation.AnimationListener, isSelected: Boolean)
}


/**
 * Selected To Not Selected
 */
const val SELECTED_TO_NOT_SELECTED = 123

/**
 * Not Selected To Selected
 */
const val NOT_SELECTED_TO_SELECTED = 321
