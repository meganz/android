package mega.privacy.android.app.domain.usecase

/**
 * Get current parent handle in manager section
 */
interface GetManagerParentHandle {
    /**
     * Invoke
     *
     * @return a Long corresponding to the parent handle
     */
    operator fun invoke(managerParentHandleType: GetManagerParentHandleType): Long
}

/**
 * Define the type of the parent handle
 */
enum class GetManagerParentHandleType {
    RubbishBin,
    Browser
}