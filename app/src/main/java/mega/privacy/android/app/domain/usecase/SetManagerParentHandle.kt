package mega.privacy.android.app.domain.usecase

/**
 * Set current parent handle in manager section
 */
interface SetManagerParentHandle {
    /**
     * Invoke
     *
     * @param managerParentHandleType the type of the parent handle
     * @param parentHandle
     */
    operator fun invoke(managerParentHandleType: SetManagerParentHandleType, parentHandle: Long)
}

/**
 * Define the type of the parent handle
 */
enum class SetManagerParentHandleType {
    RubbishBin,
    Browser
}