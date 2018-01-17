package com.github.shauway.mal.demo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JavaType;
import com.github.shauway.mal.R;
import com.github.shauway.mal.demo.LibraryListFragment.OnListFragmentInteractionListener;
import com.github.shauway.mal.demo.dummy.LibraryListContent.LibraryItem;
import com.github.shauway.mal.demo.utils.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link LibraryItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyLibraryRecyclerViewAdapter extends RecyclerView.Adapter<MyLibraryRecyclerViewAdapter.ViewHolder> {

    private List<LibraryItem> mValues;
    private final OnListFragmentInteractionListener mListener;
    private Context context;

    public MyLibraryRecyclerViewAdapter(Context context, OnListFragmentInteractionListener listener) {
        this.context = context;
        mListener = listener;

        InputStream is = null;
        try {
            is = context.getAssets().open("library_list_item.json");
            JavaType valueType = JsonUtil.parametricType(List.class, LibraryItem.class);
            mValues = JsonUtil.getObjectMapper().readValue(is, valueType);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_library_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.tvTitle.setText(mValues.get(position).getTitle());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView tvTitle;
        public LibraryItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            tvTitle = (TextView) view.findViewById(R.id.title);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + tvTitle.getText() + "'";
        }
    }
}
