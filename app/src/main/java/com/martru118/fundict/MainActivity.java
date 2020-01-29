package com.martru118.fundict;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.martru118.fundict.Helper.DatabaseOpenHelper;
import com.martru118.fundict.Helper.ThemeHelper;
import com.martru118.fundict.Model.Definition;
import com.martru118.fundict.ui.main.DefinitionFragment;
import com.martru118.fundict.ui.main.FavoritesFragment;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<Definition> result;
    private DatabaseOpenHelper db;
    private FragmentRefreshListener refreshListener;
    final Fragment definitionsFragment = new DefinitionFragment();
    final Fragment favoritesFragment = new FavoritesFragment();
    private ThemeHelper theme;

    private MaterialSearchView mSearchView;
    private MenuItem searchIcon;
    private BottomNavigationView nav;

    private List<String> previous;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initTheme();

        //initialize database
        db = new DatabaseOpenHelper(this);
        db.initTables();
        updateHistory();

        //set up navigation bar
        nav = findViewById(R.id.navigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        this.setDefaultFragment(definitionsFragment);

        //set up toolbar
        final Toolbar searchbar = findViewById(R.id.toolbar);
        setSupportActionBar(searchbar);
        searchbar.setTitle(R.string.app_name);
        searchbar.setTitleTextColor(Color.parseColor("#ffffff"));
        searchbar.setOverflowIcon(getDrawable(R.drawable.ic_more_vert_white_24dp));

        //set up searchview
        mSearchView = findViewById(R.id.search_view);
        mSearchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                nav.setVisibility(View.GONE);
            }

            @Override
            public void onSearchViewClosed() {
                nav.setVisibility(View.VISIBLE);
            }
        });
        mSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                result = db.findDefinition(query.toLowerCase());

                if (result==null) {
                    Toast.makeText(MainActivity.this, "Word not found", Toast.LENGTH_SHORT).show();
                } else {
                    //update search history
                    mSearchView.closeSearch();
                    db.addSearch(query.toLowerCase());
                    updateHistory();

                    //get most common definition
                    getDefinition(result.get(0));
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText!=null && !newText.isEmpty())
                    mSearchView.setSuggestions(db.getSuggestions(newText.toLowerCase()).toArray(new String[0]));

                mSearchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        CharSequence query = (CharSequence) parent.getItemAtPosition(position);
                        mSearchView.setQuery(query, true);
                    }
                });

                return true;
            }
        });
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.navigation_home:
                    searchIcon.setVisible(true);
                    replaceFragment(definitionsFragment);
                    updateHistory();
                    return true;

                case R.id.navigation_bookmarks:
                    searchIcon.setVisible(false);
                    replaceFragment(favoritesFragment);
                    return true;
            }

            return false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        searchIcon = menu.findItem(R.id.search_button);
        mSearchView.setMenuItem(searchIcon);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.themes:
                int nightmodeFlags = this.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                nav.setSelectedItemId(R.id.navigation_home);

                switch (nightmodeFlags) {
                    case Configuration.UI_MODE_NIGHT_YES:
                        //change to light theme
                        theme.setNightModeState(false);
                        initTheme();
                        makeToast("Light theme enabled");
                        break;

                    case Configuration.UI_MODE_NIGHT_NO:
                        //change to dark theme
                        theme.setNightModeState(true);
                        initTheme();
                        makeToast("Dark theme enabled");
                        break;
                }
                return true;

            case R.id.clear_history:
                db.clearTable("history");
                updateHistory();
                makeToast("History cleared");
                return true;

            case R.id.clear_favorites:
                db.clearTable("favorites");
                makeToast("Bookmarks cleared");

                //update list in fragment
                if (getRefreshListener()!=null)
                    getRefreshListener().onRefresh();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        //close searchview
        if (mSearchView.isSearchOpen()) {
            mSearchView.closeSearch();
            return;
        }

        //iterate through past definitions
        if (nav.getSelectedItemId()==R.id.navigation_home) {
            if (position>0) {
                position--;
                result = db.findDefinition(previous.get(position));
                getDefinition(result.get(0));
            } else {
                makeToast("End of history");
            }

            return;
        }
    }

    //dynamically add fragments
    private void setDefaultFragment(Fragment defaultFragment) {
        this.replaceFragment(defaultFragment);
    }
    private void replaceFragment(Fragment destination) {
        FragmentManager fm = this.getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.frag_frame, destination);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    //refresh fragment list
    public interface FragmentRefreshListener {
        void onRefresh();
    }
    public FragmentRefreshListener getRefreshListener() {
        return refreshListener;
    }
    public void setRefreshListener(FragmentRefreshListener frl) {
        this.refreshListener = frl;
    }

    //set up app theme
    private void initTheme() {
        theme = new ThemeHelper(this);
        if (theme.loadNightMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    //set up toast messages
    private void makeToast(String message) {
        Toast alert = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
        alert.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 200);
        alert.show();
    }

    private void getDefinition(Definition wordDef) {
        DefinitionFragment definition = (DefinitionFragment)getSupportFragmentManager().findFragmentById(R.id.frag_frame);
        definition.changeText(wordDef);
    }

    public void updateHistory() {
        previous = db.getSearchHistory();
        position = previous.size();
    }
}

