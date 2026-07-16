package mega.privacy.android.app.di.appstate

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.appstate.global.initialisation.appcreate.AccountDefaultsInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.ApiServerInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.CoilImageLoaderInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.FcmTopicInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.GreeterInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.MiscFlagsInitialiser
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppCreateInitialiser

/**
 * Provides the app-create initialisers as a single explicitly ordered list.
 *
 * Ordering is part of the boot contract: critical units run synchronously in this order, so any
 * addition or reordering must be deliberate and reviewed.
 */
@Module
@InstallIn(SingletonComponent::class)
internal class AppCreateInitialisersModule {

    @Provides
    fun provideAppCreateInitialisers(
        coilImageLoaderInitialiser: CoilImageLoaderInitialiser,
        miscFlagsInitialiser: MiscFlagsInitialiser,
        apiServerInitialiser: ApiServerInitialiser,
        accountDefaultsInitialiser: AccountDefaultsInitialiser,
        greeterInitialiser: GreeterInitialiser,
        fcmTopicInitialiser: FcmTopicInitialiser,
    ): List<@JvmSuppressWildcards AppCreateInitialiser> = listOf(
        coilImageLoaderInitialiser,
        miscFlagsInitialiser,
        apiServerInitialiser,
        accountDefaultsInitialiser,
        greeterInitialiser,
        fcmTopicInitialiser,
    )
}
