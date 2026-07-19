package mega.privacy.android.navigation.contract.initialisation

/**
 * Async app create initialiser
 *
 */
interface AsyncAppCreateInitialiser : AppCreateInitialiser {

    /**
     * Executes the initialisation work.
     */
    suspend operator fun invoke()
}