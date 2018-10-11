package mega.privacy.android.app.jobservices;

import java.util.ArrayList;
import java.util.List;

public class SyncRecord {

    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_FAILED = 2;

    private int id;

    private String localPath;

    private long timestamp;

    private int status = STATUS_PENDING;

    public SyncRecord() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public SyncRecord(String localPath,long timestamp) {
        this.localPath = localPath;
        this.timestamp = timestamp;
    }

    public static List<SyncRecord> convert(List<CameraUploadsService.PendingUpload> pending) {
        if (pending == null || pending.size() == 0) {
            return null;
        }
        List<SyncRecord> list = new ArrayList<>(pending.size());
        for (CameraUploadsService.PendingUpload p : pending) {
            list.add(convert(p));
        }
        return list;
    }

    public static SyncRecord convert(CameraUploadsService.PendingUpload pending) {
        return new SyncRecord(pending.media.filePath,pending.media.timestamp);
    }

    @Override
    public String toString() {
        return "SyncRecord{" +
                "id=" + id +
                ", localPath='" + localPath + '\'' +
                ", timestamp=" + timestamp +
                ", status=" + status +
                '}';
    }
}
