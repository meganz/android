package nz.mega.android;

public class MegaOffline {
	
	int id = -1;	
	String handle = "";
	String path = "";
	String name = "";
	int parentId = -1;
	String type = "";	
	
	public MegaOffline(String handle, String path, String name, int parentId, String type) {
		this.handle = handle;
		this.path = path;
		this.name = name;
		this.parentId = parentId;
		this.type = type;
	}
	
	public MegaOffline(int id, String handle, String path, String name, int parentId, String type) {
		this.id=id;
		this.handle = handle;
		this.path = path;
		this.name = name;
		this.parentId = parentId;
		this.type = type;
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


}