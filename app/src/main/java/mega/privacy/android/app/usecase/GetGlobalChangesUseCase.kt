package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaGlobalListenerInterface
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to subscribe to global events related to MegaApi.
 *
 * @property megaApi    MegaApi required to call the SDK
 */
@Singleton
class GetGlobalChangesUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    sealed class Result {
        data class OnUsersUpdate(val users: List<MegaUser>?) : Result()
        data class OnUserAlertsUpdate(val userAlerts: List<MegaUserAlert>?) : Result()
        data class OnNodesUpdate(val nodes: List<MegaNode>?) : Result()
        data class OnContactRequestsUpdate(val contactRequests: List<MegaContactRequest>?) : Result()
        data class OnEvent(val event: MegaEvent) : Result()
        data class OnSetsUpdate(val sets: List<MegaSet>?) : Result()
        data class OnSetElementsUpdate(val elements: List<MegaSetElement>?) : Result()
        object OnReloadNeeded : Result()
        object OnAccountUpdate : Result()
    }

    fun get(): Flowable<Result> =
        Flowable.create({ emitter ->
            val listener = OptionalMegaGlobalListenerInterface(
                onUsersUpdate = { users ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnUsersUpdate(users))
                    }
                },
                onUserAlertsUpdate = { userAlerts ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnUserAlertsUpdate(userAlerts))
                    }
                },
                onNodesUpdate = { nodes ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnNodesUpdate(nodes))
                    }
                },
                onContactRequestsUpdate = { contactRequests ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnContactRequestsUpdate(contactRequests))
                    }
                },
                onReloadNeeded = {
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnReloadNeeded)
                    }
                },
                onAccountUpdate = {
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnAccountUpdate)
                    }
                },
                onEvent = { event ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnEvent(event))
                    }
                },
                onSetsUpdate = { sets ->
                    if (!emitter.isCancelled) {
                        emitter.onNext((Result.OnSetsUpdate(sets)))
                    }
                },
                onSetElementsUpdate = { elements ->
                    if (!emitter.isCancelled) {
                        emitter.onNext((Result.OnSetElementsUpdate(elements)))
                    }
                }
            )

            megaApi.addGlobalListener(listener)

            emitter.setCancellable {
                megaApi.removeGlobalListener(listener)
            }
        }, BackpressureStrategy.BUFFER)
}
