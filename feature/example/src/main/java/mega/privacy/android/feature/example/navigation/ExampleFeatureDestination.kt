package mega.privacy.android.feature.example.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.feature.example.presentation.exampleSecondaryScreen
import mega.privacy.android.navigation.contract.FeatureDestination

class ExampleFeatureDestination : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(() -> Unit, (Any) -> Unit) -> Unit =
        { onBack, onNavigate ->
            exampleSecondaryScreen(onBack)
        }

}