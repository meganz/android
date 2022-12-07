package mega.privacy.android.domain.usecase

/**
 * Send broadcast to update folder icon for camera upload
 *
 */
fun interface UpdateFolderIconBroadcast {

    /**
     * Invoke
     *
     * @param nodeHandle    updated node handle
     * @param isSecondary   if updated node handle is secondary media
     */
    suspend operator fun invoke(nodeHandle: Long, isSecondary: Boolean)
}
