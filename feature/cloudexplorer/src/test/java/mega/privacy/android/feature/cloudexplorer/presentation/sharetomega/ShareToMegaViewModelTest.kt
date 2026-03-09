package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareToMegaViewModelTest {

    private lateinit var viewModel: ShareToMegaViewModel

    private val sharedUris = mock<List<UriPath>>()

    @BeforeEach
    fun setUp() {
        viewModel = ShareToMegaViewModel(
            args = ShareToMegaViewModel.Args(sharedUris)
        )
    }

    @Test
    fun `test that initial state is returned`() {
        assertThat(viewModel.args.shareUris).isEqualTo(sharedUris)
    }
}
