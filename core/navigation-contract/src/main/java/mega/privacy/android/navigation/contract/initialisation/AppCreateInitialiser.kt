package mega.privacy.android.navigation.contract.initialisation

/**
 * A unit of boot work executed once during `Application.onCreate`, after the dependency graph
 * exists. Units are injected as an explicitly ordered list and run by the boot orchestrator.
 *
 * Implementations must be ordinary injectable classes resolved from the regular singleton
 * component. Do NOT reach for `@EarlyEntryPoint` to run boot work before the component exists:
 * early entry points resolve against a separate early component, so in instrumented tests they
 * would construct a second SDK singleton graph that bypasses the test bindings. Running
 * initialisers after the component exists keeps production and tests on the same code path.
 */
interface AppCreateInitialiser {
    /**
     * Stable identifier for this unit, used for selective runs (e.g. excluding units in tests).
     */
    val name: String
}

/**
 * Convenience [AppCreateInitialiser] wrapping a given suspend [action].
 *
 * @property name Stable identifier for this unit.
 * @property isCritical Whether this unit is critical. See [AppCreateInitialiser] for semantics.
 */
open class AsyncAppCreateInitialiserAction(
    override val name: String,
    private val action: suspend () -> Unit,
) : AsyncAppCreateInitialiser {
    override suspend operator fun invoke() {
        action()
    }
}

/**
 * Convenience [AppCreateInitialiser] wrapping a given non-suspend [action].
 *
 * @property name Stable identifier for this unit.
 */
open class SynchronousAppCreateInitialiserAction(
    override val name: String,
    private val action: () -> Unit,
) : SynchronousAppCreateInitialiser {
    override operator fun invoke() {
        action()
    }
}
