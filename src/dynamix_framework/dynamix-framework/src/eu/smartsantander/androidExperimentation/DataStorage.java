package eu.smartsantander.androidExperimentation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

public class DataStorage extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "experimentalMessagesDB.db";
	private static final String TABLE_MESSAGES = "messages";

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_MESSAGE = "message";

	public DataStorage(Context context, String name, CursorFactory factory,int version) {
		super(context, DATABASE_NAME, factory, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
				+ COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_MESSAGE
				+ " TEXT" + ")";
		db.execSQL(CREATE_MESSAGES_TABLE);

	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	public synchronized void addMessage(String message) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_MESSAGE, message);
		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(TABLE_MESSAGES, null, values);
		db.close();
	}

	public synchronized void deleteMessage(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_MESSAGES, COLUMN_ID + " = ?",new String[] { String.valueOf(id) });
		db.close();
	}

	public synchronized Pair<Long, String> getMessage() {
		String query = "Select * FROM " + TABLE_MESSAGES
				+ " WHERE rowid= (SELECT MIN(rowid) FROM " + TABLE_MESSAGES
				+ ")";
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);
		Pair<Long, String> idMessage = new Pair<Long, String>(0L, "");
		if (cursor.moveToFirst()) {
			cursor.moveToFirst();
			idMessage = new Pair<Long, String>(Long.parseLong(cursor
					.getString(0)), cursor.getString(1));
			cursor.close();
		} else {
			idMessage = new Pair<Long, String>(0L, "");
		}
		db.close();
		return idMessage;
	}

	public synchronized Long size() {
		String query = "Select COUNT(*) FROM " + TABLE_MESSAGES + "";
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);
		Long size = 0L;
		if (cursor.moveToFirst()) {
			cursor.moveToFirst();
			size = Long.parseLong(cursor.getString(0));
			cursor.close();
		}
		db.close();
		return size;
	}

}