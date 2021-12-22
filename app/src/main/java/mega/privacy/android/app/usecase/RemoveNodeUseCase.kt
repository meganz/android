package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.data.RemoveActionResult
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.StringResourcesUtils.*
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
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
    fun remove(handles: List<Long>): Single<RemoveActionResult> =
        Single.create { emitter ->
            val count = handles.size
            var pending = count
            var success = 0
            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                    pending--

                    if (error.errorCode == MegaError.API_OK) {
                        success++
                    }

                    if (pending == 0) {
                        val errors = count - success
                        val result = when {
                            count == 1 && success == 1 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                RemoveActionResult(
                                    singleAction = true,
                                    resultText = getString(R.string.context_correctly_removed)
                                )
                            }
                            count == 1 && errors == 1 -> {
                                RemoveActionResult(
                                    resultText = if (error.errorCode == API_EMASTERONLY) {
                                        getTranslatedErrorString(error)
                                    } else {
                                        getString(R.string.context_no_removed)
                                    },
                                    allSuccess = false
                                )
                            }
                            errors == 0 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                RemoveActionResult(
                                    resultText = getString(
                                        R.string.number_correctly_removed,
                                        success
                                    )
                                )

                            }
                            else -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                val result = getString(R.string.number_correctly_removed, success) +
                                        getString(R.string.number_no_removed, errors)

                                RemoveActionResult(
                                    resultText = result,
                                    allSuccess = false
                                )
                            }
                        }

                        emitter.onSuccess(result)
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