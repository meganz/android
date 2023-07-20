package mega.privacy.android.app.presentation.filelink.model

import mega.privacy.android.app.namecollision.data.NameCollision

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.filelink.FileLinkActivity]
 *
 * @property shouldLogin        Whether to show login screen
 * @property url                Url of the file
 * @property collision          Node with existing names
 * @property copyThrowable      Throwable error on copy
 * @property copySuccess        Whether copy was success or not
 * @property snackBarMessageId  String id of content for snack bar
 */
data class FileLinkState(
    val shouldLogin: Boolean? = null,
    val url: String? = null,
    val collision: NameCollision? = null,
    val copyThrowable: Throwable? = null,
    val copySuccess: Boolean = false,
    val snackBarMessageId: Int = -1
)
