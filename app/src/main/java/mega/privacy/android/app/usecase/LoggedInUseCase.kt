package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.utils.MegaApiUtil.isUserLoggedIn
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * Use case to check if the user is logged in.
 *
 * @property megaApi    Mega API needed to check the login status.
 */
class LoggedInUseCase @Inject constructor(
        @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Check if the user is logged in.
     *
     * @return  True if it's logged in, false otherwise
     */
    fun isUserLoggedIn(): Single<Boolean> =
            Single.fromCallable { megaApi.isUserLoggedIn() }
}
