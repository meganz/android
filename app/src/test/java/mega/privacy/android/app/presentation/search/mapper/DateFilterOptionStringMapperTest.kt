package mega.privacy.android.app.presentation.search.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.search.DateFilterOption
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DateFilterOptionStringMapperTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dateFilterOptionStringMapper = DateFilterOptionStringMapper(context)

    @Test
    fun `test that map date filter option to string`() {
        assertThat(dateFilterOptionStringMapper(DateFilterOption.Today))
            .isEqualTo(context.getString(R.string.search_dropdown_chip_filter_type_date_today))
        assertThat(dateFilterOptionStringMapper(DateFilterOption.Last7Days))
            .isEqualTo(context.getString(R.string.search_dropdown_chip_filter_type_date_last_seven_days))
        assertThat(dateFilterOptionStringMapper(DateFilterOption.Last30Days))
            .isEqualTo(context.getString(R.string.search_dropdown_chip_filter_type_date_last_thirty_days))
        assertThat(dateFilterOptionStringMapper(DateFilterOption.ThisYear))
            .isEqualTo(context.getString(R.string.search_dropdown_chip_filter_type_date_this_year))
        assertThat(dateFilterOptionStringMapper(DateFilterOption.LastYear))
            .isEqualTo(context.getString(R.string.search_dropdown_chip_filter_type_date_last_year))
        assertThat(dateFilterOptionStringMapper(DateFilterOption.Older))
            .isEqualTo(context.getString(R.string.search_dropdown_chip_filter_type_date_older))
    }
}