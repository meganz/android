package mega.privacy.android.domain.entity.camerauploads

/**
 * Camera Uploads Finished Reason
 */
enum class CameraUploadsFinishedReason {
    /**
     * Camera Uploads completed successfully.
     */
    COMPLETED,

    /**
     * Camera Uploads disabled.
     */
    DISABLED,

    /**
     * Login to server failed.
     */
    LOGIN_FAILED,

    /**
     * The local primary folder is not valid.
     */
    LOCAL_PRIMARY_FOLDER_NOT_VALID,

    /**
     * The media permission is not granted.
     */
    MEDIA_PERMISSION_NOT_GRANTED,

    /**
     * The battery level is too low.
     */
    BATTERY_LEVEL_TOO_LOW,

    /**
     * Camera Uploads is configured to upload only on wifi and the device is not on wifi
     * or the device has no connection.
     */
    NETWORK_CONNECTION_REQUIREMENT_NOT_MET,

    /**
     * The target nodes has been deleted.
     */
    TARGET_NODES_DELETED,

    /**
     * The account available storage space is over quota.
     */
    ACCOUNT_STORAGE_OVER_QUOTA,

    /**
     * The local storage space is insufficient.
     */
    INSUFFICIENT_LOCAL_STORAGE_SPACE,

    /**
     * An exception occurred during transfer.
     */
    ERROR_DURING_PROCESS,

    /**
     * Unknown reason.
     */
    UNKNOWN,

    /**
     * The worker has been cancelled by the app.
     * This error is directly coming from the WorkManager system.
     *
     * @see {WorkInfo.STOP_REASON_CANCELLED_BY_APP}
     */
    SYSTEM_REASON_CANCELLED_BY_APP,

    /**
     * The worker has been preempted to run a higher priority job of the app.
     * This error is directly coming from the WorkManager system.
     *
     * @see {WorkInfo.STOP_REASON_PREEMPT}
     */
    SYSTEM_REASON_PREEMPT,

    /**
     * The worker has timed out.
     * This error is directly coming from the WorkManager system.
     *
     * @see {WorkInfo.STOP_REASON_TIMEOUT}
     */
    SYSTEM_REASON_TIMEOUT,

    /**
     * The device state (eg. Doze, battery saver, memory usage, etc) requires WorkManager to stop this worker.
     * This error is directly coming from the WorkManager system.
     *
     * @see {WorkInfo.STOP_REASON_DEVICE_STATE}
     */
    SYSTEM_REASON_DEVICE_STATE,

    /**
     * The requested battery-not-low constraint is no longer satisfied.
     * This error is directly coming from the WorkManager system.
     *
     * @see {WorkInfo.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW}
     */
    SYSTEM_REASON_CONSTRAINT_BATTERY_NOT_LOW,

    /**
     * The requested charging constraint is no longer satisfied.
     * This error is directly coming from the WorkManager system.
     *
     * @see {WorkInfo.STOP_REASON_CONSTRAINT_CHARGING}
     */
    SYSTEM_REASON_CONSTRAINT_CHARGING,

    /**
     * The requested connectivity constraint is no longer satisfied.
     * This error is directly coming from the WorkManager system.
     *
     * @see {WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY}
     */
    SYSTEM_REASON_CONSTRAINT_CONNECTIVITY,

    /**
     * The requested idle constraint is no longer satisfied.
     * This error is directly coming from the WorkManager system.
     *
     * @see {WorkInfo.STOP_REASON_CONSTRAINT_DEVICE_IDLE}
     */
    SYSTEM_REASON_CONSTRAINT_DEVICE_IDLE,

    /**
     * The requested storage-not-low constraint is no longer satisfied.
     * This error is directly coming from the WorkManager system.
     *
     * @see {WorkInfo.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW}
     */
    SYSTEM_REASON_CONSTRAINT_STORAGE_NOT_LOW,

    /**
     * The app has consumed all of its current quota. Each app is assigned a quota of how much
     * it can run workers within a certain time frame.
     * The quota is informed, in part, by app standby buckets.
     * This error is directly coming from the WorkManager system.
     *
     * @see {WorkInfo.STOP_REASON_QUOTA}

     */
    SYSTEM_REASON_QUOTA,

    /**
     * The app is restricted from running in the background.
     * This error is directly coming from the WorkManager system.
     *
     * @see {STOP_REASON_BACKGROUND_RESTRICTION}
     */
    SYSTEM_REASON_BACKGROUND_RESTRICTION,

    /**
     * The current standby bucket requires that the job stop now.
     * This error is directly coming from the WorkManager system.
     *
     * @see {STOP_REASON_APP_STANDBY}
     */
    SYSTEM_REASON_APP_STANDBY,

    /**
     * The user stopped the job. This can happen either through force-stop, adb shell commands,
     * uninstalling, or some other UI.
     * This error is directly coming from the WorkManager system.
     *
     * @see {STOP_REASON_USER}
     */
    SYSTEM_REASON_USER,

    /**
     * The system is doing some processing that requires stopping this job.
     * This error is directly coming from the WorkManager system.
     *
     * @see {STOP_REASON_SYSTEM_PROCESSING}
     */
    SYSTEM_REASON_SYSTEM_PROCESSING,

    /**
     * The system's estimate of when the app will be launched changed significantly enough to
     * decide this worker shouldn't be running right now.
     * This error is directly coming from the WorkManager system.
     *
     * @see {STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED}
     */
    SYSTEM_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED;
}


