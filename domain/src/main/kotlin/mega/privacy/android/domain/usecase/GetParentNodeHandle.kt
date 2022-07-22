package mega.privacy.android.domain.usecase

/**
 * Get the parent node handle of a node
 */
fun interface GetParentNodeHandle {
    /**
     * Get the parent node of a node
     *
     * @param handle node handle
     * @return Parent node handle
     */
    suspend operator fun invoke(handle: Long): Long?
}