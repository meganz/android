package mega.privacy.android.domain.usecase.appstart

/**
 * Interface for a task that should be executed when the app starts.
 * Extend this interface to create a new app start task. The task should be added to the DI module
 */
fun interface AppStartTask {
    /**
     * Executes the task
     */
    suspend operator fun invoke()
}