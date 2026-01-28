package mega.privacy.android.core.sharedcomponents.extension

import android.content.res.Configuration

/**
 * Extension function to check if the orientation is landscape
 */
fun Configuration.isLandscape() = this.orientation == Configuration.ORIENTATION_LANDSCAPE