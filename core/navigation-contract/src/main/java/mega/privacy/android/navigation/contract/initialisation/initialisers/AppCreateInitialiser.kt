package mega.privacy.android.navigation.contract.initialisation.initialisers

/**
 * A unit of boot work executed once during `Application.onCreate`, after the dependency graph
 * exists. Units are injected as an explicitly ordered list and run by the boot orchestrator.
 *
 * Execution semantics depend on [isCritical]:
 * - **Critical** units run synchronously in list order; a failure PROPAGATES to the caller and
 *   aborts the boot. Use this for work later code depends on (e.g. SDK configuration, crash
 *   handlers) that must never be silently skipped.
 * - **Non-critical** units are launched fire-and-forget into the application scope; failures are
 *   caught and logged, never affecting other units or the boot.
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

    /**
     * Whether this unit is critical. See the interface documentation for execution semantics.
     */
    val isCritical: Boolean

    /**
     * Executes the initialisation work.
     */
    suspend operator fun invoke()
}

/**
 * Convenience [AppCreateInitialiser] wrapping a given suspend [action].
 *
 * @property name Stable identifier for this unit.
 * @property isCritical Whether this unit is critical. See [AppCreateInitialiser] for semantics.
 */
open class AppCreateInitialiserAction(
    override val name: String,
    override val isCritical: Boolean,
    private val action: suspend () -> Unit,
) : AppCreateInitialiser {
    override suspend operator fun invoke() {
        action()
    }
}
