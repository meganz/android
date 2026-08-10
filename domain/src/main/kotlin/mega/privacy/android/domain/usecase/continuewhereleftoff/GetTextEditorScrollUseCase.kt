package mega.privacy.android.domain.usecase.continuewhereleftoff

import mega.privacy.android.domain.entity.continuewhereleftoff.TextEditorScroll
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject

/**
 * Returns saved text editor state for a node, or null if none exists.
 */
class GetTextEditorScrollUseCase @Inject constructor(
    private val repository: ContinueWhereLeftOffRepository,
) {
    suspend operator fun invoke(nodeHandle: Long): TextEditorScroll? =
        repository.getTextEditorScroll(nodeHandle)
}
