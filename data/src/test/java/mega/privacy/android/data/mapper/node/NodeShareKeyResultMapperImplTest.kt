package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.shares.AccessPermissionIntMapper
import mega.privacy.android.domain.entity.shares.AccessPermission
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class NodeShareKeyResultMapperImplTest {

    private val megaApiGateway = mock<MegaApiGateway>()
    private val accessPermissionIntMapper = mock<AccessPermissionIntMapper>()

    private val underTest: NodeShareKeyResultMapper = NodeShareKeyResultMapperImpl(
        UnconfinedTestDispatcher(),
        megaApiGateway,
        accessPermissionIntMapper
    )

    @ParameterizedTest(name = "Permission: {0}")
    @EnumSource(AccessPermission::class)
    fun `test when mapper result is invoked then api gateway setShareAccess is called with the proper email and permission`(
        permission: AccessPermission,
    ) =
        runTest {
            val megaNode = mock<MegaNode>()
            val email = "example@example.com"
            whenever(megaApiGateway.setShareAccess(any(), any(), any(), any())).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(),
                    request = mock(),
                    error = mock {
                        on { errorCode }.thenReturn(
                            MegaError.API_OK
                        )
                    },
                )
            }
            whenever(accessPermissionIntMapper.invoke(any())).thenReturn(permission.ordinal)

            val result = underTest.invoke(megaNode)
            result(permission, email)
            verify(megaApiGateway, times(1)).setShareAccess(
                eq(megaNode),
                eq(email),
                eq(permission.ordinal),
                any()
            )
        }
}