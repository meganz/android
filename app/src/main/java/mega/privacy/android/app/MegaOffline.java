package mega.privacy.android.app;

import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;

public class MegaOffline {
	
	public static final String FOLDER = "1";
	public static final String FILE = "0";

	public static final int INCOMING = 1;
	public static final int INBOX = 2;
	public static final int OTHER = 0;
	
	private int id = -1;
	private String handle = "";
	private String path = "";
	private String name = "";
	private int parentId = -1;
	private String type = "";
	private int origin = OTHER;
	private String handleIncoming = "";
	
	public MegaOffline(String handle, String path, String name, int parentId, String type, int origin, String handleIncoming) {
		this.handle = handle;
		this.path = path;
		this.name = name;
		this.parentId = parentId;
		this.type = type;
		this.origin = origin;
		this.handleIncoming = handleIncoming;
	}
	
	public MegaOffline(int id, String handle, String path, String name, int parentId, String type, int origin, String handleIncoming) {
		this.id=id;
		this.handle = handle;
		this.path = path;
		this.name = name;
		this.parentId = parentId;
		this.type = type;
		this.origin = origin;
		this.handleIncoming = handleIncoming;
	}
	
	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getParentId() {
		return parentId;
	}

	public void setParentId(int parentId) {
		this.parentId = parentId;
	}

	public String getHandleIncoming() {
		return handleIncoming;
	}

	public void setHandleIncoming(String handleIncoming) {
		this.handleIncoming = handleIncoming;
	}
	
	public boolean isFolder(){
		if (type != null){
			if(type.equals(FOLDER)){
				return true;
			}
		}
		else{
			LogUtil.logDebug("isFolder type is NULL");
		}
		return false;
	}

	public int getOrigin() {
		return origin;
	}

	public void setOrigin(int origin) {
		this.origin = origin;
	}
}