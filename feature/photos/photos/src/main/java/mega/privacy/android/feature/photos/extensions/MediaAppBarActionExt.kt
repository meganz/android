package mega.privacy.android.feature.photos.extensions

import mega.privacy.android.feature.photos.model.MediaAppBarAction
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import mega.privacy.mobile.analytics.event.MediaScreenFilterMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenMoreMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenSearchMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenSettingsMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenSortByMenuToolbarEvent

/**
 * Extension function to convert [MediaAppBarAction] to analytics event identifier
 */
internal fun MediaAppBarAction.toTrackingEvent(): EventIdentifier? = when (this) {
    is MediaAppBarAction.Search -> MediaScreenSearchMenuToolbarEvent
    is MediaAppBarAction.More -> MediaScreenMoreMenuToolbarEvent
    is MediaAppBarAction.FilterSecondary -> MediaScreenFilterMenuToolbarEvent
    is MediaAppBarAction.SortBy -> MediaScreenSortByMenuToolbarEvent
    is MediaAppBarAction.CameraUploadsSettings -> MediaScreenSettingsMenuToolbarEvent
    else -> null
}
