package mega.privacy.android.core.sharedcomponents.semantics

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Semantics key carrying the drawable resource a composable is showing.
 *
 * Compose exposes no way to assert which drawable an `Image` rendered, so composables publish the
 * id themselves and tests match on it with `hasDrawable`. It lives in production code because
 * production composables write it; the matcher that reads it lives in `:core-ui-test`.
 */
val DrawableResId = SemanticsPropertyKey<Int>("DrawableResId")

/**
 * Publishes [DrawableResId] for the composable being described.
 */
var SemanticsPropertyReceiver.drawableId by DrawableResId
