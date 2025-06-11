package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import javax.inject.Inject

/**
 * Use Case that checks if the User is logged in to the app.
 *
 * Note This use case is used to determine if the user has an active session.
 * This does not mean that megaApi is ready to use for any logged in request. For that,
 * use [IsUserLoggedInUseCase] + [RootNodeExistsUseCase] != null instead.
 */
class IsUserLoggedInUseCase @Inject constructor(
    private val getSessionUseCase: GetSessionUseCase,
) {
    /**
     * Invocation function.
     *
     * @return true if the User is logged in
     */
    suspend operator fun invoke() = getSessionUseCase() != null
}