package com.example.david.sharing;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by david on 29-12-2015.
 */
public class FileListAdapter extends BaseAdapter {
    private ArrayList<File> files;
    private LayoutInflater mInflator;
    Activity mother;

    public FileListAdapter(Context context, Activity mother, ArrayList<File> fileList) {
        files = fileList;
        mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mother = mother;
        refreshFiles();
    }

    public void refreshFiles(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    mother.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                    SystemClock.sleep(1000);
                }
            }
        }).start();
    }

    public int getCount() {
        return files.size();
    }

    public Object getItem(int i) {
        return files.get(i);
    }

    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView (int i, View view, ViewGroup viewGroup){

        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.session_item, null);
            viewHolder = new ViewHolder();
            viewHolder.name = (TextView) view.findViewById(R.id.session_item_name);
            viewHolder.owner = (TextView) view.findViewById(R.id.session_item_owner);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        File file = files.get(i);

        viewHolder.owner.setText(file.getName());
        viewHolder.name.setText(file.getOwnerMail());
        return view;
    }

    static class ViewHolder{
        TextView name;
        TextView owner;
    }
}
