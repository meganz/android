package mega.privacy.android.navigation.contract.initialisation

/**
 * Synchronous app create initialiser
 *
 */
interface SynchronousAppCreateInitialiser : AppCreateInitialiser {

    /**
     * Executes the initialisation work.
     */
    operator fun invoke()
}