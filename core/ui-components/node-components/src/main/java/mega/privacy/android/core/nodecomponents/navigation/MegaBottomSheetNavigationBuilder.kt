package mega.privacy.android.core.nodecomponents.navigation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import kotlin.reflect.KClass
import kotlin.reflect.KType

inline fun <reified T : Any> NavGraphBuilder.megaBottomSheet(
    deepLinks: List<NavDeepLink> = emptyList(),
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    noinline content: @Composable ColumnScope.(backstackEntry: NavBackStackEntry) -> Unit,
) {
    destination(
        MegaBottomSheetNavigatorDestinationBuilder(
            provider.getNavigator(MegaBottomSheetNavigator::class.java),
            T::class,
            typeMap,
            content
        )
            .apply {
                deepLinks.forEach { deepLink -> deepLink(deepLink) }
            }
    )
}

@NavDestinationDsl
class MegaBottomSheetNavigatorDestinationBuilder :
    NavDestinationBuilder<MegaBottomSheetNavigator.BottomSheetDestination> {

    private val bottomSheetNavigator: MegaBottomSheetNavigator
    private val content: @Composable ColumnScope.(NavBackStackEntry) -> Unit

    constructor(
        navigator: MegaBottomSheetNavigator,
        route: KClass<*>,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
        content: @Composable ColumnScope.(NavBackStackEntry) -> Unit,
    ) : super(navigator, route, typeMap) {
        this.bottomSheetNavigator = navigator
        this.content = content
    }

    override fun instantiateDestination(): MegaBottomSheetNavigator.BottomSheetDestination =
        MegaBottomSheetNavigator.BottomSheetDestination(bottomSheetNavigator, content)

    override fun build(): MegaBottomSheetNavigator.BottomSheetDestination = super.build()
}