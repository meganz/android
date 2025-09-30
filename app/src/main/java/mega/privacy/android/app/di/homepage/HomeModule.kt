package mega.privacy.android.app.di.homepage

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetProvider

@Module
@InstallIn(SingletonComponent::class)
class HomeModule {

    @Provides
    @IntoSet
    fun provideInjectedWidgetProvider(widgets: Set<@JvmSuppressWildcards HomeWidget>): HomeWidgetProvider =
        object : HomeWidgetProvider {
            override suspend fun getWidgets() = widgets

            override suspend fun deleteWidget(identifier: String) = false
        }
}