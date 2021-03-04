package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
import java.util.zip.ZipEntry;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;


public class ZipListAdapterLollipop  extends RecyclerView.Adapter<ZipListAdapterLollipop.ViewHolderBrowserList> implements View.OnClickListener {

	Context context;
	int positionClicked;
	RecyclerView listFragment;
	List<ZipEntry> zipNodeList;
	String currentFolder;

	// public static view holder class
	public class ViewHolderBrowserList extends RecyclerView.ViewHolder{
//		CheckBox checkbox;
		ImageView imageView;
		TextView textViewFileName;
		EmojiTextView textViewFileSize;
		RelativeLayout threeDotsLayout;

		RelativeLayout itemLayout;

		public ImageView publicLinkImage;
		int currentPosition;

		public ImageView savedOffline;
		long document;

		boolean isUnknownFile;

		public ViewHolderBrowserList(View itemView) {
			super(itemView);
		}
	}
	
	public ZipListAdapterLollipop(ZipBrowserActivityLollipop _context, RecyclerView _listView, ActionBar _aB, List<ZipEntry> _zipNodes, String _currentFolder) {

		this.context = _context;
		this.listFragment = _listView;
		this.zipNodeList = _zipNodes;		
		this.positionClicked = -1;
		this.currentFolder = _currentFolder;
	}

	public Object getItem(int position) {
		return zipNodeList.get(position);
	}
	
	public void setNodes (List<ZipEntry> _nodes){
		this.zipNodeList=_nodes;
		notifyDataSetChanged();
	}

	@Override public ViewHolderBrowserList onCreateViewHolder(ViewGroup parent, int viewType) {

		listFragment = (RecyclerView) parent;

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;
		float scaleW = getScaleW(outMetrics, density);
		float scaleH = getScaleH(outMetrics, density);

		View convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_list, parent, false);
		ViewHolderBrowserList holder = new ViewHolderBrowserList(convertView);

		holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.file_list_item_layout);
		holder.itemLayout.setOnClickListener(this);
		holder.imageView = (ImageView) convertView.findViewById(R.id.file_list_thumbnail);
		holder.textViewFileName = (TextView) convertView.findViewById(R.id.file_list_filename);
		holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		holder.textViewFileName.getLayoutParams().width = dp2px((225 * scaleW), outMetrics);
		holder.textViewFileSize = convertView.findViewById(R.id.file_list_filesize);
		holder.publicLinkImage = (ImageView) convertView.findViewById(R.id.file_list_public_link);
		holder.savedOffline = (ImageView) convertView.findViewById(R.id.file_list_saved_offline);
		holder.threeDotsLayout = (RelativeLayout) convertView.findViewById(R.id.file_list_three_dots_layout);

		convertView.setTag(holder);
		return holder;
	}

	@Override public void onBindViewHolder(ViewHolderBrowserList holder, int position) {

		holder.savedOffline.setVisibility(View.INVISIBLE);
		holder.publicLinkImage.setVisibility(View.INVISIBLE);
		holder.threeDotsLayout.setVisibility(View.INVISIBLE);

		ZipEntry zipNode = (ZipEntry) getItem(position);

		String nameFile = zipNode.getName();

		if (nameFile.equals(context.getString(R.string.transfer_unknown))) {
			holder.isUnknownFile = true;
			holder.textViewFileName.setText(nameFile);
			holder.textViewFileSize.setText("");
			holder.imageView.setImageResource(MimeTypeList.typeForName(zipNode.getName()).getIconResourceId());
		}
		else {
			holder.isUnknownFile = false;
			if(zipNode.isDirectory()){
				nameFile=nameFile.substring(0, nameFile.length()-1);
                int index = nameFile.lastIndexOf("/");
				nameFile = nameFile.substring(index+1, nameFile.length());

				String info = ((ZipBrowserActivityLollipop)context).countFiles(nameFile);

				holder.textViewFileSize.setText(info);
				holder.textViewFileName.setText(nameFile);

				holder.imageView.setImageResource(R.drawable.ic_folder_list);

			}
			else{
                int index = nameFile.lastIndexOf("/");
				nameFile = nameFile.substring(index+1, nameFile.length());

				holder.textViewFileSize.setText(getSizeString(zipNode.getSize()));
				holder.textViewFileName.setText(nameFile);

				holder.imageView.setImageResource(MimeTypeList.typeForName(zipNode.getName()).getIconResourceId());
			}
		}
	}

	@Override public long getItemId(int position) {
		return position;
	}

	@Override public int getItemCount() {
		if (zipNodeList != null){
			return zipNodeList.size();
		}
		else{
			return 0;
		}
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");

		ViewHolderBrowserList holder = (ViewHolderBrowserList) v.getTag();
		if (holder.isUnknownFile) {
			((ZipBrowserActivityLollipop) context).showSnackbar(context.getString(R.string.unknownn_file));
		}
		else {
			int currentPosition = holder.getAdapterPosition();
			int[] screenPosition = new int[2];
			ImageView imageView;
			imageView = (ImageView) v.findViewById(R.id.file_list_thumbnail);
			imageView.getLocationOnScreen(screenPosition);
			int[] dimens = new int[4];
			dimens[0] = screenPosition[0];
			dimens[1] = screenPosition[1];
			dimens[2] = imageView.getWidth();
			dimens[3] = imageView.getHeight();
			((ZipBrowserActivityLollipop) context).itemClick(currentPosition, dimens, imageView);
		}
	}
	
	public void setFolder(String folder){
		logDebug("folder: " + folder);
		this.currentFolder=folder;
	}
}
