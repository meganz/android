package mega.privacy.android.app.jobservices;

import java.util.ArrayList;
import java.util.List;

public class SyncRecord {

    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_FAILED = 2;

    private int id;

    private String localPath;

    private String fileName;

    private Long timestamp;

    private Long nodeHandle;
    
    private Boolean isSecondary = false;

    private Boolean copyOnly = false;
    
    private int status = STATUS_PENDING;

    public SyncRecord() {

    }

    public SyncRecord(String localPath,String fileName,Long timestamp, Boolean isSecondary) {
        this.localPath = localPath;
        this.fileName = fileName;
        this.timestamp = timestamp;
        this.isSecondary = isSecondary;
    }

    public SyncRecord(Long handle,String name,boolean copyOnly,String filePath,Long timestamp, Boolean isSecondary) {
        this.copyOnly = copyOnly;
        this.nodeHandle = handle;
        this.fileName = name;
        this.localPath = filePath;
        this.timestamp = timestamp;
        this.isSecondary = isSecondary;
    }

    public Boolean isCopyOnly() {
        return copyOnly;
    }

    public void setCopyOnly(Boolean copyOnly) {
        this.copyOnly = copyOnly;
    }
    
    public Boolean getSecondary() {
        return isSecondary;
    }
    
    public void setSecondary(Boolean secondary) {
        isSecondary = secondary;
    }
    
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public Long getNodeHandle() {
        return nodeHandle;
    }

    public void setNodeHandle(Long nodeHandle) {
        this.nodeHandle = nodeHandle;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public SyncRecord(String localPath,Long timestamp) {
        this.localPath = localPath;
        this.timestamp = timestamp;
    }

//    public static List<SyncRecord> convert(List<CameraUploadsService.PendingUpload> pending) {
//        if (pending == null || pending.size() == 0) {
//            return null;
//        }
//        List<SyncRecord> list = new ArrayList<>(pending.size());
//        for (CameraUploadsService.PendingUpload p : pending) {
//            list.add(convert(p));
//        }
//        return list;
//    }
//
//    public static SyncRecord convert(CameraUploadsService.PendingUpload pending) {
//        return new SyncRecord(pending.media.filePath,pending.media.timestamp);
//    }
}
