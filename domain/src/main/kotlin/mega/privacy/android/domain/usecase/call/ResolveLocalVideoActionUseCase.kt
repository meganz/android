package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.LocalVideoAction
import mega.privacy.android.domain.entity.call.LocalVideoContext
import mega.privacy.android.domain.entity.call.LocalVideoTrigger
import javax.inject.Inject

/**
 * Pure decision logic for the in-call local-video state machine (screen-lock + proximity sensor).
 *
 * Given a [LocalVideoTrigger] and an immutable [LocalVideoContext] snapshot, it returns the ordered
 * list of [LocalVideoAction]s the caller must apply. It mutates nothing and calls nothing, so it can
 * be unit-tested exhaustively with no Android, SDK, coroutine or static dependencies.
 *
 * It replicates the behaviour previously buried in `ChatManagement.screenOnOffReceiver` and
 * `ChatManagement.controlProximitySensor`.
 */
class ResolveLocalVideoActionUseCase @Inject constructor() {

    /**
     * Resolve the actions for a trigger against the current context.
     *
     * @param trigger The [LocalVideoTrigger] that fired.
     * @param context The current [LocalVideoContext] snapshot.
     * @return An ordered list of [LocalVideoAction]s to apply, or an empty list if nothing should happen.
     */
    operator fun invoke(
        trigger: LocalVideoTrigger,
        context: LocalVideoContext,
    ): List<LocalVideoAction> {
        val call = context.callInProgress ?: return emptyList()

        if (!call.status.isJoiningOrInProgress()) return emptyList()

        return when (trigger) {
            LocalVideoTrigger.ScreenOff -> resolveScreenOff(context, call)
            LocalVideoTrigger.UserPresent -> resolveUserPresent(context, call)
            LocalVideoTrigger.ProximityNear -> resolveProximity(context, call, isNear = true)
            LocalVideoTrigger.ProximityFar -> resolveProximity(context, call, isNear = false)
        }
    }

    private fun resolveScreenOff(
        context: LocalVideoContext,
        call: LocalVideoContext.CallInProgress,
    ): List<LocalVideoAction> {
        if (!context.isVideoEnabled) {
            return listOf(LocalVideoAction.SetTemporaryState(false))
        }
        val actions = mutableListOf<LocalVideoAction>()
        actions += LocalVideoAction.MuteMic
        actions += LocalVideoAction.SetDisablingLocalVideo(false)
        actions += LocalVideoAction.SetTemporaryState(true)
        if (call.hasLocalVideo) {
            actions += LocalVideoAction.SetScreenOn(false)
            actions += LocalVideoAction.DisableVideo(call.chatId)
        }
        return actions
    }

    private fun resolveUserPresent(
        context: LocalVideoContext,
        call: LocalVideoContext.CallInProgress,
    ): List<LocalVideoAction> {
        if (!context.isVideoEnabled) {
            return listOf(LocalVideoAction.SetTemporaryState(false))
        }
        val actions = mutableListOf<LocalVideoAction>()
        actions += LocalVideoAction.SetDisablingLocalVideo(false)
        actions += LocalVideoAction.SetTemporaryState(false)
        if (!call.hasLocalVideo) {
            actions += LocalVideoAction.SetScreenOn(true)
            actions += LocalVideoAction.EnableVideo(call.chatId)
        }
        return actions
    }

    private fun resolveProximity(
        context: LocalVideoContext,
        call: LocalVideoContext.CallInProgress,
        isNear: Boolean,
    ): List<LocalVideoAction> {
        if (!context.isScreenOn || !context.isFrontCameraInUse) return emptyList()

        if (!context.isVideoEnabled) {
            return listOf(LocalVideoAction.SetTemporaryState(false))
        }

        val actions = mutableListOf<LocalVideoAction>()
        if (isNear) {
            actions += LocalVideoAction.SetDisablingLocalVideo(false)
            actions += LocalVideoAction.SetTemporaryState(true)
            actions += LocalVideoAction.BroadcastLocalVideoChanged(false)
            if (call.hasLocalVideo) {
                actions += LocalVideoAction.DisableVideo(call.chatId)
            }
        } else {
            actions += LocalVideoAction.SetDisablingLocalVideo(false)
            actions += LocalVideoAction.SetTemporaryState(false)
            actions += LocalVideoAction.BroadcastLocalVideoChanged(true)
            if (!call.hasLocalVideo) {
                actions += LocalVideoAction.EnableVideo(call.chatId)
            }
        }
        return actions
    }

    private fun ChatCallStatus?.isJoiningOrInProgress() =
        this == ChatCallStatus.Joining || this == ChatCallStatus.InProgress
}
