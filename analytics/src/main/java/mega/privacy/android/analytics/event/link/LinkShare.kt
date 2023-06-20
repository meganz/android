package mega.privacy.android.analytics.event.link

import mega.privacy.android.analytics.event.GeneralInfo

/**
 * Data class track for sharing link
 */
data class LinkShare(
    override val uniqueIdentifier: Int,
    override val name: String = "Share",
    override val info: String?
) : GeneralInfo

/**
 * Info for sharing link for file
 */
const val LINK_SHARE_FILE_INFO = "Share file"

/**
 * Info for sharing link for file
 */
const val LINK_SHARE_FOLDER_INFO = "Share folder"