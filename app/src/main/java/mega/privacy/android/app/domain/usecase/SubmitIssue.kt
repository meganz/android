package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.Progress
import mega.privacy.android.app.domain.entity.SubmitIssueRequest

/**
 * Submit issue
 *
 */
interface SubmitIssue {
    /**
     * Invoke
     *
     * @param request
     * @return upload progress
     */
    suspend operator fun invoke(request: SubmitIssueRequest): Flow<Progress>
}
