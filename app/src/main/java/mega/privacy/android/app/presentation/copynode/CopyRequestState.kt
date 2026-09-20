package mega.privacy.android.app.presentation.copynode

import mega.privacy.android.shared.nodes.mapper.message.CopyRequestResult

/**
 * A temporary data class to represent the state of a copy request
 *
 * Should be removed when [mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity]
 * Java classes are converted to Kotlin
 * @property result [CopyRequestResult] The result of the copy request
 * @property error [CopyRequestResult] When copy request throws exception
 */
data class CopyRequestState(
    val result: CopyRequestResult? = null,
    val error: Throwable? = null,
)
