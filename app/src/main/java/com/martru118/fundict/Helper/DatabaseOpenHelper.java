package com.martru118.fundict.Helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.martru118.fundict.Model.Definition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This class accesses the database using SQLiteAssetHelper.
 * All database operations are performed in this class.
 */
public class DatabaseOpenHelper extends com.readystatesoftware.sqliteasset.SQLiteAssetHelper {
    private static final String dbName = "dictionary_big.db";
    private static final int dbVersion = 1;
    private Cursor c = null;

    public DatabaseOpenHelper(Context context) {
        super(context, dbName, null, dbVersion);
    }

    /**
     * Finds the definition for a given word.
     * The word can be specifically selected or randomly generated.
     * The final definition is set by retrieving all possible types and definitions of that word
     * and removing duplicate types and definitions.
     *
     * @param key -- The word to find the definition for.
     * @param isRandom -- Checks if a word needs to be randomly generated. If so, key would be null.
     * @return The final definition of a given word.
     */
    public Definition findDefinition(String key, boolean isRandom) {
        SQLiteDatabase db = getReadableDatabase();
        Definition definition = new Definition();

        if (isRandom && key==null) {
            //generate random word
            c = db.rawQuery("select word from words where _id=(abs(random())%(select (select max(_id) from words)+1))", new String[]{});
            c.moveToFirst();
            key = c.getString(c.getColumnIndex("word"));
        }

        //get word by entry
        c = db.query("words", null, "word=?", new String[]{key}, null, null, null);

        if (c.getCount()>0) {
            if (c.moveToFirst()) {
                //set individual definition
                definition.setWord(c.getString(c.getColumnIndex("word")));

                //stringbuilder used for adding type and defn to definition
                StringBuilder typeBuilder = new StringBuilder();
                StringBuilder defnBuilder = new StringBuilder();
                String allTypes, allDefns;

                //create sets storing types and definitions
                Set<String> wordTypes = new LinkedHashSet<>();
                Set<String> wordDefns = new LinkedHashSet<>();

                //get all definitions and types of a word
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    wordTypes.add(c.getString(c.getColumnIndex("type")));
                    wordDefns.add(c.getString(c.getColumnIndex("defn")));
                }

                //build final definition
                for (String type : wordTypes) {typeBuilder.append(type).append("\n");}
                for (String defn : wordDefns) {defnBuilder.append(defn).append("\n\n");}

                //set final definition
                allTypes = typeBuilder.substring(0, typeBuilder.length()-1);
                allDefns = defnBuilder.toString();
                definition.setType(allTypes);
                definition.setDefn(allDefns);
            }
        } else {
            //no definitions found
            definition = null;
        }

        c.close();
        db.close();
        return definition;
    }

    /**
     * Retrieves search suggestions from SQLite database based on search query.
     *
     * @param query -- The string to base the search from.
     * @return A list of words similar to the search query.
     */
    public List<String> getSuggestions(String query) {
        SQLiteDatabase db = getReadableDatabase();
        c = db.query(true,"words", new String[]{"word"},"word like ? and wlen>2", new String[]{query+"%"}, null, null, "_id asc, word asc", "6");
        List<String> results = new ArrayList<>();

        //retrieve top suggestions based on query
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
            results.add(c.getString(c.getColumnIndex("word")));

        c.close();
        db.close();
        return results;
    }

    /**
     * Retrieves previous words from database.
     *
     * @param asRecent -- If true, retrieve previously searched words instead.
     * @return A list of previous words.
     */
    public List<String> getHistory(boolean asRecent) {
        SQLiteDatabase db = getReadableDatabase();
        List<String> results = new ArrayList<>();

        if (asRecent) {
            //get search history
            c = db.query(true, "history", new String[]{"word"}, "recent=1", null, null, null, "_id desc", "6");
        } else {
            //get previous words
            c = db.rawQuery("select distinct word from history order by _id desc", null);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
            results.add(c.getString(c.getColumnIndex("word")));

        c.close();
        db.close();
        return results;
    }

    /**
     * Adds a word to history along with its 'recent' value.
     * The 'recent' value denotes if a word has been searched (0 = false; 1 = true).
     *
     * @param query -- The word to add to history.
     * @param isRecent -- The 'recent' value.
     */
    public void addtoHistory(String query, int isRecent) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues entry = new ContentValues();
        entry.put("word", query);
        entry.put("recent", isRecent);
        db.insert("history", null, entry);
        db.close();
    }

    /**
     * Removes a word from search history by changing its 'recent' value to 0.
     * 0 means the word has not been searched.
     *
     * @param query -- The word to remove from history.
     * @param clear -- If true, clear search history.
     */
    public void removefromHistory(String query, boolean clear) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues search = new ContentValues();
        search.put("recent", 0);

        if (clear && query==null) {
            //clear search history
            db.update("history", search, "recent=1", null);
        } else {
            //remove word from search history
            db.update("history", search, "word=?", new String[]{query});
        }

        db.close();
    }

    /**
     * @return A cursor containing all of the favorite definitions from database.
     */
    public Cursor getAllFavorites() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("select * from favorites order by word asc", null);
    }

    /**
     * Adds a definition to favorites.
     *
     * @param favDefn -- The definition to add to favorites.
     */
    public void addtoFavorites(Definition favDefn) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues bookmark = new ContentValues();
        bookmark.put("word", favDefn.getWord());
        bookmark.put("type", favDefn.getType());
        bookmark.put("defn", favDefn.getDefn());
        db.insert("favorites", null, bookmark);
        db.close();
    }

    /**
     * Removes a specific definition from favorites.
     * The definition is found by matching its ID to the ID of another definition in the same column.
     * If the IDs match, then the definition is removed.
     *
     * @param column -- The column to search in.
     * @param id -- The ID of a definition.
     */
    public void removefromFavorites(String column, String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("favorites", column+"=?", new String[]{id});
        db.close();
    }

    /**
     * Checks if a word exists in a table by counting the number of occurrences of a word.
     * A word exists if it has more than 0 occurrences.
     *
     * @param table -- The table to search in.
     * @param query -- The word to look for in the table.
     * @return If the query exists in the table.
     */
    public boolean exists(String table, String query) {
        SQLiteDatabase db = getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, table, "word like ?", new String[]{query});
        db.close();
        return count>0;
    }

    /**
     * Removes all values in a given table.
     *
     * @param table -- The table to remove values from.
     */
    public void clearTable(String table) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(table, null, null);
        db.close();
    }

    /**
     * Clears both History and Favorites tables when called.
     */
    public void clearAll() {
        clearTable("history");
        clearTable("favorites");
    }

    /**
     * Creates tables in database on first run.
     */
    public void initTables() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("create table if not exists favorites (_id integer not null primary key autoincrement unique, word text not null, type text, defn text not null)");
        db.execSQL("create table if not exists history (_id integer not null primary key autoincrement unique, word text not null, recent boolean not null check(recent in (0,1)))");
        db.close();
    }
}