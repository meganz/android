package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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
import mega.privacy.android.app.utils.Util;


public class ZipListAdapterLollipop  extends BaseAdapter implements OnClickListener {
	
	Context context;
	int positionClicked;
	ListView listFragment;	
//	ActionBar aB;
	List<ZipEntry> zipNodeList;
	String currentFolder;
	
	/* public static view holder class */
	public class ViewHolderBrowserList {
//		CheckBox checkbox;
		ImageView imageView;
		TextView textViewFileName;
		TextView textViewFileSize;
		ImageButton imageButtonThreeDots;
		RelativeLayout itemLayout;

		public ImageView publicLinkImage;
		//ImageView arrowSelection;
//		LinearLayout optionsLayout;
		//ImageView optionDownload;
		//ImageView optionProperties;
        ProgressBar transferProgressBar;
		//ImageView optionRename;
		//ImageView optionCopy;
		//ImageView optionMove;
		//ImageView optionPublicLink;
		//ImageView optionDelete;
		int currentPosition;

		public ImageView savedOffline;
		long document;
	}
	
	public ZipListAdapterLollipop(ZipBrowserActivityLollipop _context, ListView _listView, ActionBar _aB, List<ZipEntry> _zipNodes, String _currentFolder) {
		
		this.context = _context;				
		this.listFragment = _listView;
		this.zipNodeList = _zipNodes;		
//		this.aB = aB;
		this.positionClicked = -1;		
		//Set the name of the folder
		this.currentFolder = _currentFolder;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		log("onCreate");

		ViewHolderBrowserList holder = null;

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;
		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_file_list, parent,false);
			holder = new ViewHolderBrowserList();
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.file_list_item_layout);

			holder.imageView = (ImageView) convertView.findViewById(R.id.file_list_thumbnail);
			holder.textViewFileName = (TextView) convertView.findViewById(R.id.file_list_filename);			
			holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName.getLayoutParams().width = Util.px2dp((225 * scaleW), outMetrics);
			holder.textViewFileSize = (TextView) convertView.findViewById(R.id.file_list_filesize);
			holder.transferProgressBar = (ProgressBar) convertView.findViewById(R.id.transfers_list_browser_bar);
			//holder.arrowSelection = (ImageView) convertView.findViewById(R.id.file_list_arrow_selection);
			holder.publicLinkImage = (ImageView) convertView.findViewById(R.id.file_list_public_link);
			holder.savedOffline = (ImageView) convertView.findViewById(R.id.file_list_saved_offline);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.file_list_three_dots);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolderBrowserList) convertView.getTag();
		}

		holder.savedOffline.setVisibility(View.INVISIBLE);
		holder.transferProgressBar.setVisibility(View.GONE);
		holder.publicLinkImage.setVisibility(View.INVISIBLE);
		holder.imageButtonThreeDots.setVisibility(View.GONE);

		ZipEntry zipNode = (ZipEntry) getItem(position);		
		
		String nameFile = zipNode.getName();
		
		if(zipNode.isDirectory()){			
			
			int index = nameFile.lastIndexOf("/");
			
			nameFile=nameFile.substring(0, nameFile.length()-1);
			index = nameFile.lastIndexOf("/");			
			nameFile = nameFile.substring(index+1, nameFile.length());
			
			String info = ((ZipBrowserActivityLollipop)context).countFiles(nameFile);
			
			holder.textViewFileSize.setText(info);
			holder.imageView.setImageResource(R.drawable.ic_folder_list);
			
		}
		else{
			int	index = nameFile.lastIndexOf("/");			
			nameFile = nameFile.substring(index+1, nameFile.length());
			
			holder.textViewFileSize.setText(Util.getSizeString(zipNode.getSize()));	
			holder.imageView.setImageResource(MimeTypeList.typeForName(zipNode.getName()).getIconResourceId());
		}	
						
		holder.textViewFileName.setText(nameFile);
//		holder.textViewFileSize.setText(""+zipNode.getSize());		
				
		if (positionClicked == -1){
			//holder.arrowSelection.setVisibility(View.GONE);
//			LinearLayout.LayoutParams params = holder.optionsLayout.getLayoutParams();
//			params.height = 0;
			holder.itemLayout.setBackgroundColor(Color.WHITE);
			holder.transferProgressBar.setVisibility(View.GONE);
			//holder.arrowSelection.setVisibility(View.GONE);
		}

		return convertView;
	}
	

	@Override
	public int getCount() {
		// TODO Auto-generated method stub		
		return zipNodeList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		
		return zipNodeList.get(position);
	}
	
	public void setNodes (List<ZipEntry> _nodes){
		this.zipNodeList=_nodes;
		notifyDataSetChanged();
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}


	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}
	
	private static void log(String log) {
		Util.log("ZipListAdapter", log);
	}
	
	public void setFolder(String folder){
		log("setFolder: "+folder);
		this.currentFolder=folder;
	}
}
