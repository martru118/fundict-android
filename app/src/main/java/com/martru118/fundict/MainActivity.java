package com.martru118.fundict;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.martru118.fundict.Helper.DatabaseOpenHelper;
import com.martru118.fundict.Helper.ThemeHelper;
import com.martru118.fundict.ui.main.DefinitionFragment;

/**~~~~~~~~~~~~~~~~~~~~~~TODO~~~~~~~~~~~~~~~~~~~~~~
 * add text size slider (optional, save for last)
 * change dictionary database
 */
public class MainActivity extends AppCompatActivity {
    private DatabaseOpenHelper db;
    private int selection;
    private final int requestFlag = 1;

    private ThemeHelper theme;
    private DefinitionFragment definition = new DefinitionFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initTheme();

        //set up database
        db = new DatabaseOpenHelper(this);
        db.initTables();

        //set up fragment
        FragmentTransaction fragment = getSupportFragmentManager().beginTransaction();
        fragment.replace(R.id.home, definition).commit();

        //set up toolbar
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setOverflowIcon(getDrawable(R.drawable.ic_more_vert_white_24dp));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_button:
                Intent search = new Intent(MainActivity.this, SearchActivity.class);
                startActivityForResult(search, requestFlag);
                return true;

            //toggle themes
            case R.id.themes:
                int nightmodeFlags = this.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                switch (nightmodeFlags) {
                    //change to light theme
                    case Configuration.UI_MODE_NIGHT_YES:
                        theme.setNightModeState(false);
                        makeToast("Light theme enabled");
                        break;

                    //change to dark theme
                    case Configuration.UI_MODE_NIGHT_NO:
                        theme.setNightModeState(true);
                        makeToast("Dark theme enabled");
                        break;
                }

                restart();
                return true;

            case R.id.clear_data:
                final String[] options = {"Clear All", "Clear History", "Clear Search History", "Clear Favorites"};
                AlertDialog.Builder clear_builder = new AlertDialog.Builder(this);
                clear_builder.setTitle("Clear Data")
                        .setSingleChoiceItems(options, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                selection = which;
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (selection) {
                                    case 0:
                                        db.clearAll();
                                        makeToast("All data cleared");
                                        break;

                                    case 1:
                                        db.clearTable("history");
                                        makeToast("History cleared");
                                        break;

                                    case 2:
                                        db.clearSearchHistory();
                                        makeToast("Search History cleared");
                                        break;

                                    case 3:
                                        db.clearTable("favorites");
                                        makeToast("Favorites cleared");
                                        break;
                                }

                                dialog.dismiss();
                                restart();
                            }
                        })
                        .create().show();
                return true;

            //display help dialog
            case R.id.help_main:
                AlertDialog.Builder help_builder = new AlertDialog.Builder(this);
                help_builder.setTitle("Getting Started")
                        .setItems(getResources().getStringArray(R.array.help_main), null)
                        .setPositiveButton("OK", null)
                        .create().show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //change definition
        if (requestCode==requestFlag) {
            if (resultCode==RESULT_OK) {
                String searchResult = data.getStringExtra("result");
                definition.show(db.findDefinition(searchResult, false), 1);
            }
        }
    }

    //set up app theme
    private void initTheme() {
        theme = new ThemeHelper(this);
        if (theme.loadNightMode())
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    //set up toast messages
    private void makeToast(String message) {
        Toast alert = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
        alert.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 200);
        alert.show();
    }

    //restart activity
    private void restart() {
        finish();
        startActivity(getIntent());
    }
}