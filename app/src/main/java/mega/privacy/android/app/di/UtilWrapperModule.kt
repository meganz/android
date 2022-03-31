package mega.privacy.android.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.wrapper.IsOnlineWrapper

/**
 * Util wrapper module
 *
 * Module for providing temporary wrappers around static util methods. All of these dependencies
 * need to be removed during the refactoring process.
 */
@Module
@InstallIn(FragmentComponent::class)
class UtilWrapperModule {

    @Provides
    fun provideIsOnlineWrapper(): IsOnlineWrapper {
        return object : IsOnlineWrapper {
            override fun isOnline(context: Context): Boolean {
                return Util.isOnline(context)
            }
        }
    }
}