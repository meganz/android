package mega.privacy.android.app.presentation.verification

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import mega.privacy.android.app.main.CountryCodePickerActivity

/**
 * Select Country Code Contract
 */
class SelectCountryCodeContract :
    ActivityResultContract<ArrayList<String>?, Triple<String?, String?, String?>?>() {
    override fun createIntent(context: Context, input: ArrayList<String>?) =
        Intent(context, CountryCodePickerActivity::class.java).apply {
            putStringArrayListExtra("country_code", input)
        }

    /**
     * returns Triple<String?,String?,String?> where first one is selectedCountryCode
     * and second one is selectedCountryName and third is selectedDialCode
     */
    override fun parseResult(resultCode: Int, intent: Intent?) =
        intent?.takeIf { resultCode == AppCompatActivity.RESULT_OK }?.let {
            val selectedCountryCode = it.getStringExtra(CountryCodePickerActivity.COUNTRY_CODE)
            val selectedCountryName = it.getStringExtra(CountryCodePickerActivity.COUNTRY_NAME)
            val selectedDialCode = it.getStringExtra(CountryCodePickerActivity.DIAL_CODE)
            Triple(
                selectedCountryCode,
                selectedCountryName,
                selectedDialCode
            )
        }
}
