package com.martru118.fundict.Helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.martru118.fundict.Model.Definition;

import java.util.ArrayList;
import java.util.List;

public class DatabaseOpenHelper extends com.readystatesoftware.sqliteasset.SQLiteAssetHelper {
    private static final String dbName = "dictionary_big.db";
    private static final int dbVersion = 1;
    private Cursor c = null;

    public DatabaseOpenHelper(Context context) {
        super(context, dbName, null, dbVersion);
    }

    public List<Definition> findDefinition(String key) {
        SQLiteDatabase db = getReadableDatabase();
        List<Definition> definitions = new ArrayList<>();

        //find definition depending on source
        if (key.equals("generateRandom()"))
            c = db.rawQuery("select * from words where _rowid_=(abs(random())%(select (select max(_rowid_) from words)+1))", new String[]{});
        else
            c = db.query("words", null, "word=?", new String[]{key}, null, null, null);

        //set definition
        if (c.getCount()>0) {
            if (c.moveToFirst()) {
                do {
                    Definition defn = new Definition();
                    defn.setWord(c.getString(c.getColumnIndex("word")));
                    defn.setType(c.getString(c.getColumnIndex("type")));
                    defn.setDefn(c.getString(c.getColumnIndex("defn")));

                    definitions.add(defn);
                } while (c.moveToNext());
            }

            c.close();
            db.close();
            return definitions;
        } else {
            c.close();
            db.close();
            return null;
        }
    }

    public List<String> getSuggestions(String query) {
        SQLiteDatabase db = getReadableDatabase();
        c = db.query(true,"words", new String[]{"word"},"word like ? and wlen>2", new String[]{query+"%"}, null, null, "word asc", "10");
        List<String> result = new ArrayList<>();

        //show top 10 suggestions
        if (c.moveToFirst()) {
            do {
                result.add(c.getString(c.getColumnIndex("word")));
            } while (c.moveToNext());
        }

        c.close();
        db.close();
        return result;
    }

    public void addSearch(String query) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("searches", query);
        db.insert("history", null, values);
        db.close();
    }

    public List<String> getSearchHistory() {
        SQLiteDatabase db = getReadableDatabase();
        c = db.rawQuery("select distinct searches from history", null);
        List<String> searches = new ArrayList<>();

        if (c.moveToFirst()) {
            do {
                searches.add(c.getString(c.getColumnIndex("searches")));
            } while (c.moveToNext());
        }

        c.close();
        db.close();
        return searches;
    }

    public Cursor getAllFavorites() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("select * from favorites", null);
    }

    public void addtoFavorites(String favWord, String favType, String favDefn) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("word", favWord);
        values.put("type", favType);
        values.put("defn", favDefn);
        db.insert("favorites", null, values);
        db.close();
    }

    public void swipetoRemove(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from favorites where _id="+id);
        db.close();
    }

    public void clicktoRemove(String word, String type, String defn) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from favorites where word=? and type=? and defn=?", new String[]{word, type, defn});
        db.close();
    }

    public boolean isFavorite(String word, String type, String defn) {
        SQLiteDatabase db = getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, "favorites", "word=? and type=? and defn=?", new String[]{word, type, defn});
        db.close();
        return count>0;
    }

    public void clearTable(String table) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(table, null, null);
        db.close();
    }

    public void initTables() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("create table if not exists favorites (_id integer not null primary key autoincrement unique, word text not null, type text, defn text not null)");
        db.execSQL("create table if not exists history (_id integer not null primary key autoincrement unique, searches text not null)");
        db.close();
    }
}
