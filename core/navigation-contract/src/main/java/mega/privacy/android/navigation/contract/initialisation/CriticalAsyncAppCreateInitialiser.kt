package mega.privacy.android.navigation.contract.initialisation

/**
 * An [AsyncAppCreateInitialiser] that other boot phases depend on.
 *
 * The boot orchestrator runs all critical async initialisers to completion, sequentially in
 * their contribution order, before launching the remaining async app-create initialisers and
 * before running any app-start or post-login initialisers.
 */
interface CriticalAsyncAppCreateInitialiser : AsyncAppCreateInitialiser
