package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringResourcesUtils.*
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_EMASTERONLY
import javax.inject.Inject

/**
 * Use case for removing MegaNodes.
 *
 * @property megaApi MegaApiAndroid instance to move nodes..
 */
class RemoveNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {
    /**
     * Removes a list of MegaNodes.
     *
     * @param handles   List of MegaNode handles to remove.
     * @return The removal result.
     */
    fun remove(handles: List<Long>): Single<String> =
        Single.create { emitter ->
            val count = handles.size
            var pending = count
            var success = 0
            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        success++
                    }

                    if (pending == 0) {
                        val errors = count - success
                        val message: String = when {
                            count == 1 && success == 1 -> {
                                getString(R.string.context_correctly_removed)
                            }
                            count == 1 && errors == 1 -> {
                                if (error.errorCode == API_EMASTERONLY) {
                                    getTranslatedErrorString(error)
                                } else {
                                    getString(R.string.context_no_removed)
                                }
                            }
                            errors == 0 -> {
                                getString(R.string.number_correctly_removed, success)
                            }
                            else -> {
                                getString(R.string.number_correctly_removed, success) +
                                        getString(R.string.number_no_removed, errors)
                            }
                        }

                        DBUtil.resetAccountDetailsTimeStamp()
                        emitter.onSuccess(message)
                    }
                })

            for (handle in handles) {
                val node = megaApi.getNodeByHandle(handle)

                if (node == null) {
                    pending--
                    continue
                }

                megaApi.remove(node, listener)
            }
        }
}