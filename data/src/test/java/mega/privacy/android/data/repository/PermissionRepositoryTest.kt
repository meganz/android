package mega.privacy.android.data.repository

import android.os.Build
import android.os.Environment
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.gateway.PermissionGateway
import mega.privacy.android.domain.repository.PermissionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PermissionRepositoryTest {

    private val permissionGateway: PermissionGateway = mock()

    private lateinit var permissionRepository: PermissionRepository

    @BeforeAll
    fun setUp() {
        permissionRepository = PermissionRepositoryImpl(permissionGateway)
    }

    @BeforeEach
    fun reset() {
        reset(permissionGateway)
    }

    @Test
    fun `test hasMediaPermission returns true when permissions are granted`() {
        whenever(
            permissionGateway.hasPermissions(
                permissionGateway.getImagePermissionByVersion(),
                permissionGateway.getVideoPermissionByVersion()
            )
        ).thenReturn(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            whenever(permissionGateway.hasPermissions(permissionGateway.getPartialMediaPermission()))
                .thenReturn(true)
        }

        val result = permissionRepository.hasMediaPermission()
        assertThat(result).isTrue()
    }

    @Test
    fun `test hasMediaPermission returns false when permissions are not granted`() {
        whenever(
            permissionGateway.hasPermissions(
                permissionGateway.getImagePermissionByVersion(),
                permissionGateway.getVideoPermissionByVersion()
            )
        ).thenReturn(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            whenever(permissionGateway.hasPermissions(permissionGateway.getPartialMediaPermission()))
                .thenReturn(false)
        }

        val result = permissionRepository.hasMediaPermission()
        assertThat(result).isFalse()
    }

    @ParameterizedTest
    @MethodSource("provideAudioPermissions")
    fun `test hasAudioPermission returns correct value based on permission`(
        audioPermissionGranted: Boolean,
        expected: Boolean,
    ) {
        whenever(permissionGateway.hasPermissions(permissionGateway.getAudioPermissionByVersion()))
            .thenReturn(audioPermissionGranted)

        val result = permissionRepository.hasAudioPermission()
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test hasManageExternalStoragePermission returns true when permission is granted`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            whenever(Environment.isExternalStorageManager()).thenReturn(true)
        } else {
            whenever(permissionGateway.hasPermissions(permissionGateway.getReadExternalStoragePermission()))
                .thenReturn(true)
        }

        val result = permissionRepository.hasManageExternalStoragePermission()
        assertThat(result).isTrue()
    }

    @Test
    fun `test hasManageExternalStoragePermission returns false when permission is not granted`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            whenever(Environment.isExternalStorageManager()).thenReturn(false)
        } else {
            whenever(permissionGateway.hasPermissions(permissionGateway.getReadExternalStoragePermission()))
                .thenReturn(false)
        }

        val result = permissionRepository.hasManageExternalStoragePermission()
        assertThat(result).isFalse()
    }

    @ParameterizedTest
    @MethodSource("provideLocationPermissions")
    fun `test isLocationPermissionGranted returns correct value based on permissions`(
        fineLocationGranted: Boolean,
        coarseLocationGranted: Boolean,
        expected: Boolean,
    ) {
        whenever(permissionGateway.hasPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION))
            .thenReturn(fineLocationGranted)
        whenever(permissionGateway.hasPermissions(android.Manifest.permission.ACCESS_COARSE_LOCATION))
            .thenReturn(coarseLocationGranted)

        val result = permissionRepository.isLocationPermissionGranted()
        assertThat(result).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        private fun provideLocationPermissions(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(true, false, true),
                Arguments.of(false, true, true),
                Arguments.of(false, false, false)
            )
        }

        @JvmStatic
        private fun provideAudioPermissions(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(true, true),
                Arguments.of(false, false)
            )
        }
    }
}