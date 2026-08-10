package mega.privacy.android.feature.contact.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.contact.navigation.ContactFeatureDestination
import mega.privacy.android.feature.contact.navigation.ContactFeatureDialogDestinations
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations

/**
 * Contact module
 */
@Module
@InstallIn(SingletonComponent::class)
class ContactModule {

    /**
     * Provide contact feature destination
     */
    @Provides
    @IntoSet
    fun provideContactFeatureDestination(): FeatureDestination = ContactFeatureDestination()

    /**
     * Provide contact dialog destinations
     */
    @Provides
    @IntoSet
    fun provideContactDialogDestinations(): AppDialogDestinations =
        ContactFeatureDialogDestinations()

}