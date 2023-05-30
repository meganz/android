package mega.privacy.android.analytics.event.content

import mega.privacy.android.analytics.event.ScreenInfo

/**
 * Photos
 */
object PhotosScreenInfo : ScreenInfo {
    override val uniqueIdentifier = 200
    override val name = "screen_photos"
}

/**
 * SlideShowInfo
 */
object SlideShowInfo : ScreenInfo {
    override val uniqueIdentifier = 201
    override val name = "screen_slideshow"
}

