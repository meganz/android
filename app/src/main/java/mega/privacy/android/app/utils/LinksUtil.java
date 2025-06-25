package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.Constants.HANDLE;
import static mega.privacy.android.app.utils.Constants.HANDLE_LIST;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.getLink.GetLinkActivity;
import mega.privacy.android.app.listeners.SessionTransferURLListener;

public class LinksUtil {

    private static final String REQUIRES_TRANSFER_SESSION = "fm/";

    /**
     * Checks if the link received requires transfer session.
     *
     * @param url link to check
     * @return True if the link requires transfer session, false otherwise.
     */
    public static boolean requiresTransferSession(Context context, String url) {
        if (url.contains(REQUIRES_TRANSFER_SESSION)) {
            int start = url.indexOf(REQUIRES_TRANSFER_SESSION);
            if (start != -1) {
                String path = url.substring(start + REQUIRES_TRANSFER_SESSION.length());
                if (!isTextEmpty(path)) {
                    MegaApplication.getInstance().getMegaApi().getSessionTransferURL(path, new SessionTransferURLListener(context));
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Splits the link from its decryption key and returns the link.
     *
     * @param linkWithKey The link with the decryption key.
     * @return The link without the decryption key.
     */
    public static String getLinkWithoutKey(String linkWithKey) {
        return getLinkWithoutKeyOrOnlyKey(linkWithKey, false);
    }

    /**
     * Splits the link from its decryption key and returns the decryption key.
     *
     * @param linkWithKey The link with the decryption key
     * @return The decryption key of the link.
     */
    public static String getKeyLink(String linkWithKey) {
        return getLinkWithoutKeyOrOnlyKey(linkWithKey, true);
    }

    /**
     * Splits the link from its decryption key and returns the link or decryption key,
     * depending on the value received on onlyKey param.
     *
     * @param linkWithKey The link with the decryption key
     * @return The link without the decryption key or the decryption key of the link.
     */
    public static String getLinkWithoutKeyOrOnlyKey(String linkWithKey, boolean onlyKey) {
        if (linkWithKey.contains("#!") || linkWithKey.contains("#F!")) {
            //old file or folder link format
            String[] s = linkWithKey.split("!");

            if (s.length == 3) {
                return onlyKey ? s[2] : s[0] + "!" + s[1];
            }
        } else {
            // new file or folder link format
            String[] s = linkWithKey.split("#");

            if (s.length == 2) {
                return onlyKey ? s[1] : s[0];
            }
        }

        return null;
    }

    /**
     * Launches an intent to show get link activity.
     *
     * @param activity Activity which launches the intent.
     * @param handle   identifier of the node to get or manage its link.
     */
    public static void showGetLinkActivity(Activity activity, long handle) {
        activity.startActivity(new Intent(activity, GetLinkActivity.class)
                .putExtra(HANDLE, handle));
    }

    /**
     * Launches an intent to show get link activity and get several links.
     *
     * @param activity Activity which launches the intent.
     * @param handles  List of handles to get their link.
     */
    public static void showGetLinkActivity(Activity activity, long[] handles) {
        activity.startActivity(new Intent(activity, GetLinkActivity.class)
                .putExtra(HANDLE_LIST, handles));
    }

    /**
     * Launches an intent to show get link activity.
     *
     * @param fragment Fragment which launches the intent.
     * @param handle   identifier of the node to get or manage its link.
     */
    public static void showGetLinkActivity(Fragment fragment, long handle) {
        fragment.startActivity(new Intent(fragment.getContext(), GetLinkActivity.class)
                .putExtra(HANDLE, handle));
    }

    /**
     * Launches an intent to show get link activity and get several links.
     *
     * @param fragment Fragment which launches the intent.
     * @param handles  List of handles to get their link.
     */
    public static void showGetLinkActivity(Fragment fragment, long[] handles) {
        fragment.startActivity(new Intent(fragment.getContext(), GetLinkActivity.class)
                .putExtra(HANDLE_LIST, handles));
    }
}
