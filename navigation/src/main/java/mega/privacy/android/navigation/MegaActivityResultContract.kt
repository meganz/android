package mega.privacy.android.navigation

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.chat.SendToChatResult
import mega.privacy.android.navigation.camera.CameraArg

/**
 * Centralized interface for all activity result contracts used in the MEGA application.
 *
 * Provides type-safe contracts for handling various user interactions and navigation flows
 * using the AndroidX Activity Result API.
 *
 * @see ActivityResultContract
 * @see SendToChatResult
 */
interface MegaActivityResultContract {

    /**
     * Contract for file version management.
     *
     * @return [ActivityResultContract] with input type [Long] (file handle) and output type [Long?] (selected version handle)
     */
    val versionsFileActivityResultContract: ActivityResultContract<Long, Long?>

    /**
     * Contract for selecting destination folder for move operations.
     *
     * @return [ActivityResultContract] with input type [LongArray] (node handles to move)
     *         and output type [Pair<LongArray, Long>?] (node handles and target folder handle)
     */
    val selectFolderToMoveActivityResultContract: ActivityResultContract<LongArray, Pair<LongArray, Long>?>

    /**
     * Contract for selecting destination folder for copy operations.
     *
     * @return [ActivityResultContract] with input type [LongArray] (node handles to copy)
     *         and output type [Pair<LongArray, Long>?] (node handles and target folder handle)
     */
    val selectFolderToCopyActivityResultContract: ActivityResultContract<LongArray, Pair<LongArray, Long>?>

    /**
     * Contract for sharing folders with contacts.
     *
     * @return [ActivityResultContract] with input type [LongArray] (folder handles to share)
     *         and output type [Pair<List<String>, List<Long>>?] (contact IDs and shared folder handles)
     */
    val shareFolderActivityResultContract: ActivityResultContract<LongArray, Pair<List<String>, List<Long>>?>

    /**
     * Contract for sending files/folders to chat conversations.
     *
     * @return [ActivityResultContract] with input type [LongArray] (node handles to send)
     *         and output type [SendToChatResult?] (result containing node, chat, and user handles)
     */
    val sendToChatActivityResultContract: ActivityResultContract<LongArray, SendToChatResult?>

    /**
     * Contract for hidden nodes onboarding process.
     *
     * @return [ActivityResultContract] with input type [Boolean] (onboarding state)
     *         and output type [Boolean] (onboarding completion status)
     */
    val hiddenNodeOnboardingActivityResultContract: ActivityResultContract<Boolean, Boolean>

    /**
     * Contract for in-app camera functionality.
     */
    val inAppCameraResultContract: ActivityResultContract<CameraArg, Uri?>

    /**
     * Contract for handling name collisions in file operations.
     *
     * @return [ActivityResultContract] with input type [ArrayList<NameCollision>] (list of name collisions)
     *         and output type [String?] (resulting file name or null if no resolution was made)
     */
    val nameCollisionActivityContract: ActivityResultContract<ArrayList<NameCollision>, String?>
}