package test.mega.privacy.android.app.presentation.manager.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.manager.model.mapper.InitialScreenMapper
import mega.privacy.android.app.presentation.manager.model.mapper.InitialScreenMapperImpl
import mega.privacy.android.domain.entity.preference.StartScreen
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class InitialScreenMapperImplTest {
    private val underTest: InitialScreenMapper = InitialScreenMapperImpl()

    @TestFactory
    fun `test mapping`() = listOf(
        StartScreen.CloudDrive to DrawerItem.CLOUD_DRIVE,
        StartScreen.Photos to DrawerItem.PHOTOS,
        StartScreen.Home to DrawerItem.HOMEPAGE,
        StartScreen.Chat to DrawerItem.CHAT,
        StartScreen.SharedItems to DrawerItem.SHARED_ITEMS,
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("test that $input is mapped to $expected") {
            assertThat(underTest(input)).isEqualTo(expected)
        }
    }

}