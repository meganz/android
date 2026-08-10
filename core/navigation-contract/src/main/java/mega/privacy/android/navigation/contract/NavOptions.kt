package mega.privacy.android.navigation.contract

import kotlin.reflect.KClass

/**
 * Lightweight navigation options for back stack manipulation.
 *
 * Supports [launchSingleTop], [dropIfAlreadyShown] and [popUpTo] operations.
 * Animation options are not supported as they are handled by the scene strategy.
 */
class NavOptions internal constructor(
    val launchSingleTop: Boolean,
    val dropIfAlreadyShown: Boolean,
    val popUpTo: PopUpTo?,
) {

    /**
     * Pop-up-to configuration specifying which destination to pop the back stack to.
     *
     * When [routeClass] is `null`, the pop targets the root of the back stack
     * (equivalent to `popUpTo(0)` in `androidx.navigation`).
     */
    class PopUpTo internal constructor(
        val routeClass: KClass<*>?,
        val inclusive: Boolean,
    ) {

        /**
         * Builder for constructing [PopUpTo] instances via DSL.
         */
        class Builder @PublishedApi internal constructor(
            private val routeClass: KClass<*>?,
        ) {
            /**
             * Whether the destination itself should be popped from the back stack.
             */
            var inclusive: Boolean = false

            fun build() = PopUpTo(routeClass, inclusive)
        }
    }

    /**
     * Builder for constructing [NavOptions] instances via DSL.
     */
    class Builder {
        /**
         * Whether the destination should be launched as single top.
         *
         * When `true`, an existing instance of the same destination at the top of the back stack
         * is removed and the new one is added in its place.
         */
        var launchSingleTop: Boolean = false

        /**
         * Whether the navigation should be dropped when the destination is already on the back stack.
         *
         * When `true`, if an instance of the same destination is already present anywhere on the
         * back stack, the navigation request is ignored and the existing instance is kept in place.
         * Unlike [launchSingleTop], no existing entry is removed or moved.
         */
        var dropIfAlreadyShown: Boolean = false

        @PublishedApi
        internal var popUpTo: PopUpTo? = null

        /**
         * Pop up to a destination identified by its reified type.
         */
        inline fun <reified T : Any> popUpTo(block: PopUpTo.Builder.() -> Unit = {}) {
            popUpTo = PopUpTo.Builder(routeClass = T::class).apply(block).build()
        }

        fun popUpTo(routeClass: KClass<*>, block: PopUpTo.Builder.() -> Unit = {}) {
            popUpTo = PopUpTo.Builder(routeClass = routeClass).apply(block).build()
        }

        /**
         * Pop up to the root of the back stack, equivalent to `popUpTo(0)` in
         * `androidx.navigation`.
         */
        fun popUpToRoot(block: PopUpTo.Builder.() -> Unit = {}) {
            popUpTo = PopUpTo.Builder(routeClass = null).apply(block).build()
        }

        fun build() = NavOptions(launchSingleTop, dropIfAlreadyShown, popUpTo)
    }
}

/**
 * DSL function for building [NavOptions].
 *
 * Usage:
 * ```
 * navOptions {
 *     launchSingleTop = true
 * }
 *
 * navOptions {
 *     dropIfAlreadyShown = true
 * }
 *
 * navOptions {
 *     popUpTo<Destination> {
 *         inclusive = true
 *     }
 * }
 *
 * navOptions {
 *     popUpToRoot {
 *         inclusive = true
 *     }
 * }
 * ```
 */
inline fun navOptions(block: NavOptions.Builder.() -> Unit): NavOptions =
    NavOptions.Builder().apply(block).build()
