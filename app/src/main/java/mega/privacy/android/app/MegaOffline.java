package mega.privacy.android.app;

import mega.privacy.android.app.utils.Util;

public class MegaOffline {
	
	public static String FOLDER = "1";
	public static String FILE = "0";

	public static int INCOMING = 1;
	public static int INBOX = 2;
	public static int OTHER = 0;
	
	int id = -1;	
	String handle = "";
	String path = "";
	String name = "";
	int parentId = -1;
	String type = "";	
	int origin = OTHER;
	String handleIncoming = "";
	
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

	public int getparentId() {
		return parentId;
	}

	public void setparentId(int parentId) {
		this.parentId = parentId;
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
			log("isFolder type is NULL");
		}
		return false;
	}

	public int getOrigin() {
		return origin;
	}

	public void setOrigin(int origin) {
		this.origin = origin;
	}

	private static void log(String log) {
		Util.log("MegaOffline", log);
	}

}