package mega.privacy.android.navigation.contract

import kotlin.reflect.KClass

/**
 * Lightweight navigation options for back stack manipulation.
 *
 * Supports [launchSingleTop] and [popUpTo] operations.
 * Animation options are not supported as they are handled by the scene strategy.
 */
class NavOptions internal constructor(
    val launchSingleTop: Boolean,
    val popUpTo: PopUpTo?,
) {

    /**
     * Pop-up-to configuration specifying which destination to pop the back stack to.
     */
    class PopUpTo internal constructor(
        val routeClass: KClass<*>,
        val inclusive: Boolean,
    ) {

        /**
         * Builder for constructing [PopUpTo] instances via DSL.
         */
        class Builder @PublishedApi internal constructor(
            private val routeClass: KClass<*>,
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
         */
        var launchSingleTop: Boolean = false

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

        fun build() = NavOptions(launchSingleTop, popUpTo)
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
 *     popUpTo<Destination> {
 *         inclusive = true
 *     }
 * }
 * ```
 */
inline fun navOptions(block: NavOptions.Builder.() -> Unit): NavOptions =
    NavOptions.Builder().apply(block).build()
