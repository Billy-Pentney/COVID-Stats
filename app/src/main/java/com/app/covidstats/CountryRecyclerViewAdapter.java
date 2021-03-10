package com.app.covidstats;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.covidstats.region.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CountryRecyclerViewAdapter extends RecyclerView.Adapter<CountryRecyclerViewAdapter.MyViewHolder> {
    private ArrayList<Region> mData = new ArrayList<>();
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private int lastPosition = -1;
    private final Context context;
    private Comparator<Region> regionComparator = Region.CasesComparator;

    // data is passed into the constructor
    public CountryRecyclerViewAdapter(Context context, ArrayList<Region> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData.addAll(data);
        this.context = context;
    }

    public void UpdateData(ArrayList<Region> newData) {
        mData = newData;
        sortData();
    }

    public void sort(int comparatorID) {
        Comparator<Region> comparator = Region.getComparatorFromMenuID(comparatorID);
        if (regionComparator != null) {
            regionComparator = comparator;
            sortData();
        }
    }

    private void sortData() {
        Collections.sort(mData, regionComparator);
        notifyDataSetChanged();
    }

    // inflates the row layout from xml when needed
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.country_view, parent, false);
        return new MyViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(MyViewHolder holder, int index) {
        Region region = mData.get(index);

        holder.indexTxt.setText(Region.IntToString( index + 1));
        holder.nameTxt.setText(region.getName());
        holder.populationTxt.setText(region.getPopulation());
        holder.casesTxt.setText(region.getTotalCases());
        holder.deathsTxt.setText(region.getTotalDeaths());

        Drawable flagDrawable = region.getFlag(context);
        if (flagDrawable != null) {
            holder.flagImg.setVisibility(View.VISIBLE);
            holder.flagImg.setImageDrawable(flagDrawable);
        }
        else {
            holder.flagImg.setVisibility(View.GONE);
        }

        setAnimation(holder.parent, index);
    }

    @Override
    public void onViewRecycled(@NonNull MyViewHolder holder) {
        holder.parent.clearAnimation();
        super.onViewRecycled(holder);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            // descending scroll
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    // stores and recycles views as they are scrolled off screen
    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        RelativeLayout parent;
        TextView indexTxt;
        TextView nameTxt;
        TextView populationTxt;
        TextView casesTxt;
        TextView deathsTxt;
        ImageView flagImg;

        MyViewHolder(View itemView) {
            super(itemView);
            parent = itemView.findViewById(R.id.parent);
            indexTxt = itemView.findViewById(R.id.countryIndexTxt);
            nameTxt = itemView.findViewById(R.id.countryNameTxt);
            populationTxt = itemView.findViewById(R.id.countryPopTxt);
            casesTxt = itemView.findViewById(R.id.countryCasesTxt);
            deathsTxt = itemView.findViewById(R.id.countryDeathsTxt);
            flagImg = itemView.findViewById(R.id.flagImg);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            if (mClickListener != null) mClickListener.onItemClick(view, position);
        }
    }

    // convenience method for getting data at click position
    Region getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}