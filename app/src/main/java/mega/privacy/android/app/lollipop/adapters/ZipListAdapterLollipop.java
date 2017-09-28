package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
import java.util.zip.ZipEntry;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop;
import mega.privacy.android.app.utils.Util;


public class ZipListAdapterLollipop  extends RecyclerView.Adapter implements View.OnClickListener, View.OnLongClickListener {

	Context context;
	int positionClicked;
//	ListView listFragment;
RecyclerView listFragment;

	//	ActionBar aB;
	List<ZipEntry> zipNodeList;
	String currentFolder;

	@Override
	public void onClick(View v) {

	}

	@Override
	public boolean onLongClick(View v) {
		return false;
	}

	/* public static view holder class */
	public class ViewHolderBrowserList {
//		CheckBox checkbox;
		ImageView imageView;
		TextView textViewFileName;
		TextView textViewFileSize;
		ImageButton imageButtonThreeDots;
		RelativeLayout itemLayout;

		public ImageView publicLinkImage;
		int currentPosition;

		public ImageView savedOffline;
		long document;
	}
	
	//public ZipListAdapterLollipop(ZipBrowserActivityLollipop _context, ListView _listView, ActionBar _aB, List<ZipEntry> _zipNodes, String _currentFolder) {
	public ZipListAdapterLollipop(ZipBrowserActivityLollipop _context, RecyclerView _listView, ActionBar _aB, List<ZipEntry> _zipNodes, String _currentFolder) {

		this.context = _context;
		this.listFragment = _listView;
		this.zipNodeList = _zipNodes;		
//		this.aB = aB;
		this.positionClicked = -1;		
		//Set the name of the folder
		this.currentFolder = _currentFolder;
	}


//	@Override
//	public View getView(int position, View convertView, ViewGroup parent) {
//
//ViewHolderBrowserList holder = null;
//
//	Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
//	DisplayMetrics outMetrics = new DisplayMetrics();
//		display.getMetrics(outMetrics);
//	float density = ((Activity) context).getResources().getDisplayMetrics().density;
//	float scaleW = Util.getScaleW(outMetrics, density);
//	float scaleH = Util.getScaleH(outMetrics, density);
//
//	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		if (convertView == null) {
//		convertView = inflater.inflate(R.layout.item_file_list, parent,false);
//		holder = new ViewHolderBrowserList();
//		holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.file_list_item_layout);
//
//		holder.imageView = (ImageView) convertView.findViewById(R.id.file_list_thumbnail);
//		holder.textViewFileName = (TextView) convertView.findViewById(R.id.file_list_filename);
//		holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
//		holder.textViewFileName.getLayoutParams().width = Util.px2dp((225 * scaleW), outMetrics);
//		holder.textViewFileSize = (TextView) convertView.findViewById(R.id.file_list_filesize);
//		//holder.arrowSelection = (ImageView) convertView.findViewById(R.id.file_list_arrow_selection);
//		holder.publicLinkImage = (ImageView) convertView.findViewById(R.id.file_list_public_link);
//		holder.savedOffline = (ImageView) convertView.findViewById(R.id.file_list_saved_offline);
//		holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.file_list_three_dots);
//
//		convertView.setTag(holder);
//	} else {
//		holder = (ViewHolderBrowserList) convertView.getTag();
//	}
//
//		holder.savedOffline.setVisibility(View.INVISIBLE);
//
//		holder.publicLinkImage.setVisibility(View.INVISIBLE);
//		holder.imageButtonThreeDots.setVisibility(View.GONE);
//
//	ZipEntry zipNode = (ZipEntry) getItem(position);
//
//	String nameFile = zipNode.getName();
//
//		if(zipNode.isDirectory()){
//
//		int index = nameFile.lastIndexOf("/");
//
//		nameFile=nameFile.substring(0, nameFile.length()-1);
//		index = nameFile.lastIndexOf("/");
//		nameFile = nameFile.substring(index+1, nameFile.length());
//
//		String info = ((ZipBrowserActivityLollipop)context).countFiles(nameFile);
//
//		holder.textViewFileSize.setText(info);
//		holder.imageView.setImageResource(R.drawable.ic_folder_list);
//
//	}
//		else{
//		int	index = nameFile.lastIndexOf("/");
//		nameFile = nameFile.substring(index+1, nameFile.length());
//
//		holder.textViewFileSize.setText(Util.getSizeString(zipNode.getSize()));
//		holder.imageView.setImageResource(MimeTypeList.typeForName(zipNode.getName()).getIconResourceId());
//	}
//
//		holder.textViewFileName.setText(nameFile);
////		holder.textViewFileSize.setText(""+zipNode.getSize());
//
//		if (positionClicked == -1){
//		holder.itemLayout.setBackgroundColor(Color.WHITE);
//	}
//
//		return convertView;
//	}


	public Object getItem(int position) {

		return zipNodeList.get(position);
	}
	
	public void setNodes (List<ZipEntry> _nodes){
		this.zipNodeList=_nodes;
		notifyDataSetChanged();
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//		log("onCreateViewHolder -> type: ITEM_VIEW_TYPE_LIST");
//
//		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_list, parent, false);
//
//		ViewHolderBrowserList holderList = new ViewHolderBrowserList(v);
//		holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.file_list_item_layout);
//		holderList.imageView = (ImageView) v.findViewById(R.id.file_list_thumbnail);
//		holderList.savedOffline = (ImageView) v.findViewById(R.id.file_list_saved_offline);
//
//		holderList.publicLinkImage = (ImageView) v.findViewById(R.id.file_list_public_link);
//		holderList.permissionsIcon = (ImageView) v.findViewById(R.id.file_list_incoming_permissions);
//
//		holderList.textViewFileName = (TextView) v.findViewById(R.id.file_list_filename);
//
//		holderList.textViewFileSize = (TextView) v.findViewById(R.id.file_list_filesize);
//
//		holderList.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.file_list_three_dots);
//
//		holderList.savedOffline.setVisibility(View.INVISIBLE);
//
//		holderList.publicLinkImage.setVisibility(View.INVISIBLE);
//
//		holderList.textViewFileSize.setVisibility(View.VISIBLE);
//
//		holderList.itemLayout.setTag(holderList);
//		holderList.itemLayout.setOnClickListener(this);
//		holderList.itemLayout.setOnLongClickListener(this);
//
//		holderList.imageButtonThreeDots.setTag(holderList);
//		holderList.imageButtonThreeDots.setOnClickListener(this);
//
//		v.setTag(holderList);
//
//		return holderList;
		return null;
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemCount() {
		if (zipNodeList != null){
			return zipNodeList.size();
		}
		else{
			return 0;
		}
	}

	private static void log(String log) {
		Util.log("ZipListAdapter", log);
	}
	
	public void setFolder(String folder){
		log("setFolder: "+folder);
		this.currentFolder=folder;
	}

}
