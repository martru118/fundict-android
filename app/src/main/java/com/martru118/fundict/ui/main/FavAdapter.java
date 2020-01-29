package com.martru118.fundict.ui.main;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.martru118.fundict.R;

//cursor adapter for recycler view
public class FavAdapter extends RecyclerView.Adapter<FavAdapter.FavoritesViewHolder> {
    private Context mContext;
    private Cursor mCursor;

    public FavAdapter(Context context, Cursor cursor) {
        this.mContext = context;
        this.mCursor = cursor;
    }

    @NonNull
    @Override
    public FavAdapter.FavoritesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.favorites_item, parent, false);
        return new FavAdapter.FavoritesViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FavAdapter.FavoritesViewHolder holder, int position) {
        if (!mCursor.moveToPosition(position))
            return;     //exit if cursor does not exist

        String word_item = mCursor.getString(mCursor.getColumnIndex("word"));
        String type_item = mCursor.getString(mCursor.getColumnIndex("type"));
        String definition_item = mCursor.getString(mCursor.getColumnIndex("defn"));
        long id = mCursor.getLong(mCursor.getColumnIndex("_id"));

        holder.word.setText(word_item);
        holder.type.setText(type_item);
        holder.defn.setText(definition_item);
        holder.star.setImageResource(R.drawable.ic_star_24dp);
        holder.itemView.setTag(id);

        //set default state for defn
        holder.defn.setMaxLines(1);
        holder.defn.setEllipsize(TextUtils.TruncateAt.END);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mCursor.close();
    }

    public void swapCursor(Cursor newCursor) {
        if (mCursor!=null)
            mCursor.close();

        mCursor = newCursor;

        if (newCursor!=null)
            notifyDataSetChanged();
    }

    public class FavoritesViewHolder extends RecyclerView.ViewHolder {
        private TextView word, type, defn;
        private ImageView star;

        private FavoritesViewHolder(@NonNull View itemView) {
            super(itemView);

            word = itemView.findViewById(R.id.item_word);
            type = itemView.findViewById(R.id.item_type);
            defn = itemView.findViewById(R.id.item_definition);
            star = itemView.findViewById(R.id.star);

            //expand textview
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (defn.getMaxLines()!=1) {
                        defn.setMaxLines(1);
                        defn.setEllipsize(TextUtils.TruncateAt.END);
                    } else {
                        defn.setMaxLines(Integer.MAX_VALUE);
                        defn.setEllipsize(null);
                    }
                }
            });
        }
    }
}
