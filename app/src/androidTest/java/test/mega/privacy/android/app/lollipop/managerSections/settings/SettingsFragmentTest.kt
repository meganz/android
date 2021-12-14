package test.mega.privacy.android.app.lollipop.managerSections.settings

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.AppModule
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.app.lollipop.managerSections.settings.SettingsFragment
import mega.privacy.android.app.wrapper.InjectWrapper
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import test.mega.privacy.android.app.launchFragmentInHiltContainer
import javax.inject.Singleton

//private val chatSdk = mock<MegaChatApiAndroid>()
//private val sdk = mock<MegaApiAndroid>()
//private val databaseHandler = mock<DatabaseHandler>()

@HiltAndroidTest
@UninstallModules(AppModule::class)
class SettingsFragmentTest{

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Module
    @InstallIn(SingletonComponent::class)
    object TestAppModule {

        @Provides
        fun provideApiInjectWrapper(): InjectWrapper<MegaApiAndroid> {
            return object : InjectWrapper<MegaApiAndroid> {
                override fun get(): MegaApiAndroid {
//                    return sdk
                    throw NotImplementedError()
                }
            }
        }

        @Provides
        fun provideChatApiInjectWrapper(): InjectWrapper<MegaChatApiAndroid> {
            return object : InjectWrapper<MegaChatApiAndroid> {
                override fun get(): MegaChatApiAndroid {
                    throw NotImplementedError()
//                    return chatSdk
                }
            }
        }

        @Provides
        fun provideDatabaseHandlerInjectWrapper(): InjectWrapper<DatabaseHandler> {
            return object : InjectWrapper<DatabaseHandler> {
                override fun get(): DatabaseHandler {
                    throw NotImplementedError()
//                    return databaseHandler
                }
            }
        }


        @MegaApi
        @Singleton
        @Provides
        fun provideMegaApi(): MegaApiAndroid {
            throw NotImplementedError()
        }

        @MegaApiFolder
        @Singleton
        @Provides
        fun provideMegaApiFolder(): MegaApiAndroid {
            throw NotImplementedError()
        }

        @Singleton
        @Provides
        fun provideMegaChatApi(@MegaApi megaApi: MegaApiAndroid): MegaChatApiAndroid {
            throw NotImplementedError()
        }

        @Singleton
        @Provides
        fun provideDbHandler(@ApplicationContext context: Context): DatabaseHandler {
            throw NotImplementedError()
        }
    }

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun example_fragment_test() {
        val scenario = launchFragmentInHiltContainer<SettingsFragment>()
    }
}