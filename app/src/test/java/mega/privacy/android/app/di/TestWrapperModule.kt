package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.data.facade.security.SetLogoutFlagWrapper
import mega.privacy.android.data.gateway.global.SetupMegaChatApiWrapper
import mega.privacy.android.data.wrapper.ApplicationIpAddressWrapper
import mega.privacy.android.data.wrapper.AvatarWrapper
import mega.privacy.android.data.wrapper.StringWrapper
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [UtilWrapperModule::class]
)
object TestWrapperModule {

    val permissionUtilWrapper = mock<PermissionUtilWrapper>()
    val fetchNodeWrapper = mock<FetchNodeWrapper>()
    val avatarWrapper = mock<AvatarWrapper>()

    @Provides
    fun providePermissionUtilWrapper(): PermissionUtilWrapper = permissionUtilWrapper

    @Provides
    fun provideFetchNodeWrapper(): FetchNodeWrapper = fetchNodeWrapper

    @Provides
    fun provideAvatarWrapper(): AvatarWrapper = avatarWrapper

    @Provides
    fun provideMegaNodeUtilWrapper(): MegaNodeUtilWrapper = mock()

    @Provides
    fun providesFileUtilWrapper(): FileUtilWrapper = mock()

    @Provides
    fun provideSetLogoutFlagWrapper(): SetLogoutFlagWrapper = mock()

    @Provides
    fun provideStringWrapper(): StringWrapper = mock()

    @Provides
    fun provideApplicationIpAddressWrapper(): ApplicationIpAddressWrapper = mock()

    @Provides
    fun provideSetupMegaChatApiWrapper(): SetupMegaChatApiWrapper = mock()

}
