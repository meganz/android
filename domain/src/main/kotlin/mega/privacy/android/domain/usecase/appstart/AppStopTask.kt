package mega.privacy.android.domain.usecase.appstart

/**
 * Interface for a task that should be executed when the app stops.
 * Extend this interface to create a new app stop task. The task should be added to the DI module
 */
fun interface AppStopTask {
    /**
     * Executes the task
     */
    suspend operator fun invoke()
}