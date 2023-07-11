package mega.privacy.android.feature.devicecenter.data.repository

import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [DeviceCenterRepository]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceCenterRepositoryImplTest {
    private lateinit var underTest: DeviceCenterRepository

    @BeforeAll
    fun setUp() {
        underTest = DeviceCenterRepositoryImpl()
    }
}