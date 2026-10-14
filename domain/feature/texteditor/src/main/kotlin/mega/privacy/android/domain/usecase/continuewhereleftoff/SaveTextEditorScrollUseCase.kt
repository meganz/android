package mega.privacy.android.domain.usecase.continuewhereleftoff

import mega.privacy.android.domain.entity.continuewhereleftoff.TextEditorScroll
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject

/**
 * Saves text editor cursor and scroll position.
 */
class SaveTextEditorScrollUseCase @Inject constructor(
    private val repository: ContinueWhereLeftOffRepository,
) {
    suspend operator fun invoke(textEditorScroll: TextEditorScroll) =
        repository.saveTextEditorScroll(textEditorScroll)
}
