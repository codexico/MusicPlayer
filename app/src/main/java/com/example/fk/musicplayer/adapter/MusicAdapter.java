package com.example.fk.musicplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.fk.musicplayer.R;
import com.example.fk.musicplayer.model.Music;

import java.util.List;

public class MusicAdapter extends BaseAdapter {

    private final Context context;
    private final List<Music> musicList;

    public MusicAdapter(Context context, List<Music> musicList) {
        this.context = context;
        this.musicList = musicList;
    }

    @Override
    public int getCount() {
        return musicList.size();
    }

    @Override
    public Object getItem(int position) {
        return musicList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return musicList.get(position).getId();
    }

    private class ViewHolder {
        TextView tvTitle;
        TextView tvArtist;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        Music music = musicList.get(position);

        if (convertView == null) {
            holder = new ViewHolder();

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.music_item, parent, false);

            holder.tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
            holder.tvArtist = (TextView) convertView.findViewById(R.id.tvArtist);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvTitle.setText(music.getTitle());
        holder.tvArtist.setText(music.getArtist());

        return convertView;
    }
}
