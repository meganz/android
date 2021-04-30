package mega.privacy.android.app.utils.permission;

/**
 * Interface used by onShowRationale methods to allow for continuation
 * or cancellation of a permission request.
 */
public interface PermissionRequest {
    void proceed();

    void cancel();
}
