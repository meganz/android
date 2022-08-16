package mega.privacy.android.domain.usecase

/**
 * Get remove GPS setting
 *
 */
fun interface GetRemoveGps {

    /**
     * Invoke
     *
     * @return remove GPS setting
     */
    suspend operator fun invoke(): Boolean
}
