package mega.privacy.android.data.repository

import mega.privacy.android.data.gateway.ClipboardGateway
import mega.privacy.android.domain.repository.ClipboardRepository
import javax.inject.Inject

/**
 * Repository implementtation of [ClipboardRepository]
 *
 */
class DefaultClipboardRepository @Inject constructor(
    private val clipboardGateway: ClipboardGateway,
) : ClipboardRepository {

    override fun setClip(label: String, text: String) = clipboardGateway.setClip(label, text)
}