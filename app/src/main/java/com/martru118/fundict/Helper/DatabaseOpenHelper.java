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
import java.util.TreeSet;

public class DatabaseOpenHelper extends com.readystatesoftware.sqliteasset.SQLiteAssetHelper {
    private static final String dbName = "dictionary_big.db";
    private static final int dbVersion = 1;
    private Cursor c = null;

    public DatabaseOpenHelper(Context context) {
        super(context, dbName, null, dbVersion);
    }

    //dictionary engine
    public Definition findDefinition(String key, boolean isRandom) {
        SQLiteDatabase db = getReadableDatabase();
        Definition definition = new Definition();

        if (isRandom) {
            //generate random word
            c = db.rawQuery("select word from words where _rowid_=(abs(random())%(select (select max(_rowid_) from words)+1))", new String[]{});
            c.moveToFirst();
            key = c.getString(c.getColumnIndex("word"));
        }

        //get word by entry
        c = db.query("words", null, "word=?", new String[]{key}, null, null, null);

        if (c.getCount()>0) {
            if (c.moveToFirst()) {
                //set individual definition
                definition.setWord(c.getString(c.getColumnIndex("word")));

                StringBuilder typeBuilder = new StringBuilder();
                StringBuilder defnBuilder = new StringBuilder();
                Set<String> wordTypes = new TreeSet<>();
                Set<String> wordDefns = new LinkedHashSet<>();

                //get all definitions and types of a word
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    wordTypes.add(c.getString(c.getColumnIndex("type")));
                    wordDefns.add(c.getString(c.getColumnIndex("defn")));
                }

                //build final definition
                for (String type : wordTypes) {typeBuilder.append(type).append("\n");}
                for (String defn : wordDefns) {defnBuilder.append(defn).append("\n\n");}
                definition.setType(typeBuilder.substring(0, typeBuilder.length()-1));
                definition.setDefn(defnBuilder.substring(0, defnBuilder.length()-2));
            }
        } else {
            //no definitions found
            definition = null;
        }

        c.close();
        db.close();
        return definition;
    }

    public List<String> getSuggestions(String query) {
        SQLiteDatabase db = getReadableDatabase();
        c = db.query(true,"words", new String[]{"word"},"word like ? and wlen>2", new String[]{query+"%"}, null, null, "word asc", "6");
        List<String> results = new ArrayList<>();

        //show top suggestions based on query
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
            results.add(c.getString(c.getColumnIndex("word")));

        c.close();
        db.close();
        return results;
    }

    public List<String> getHistory(boolean asRecent) {
        SQLiteDatabase db = getReadableDatabase();
        List<String> results = new ArrayList<>();

        if (asRecent) {
            //get search history
            c = db.query(true, "history", new String[]{"word"}, "recent=1", null, null, null, "_id desc", "6");
        } else {
            //get previous words
            c = db.rawQuery("select distinct word from history", null);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
            results.add(c.getString(c.getColumnIndex("word")));

        c.close();
        db.close();
        return results;
    }

    public void addtoHistory(String query, int isRecent) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues search = new ContentValues();
        search.put("word", query);
        search.put("recent", isRecent);
        db.insert("history", null, search);
        db.close();
    }

    public void removefromHistory(String query) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues search = new ContentValues();
        search.put("recent", 0);
        db.update("history", search, "word=?", new String[]{query});
        db.close();
    }

    public void clearSearchHistory() {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues search = new ContentValues();
        search.put("recent", 0);
        db.update("history", search, "recent=1", null);
        db.close();
    }

    public Cursor getAllFavorites() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("select * from favorites order by word asc", null);
    }

    public void addtoFavorites(Definition favDefn) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues bookmark = new ContentValues();
        bookmark.put("word", favDefn.getWord());
        bookmark.put("type", favDefn.getType());
        bookmark.put("defn", favDefn.getDefn());
        db.insert("favorites", null, bookmark);
        db.close();
    }

    public void removefromFavorites(String column, String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("favorites", column+"=?", new String[]{id});
        db.close();
    }

    public boolean exists(String table, String query) {
        SQLiteDatabase db = getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, table, "word like ?", new String[]{query});
        db.close();
        return count>0;
    }

    public void clearTable(String table) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(table, null, null);
        db.close();
    }

    public void clearAll() {
        clearTable("history");
        clearTable("favorites");
    }

    public void initTables() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("create table if not exists favorites (_id integer not null primary key autoincrement unique, word text not null, type text, defn text not null)");
        db.execSQL("create table if not exists history (_id integer not null primary key autoincrement unique, word text not null, recent boolean not null check(recent in (0,1)))");
        db.close();
    }
}