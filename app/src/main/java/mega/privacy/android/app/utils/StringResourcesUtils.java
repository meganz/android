package mega.privacy.android.app.utils;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaError;

import static nz.mega.sdk.MegaChatError.*;
import static nz.mega.sdk.MegaError.*;

public class StringResourcesUtils {

    /**
     * Gets the translated string of an error received in a request.
     *
     * @param error MegaError received in the request
     * @return The translated string
     */
    public static String getTranslatedErrorString(MegaError error) {
        MegaApplication app = MegaApplication.getInstance();
        if (app == null) {
            return error.getErrorString();
        }

        if (error.getErrorCode() > 0) {
            return app.getString(R.string.api_error_http);
        }

        switch (error.getErrorCode()) {
            case API_OK:
                return app.getString(R.string.api_ok);
            case API_EINTERNAL:
                return app.getString(R.string.api_einternal);
            case API_EARGS:
                return app.getString(R.string.api_eargs);
            case API_EAGAIN:
                return app.getString(R.string.api_eagain);
            case API_ERATELIMIT:
                return app.getString(R.string.api_eratelimit);
            case API_EFAILED:
                return app.getString(R.string.api_efailed);
            case API_ETOOMANY:
                if (error.getErrorString().equals("Terms of Service breached")) {
                    return app.getString(R.string.api_etoomany_ec_download);
                } else if (error.getErrorString().equals("Too many concurrent connections or transfers")){
                    return app.getString(R.string.api_etoomay);
                } else {
                    return error.getErrorString();
                }
            case API_ERANGE:
                return app.getString(R.string.api_erange);
            case API_EEXPIRED:
                return app.getString(R.string.api_eexpired);
            case API_ENOENT:
                return app.getString(R.string.api_enoent);
            case API_ECIRCULAR:
                if (error.getErrorString().equals("Upload produces recursivity")) {
                    return app.getString(R.string.api_ecircular_ec_upload);
                } else if (error.getErrorString().equals("Circular linkage detected")){
                    return app.getString(R.string.api_ecircular);
                } else {
                    return error.getErrorString();
                }
            case API_EACCESS:
                return app.getString(R.string.api_eaccess);
            case API_EEXIST:
                return app.getString(R.string.api_eexist);
            case API_EINCOMPLETE:
                return app.getString(R.string.api_eincomplete);
            case API_EKEY:
                return app.getString(R.string.api_ekey);
            case API_ESID:
                return app.getString(R.string.api_esid);
            case API_EBLOCKED:
                if (error.getErrorString().equals("Not accessible due to ToS/AUP violation")) {
                    return app.getString(R.string.api_eblocked_ec_import_ec_download);
                } else if (error.getErrorString().equals("Blocked")) {
                    return app.getString(R.string.api_eblocked);
                } else {
                    return error.getErrorString();
                }
            case API_EOVERQUOTA:
                return app.getString(R.string.api_eoverquota);
            case API_ETEMPUNAVAIL:
                return app.getString(R.string.api_etempunavail);
            case API_ETOOMANYCONNECTIONS:
                return app.getString(R.string.api_etoomanyconnections);
            case API_EWRITE:
                return app.getString(R.string.api_ewrite);
            case API_EREAD:
                return app.getString(R.string.api_eread);
            case API_EAPPKEY:
                return app.getString(R.string.api_eappkey);
            case API_ESSL:
                return app.getString(R.string.api_essl);
            case API_EGOINGOVERQUOTA:
                return app.getString(R.string.api_egoingoverquota);
            case API_EMFAREQUIRED:
                return app.getString(R.string.api_emfarequired);
            case API_EMASTERONLY:
                return app.getString(R.string.api_emasteronly);
            case API_EBUSINESSPASTDUE:
                return app.getString(R.string.api_ebusinesspastdue);
            case PAYMENT_ECARD:
                return app.getString(R.string.payment_ecard);
            case PAYMENT_EBILLING:
                return app.getString(R.string.payment_ebilling);
            case PAYMENT_EFRAUD:
                return app.getString(R.string.payment_efraud);
            case PAYMENT_ETOOMANY:
                return app.getString(R.string.payment_etoomay);
            case PAYMENT_EBALANCE:
                return app.getString(R.string.payment_ebalance);
            case PAYMENT_EGENERIC:
            default:
                return app.getString(R.string.payment_egeneric_api_error_unknown);
        }
    }

    /**
     * Gets the translated string of an error received in a request.
     *
     * @param error MegaChatError received in the request
     * @return The translated string
     */
    public static String getTranslatedErrorString(MegaChatError error) {
        MegaApplication app = MegaApplication.getInstance();
        if (app == null) {
            return error.getErrorString();
        }

        switch (error.getErrorCode()) {
            case ERROR_OK:
                return app.getString(R.string.error_ok);
            case ERROR_ARGS:
                return app.getString(R.string.error_args);
            case ERROR_ACCESS:
                return app.getString(R.string.error_access);
            case ERROR_NOENT:
                return app.getString(R.string.error_noent);
            case ERROR_EXIST:
                return app.getString(R.string.error_exist);
            case ERROR_TOOMANY:
                return app.getString(R.string.error_toomany);
            case ERROR_UNKNOWN:
            default:
                return app.getString(R.string.error_unknown);
        }
    }
}
