package com.martru118.fundict.ui.search;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.martru118.fundict.Helper.DatabaseOpenHelper;
import com.martru118.fundict.R;

import in.myinnos.alphabetsindexfastscrollrecycler.IndexFastScrollRecyclerView;

public class FavoritesFragment extends Fragment {
    private DatabaseOpenHelper db;
    private FavAdapter adapter;

    private IndexFastScrollRecyclerView favoritesList;
    private TextView empty;

    public FavoritesFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frame_favorites, container, false);
        db = new DatabaseOpenHelper(getContext());
        empty = v.findViewById(R.id.empty);

        //set up recycler view and its adapter
        favoritesList = v.findViewById(R.id.fav_list);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        favoritesList.setLayoutManager(llm);
        adapter = new FavAdapter(getContext(), db.getAllFavorites());
        favoritesList.setAdapter(adapter);
        favoritesList.scrollToPosition(0);

        //swipe to remove
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                removeFromFavorites((long)viewHolder.itemView.getTag());
                setCurrentView();
            }
        }).attachToRecyclerView(favoritesList);

        setCurrentView();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        //close remaining database access
        adapter.getCursor().close();
        db.close();
    }

    /**
     * Removes a definition from favorites based on its ID in the database.
     * Each item in the recyclerview is assigned a numerical ID.
     *
     * @param id -- The ID of the definition in the favorites table.
     */
    private void removeFromFavorites(long id) {
        db.removefromFavorites("_id", String.valueOf(id));
        adapter.swapCursor(db.getAllFavorites());
        Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays either a RecyclerView or a TextView based on the number of items in the adapter.
     * If the adapter is empty, display the TextView; otherwise display the RecyclerView.
     */
    private void setCurrentView() {
        if (adapter.getItemCount()>0) {
            empty.setVisibility(View.GONE);
            favoritesList.setVisibility(View.VISIBLE);
        } else {
            favoritesList.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        }
    }
}
