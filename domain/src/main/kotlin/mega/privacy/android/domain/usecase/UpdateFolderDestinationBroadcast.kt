package mega.privacy.android.domain.usecase

/**
 * Send broadcast to update folder destination for camera upload
 *
 */
fun interface UpdateFolderDestinationBroadcast {

    /**
     * Invoke
     *
     * @param nodeHandle    updated node handle
     * @param isSecondary   if updated node handle is secondary folder
     */
    suspend operator fun invoke(nodeHandle: Long, isSecondary: Boolean)
}
