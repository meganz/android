package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.MapAddress;
import mega.privacy.android.app.lollipop.megachat.MapsActivity;
import mega.privacy.android.app.utils.Util;

public class MapsAdapter extends RecyclerView.Adapter<MapsAdapter.ViewHolderMap> implements View.OnClickListener {

    Context context;
    ArrayList<MapAddress> addresses;

    ViewHolderMap holder;

    public MapsAdapter (Context context, ArrayList<MapAddress> addresses) {
        this.context = context;
        setAddresses(addresses);
    }

    @Override
    public ViewHolderMap onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onCreateViewHolder");

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_map_address, parent, false);
        holder = new ViewHolderMap(v);

        holder.itemLayout = (RelativeLayout) v.findViewById(R.id.item_layout);
        holder.icon = (ImageView) v.findViewById(R.id.address_icon);
        holder.name = (TextView) v.findViewById(R.id.address_name_label);
        holder.address = (TextView) v.findViewById(R.id.address_label);
        holder.nearbyPlacesHeader = (RelativeLayout) v.findViewById(R.id.nearby_places_header_layout);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderMap holder, int position) {
        log("onBindViewHolder");
        MapAddress address = (MapAddress) getItem(position);

        if (position == 0) {
            holder.icon.setImageResource(R.drawable.ic_location);
            holder.nearbyPlacesHeader.setVisibility(View.VISIBLE);
        }
        else {
            holder.icon.setImageResource(R.drawable.ic_outline_location);
            holder.nearbyPlacesHeader.setVisibility(View.GONE);
        }

        holder.name.setText(address.getName());
        holder.address.setText(address.getAddress());
        holder.itemLayout.setOnClickListener(this);
        holder.itemLayout.setTag(holder);
    }

    public Object getItem(int position) {
        if (addresses != null && addresses.size() > 0) {
            return addresses.get(position);
        }
        else {
            return null;
        }
    }
    @Override
    public int getItemCount() {

        return addresses.size();
    }

    @Override
    public void onClick(View v) {

        ViewHolderMap holder = (ViewHolderMap) v.getTag();

        switch (v.getId()){
            case R.id.item_layout: {
                int position = holder.getAdapterPosition();
                ((MapsActivity) context).itemClick(position);
                break;
            }
        }
    }

    public class ViewHolderMap extends RecyclerView.ViewHolder {

        RelativeLayout itemLayout;
        ImageView icon;
        TextView name;
        TextView address;
        RelativeLayout nearbyPlacesHeader;

        public ViewHolderMap(View v) {
            super(v);
        }
    }

    public void setAddresses (ArrayList<MapAddress> addresses) {
        log("setAddresses size: "+addresses.size());
        this.addresses = addresses;
        notifyDataSetChanged();
    }

    public void setMarker (String name, String address) {

    }

    public static void log(String message) {
        Util.log("MapsAdapter", message);
    }
}
