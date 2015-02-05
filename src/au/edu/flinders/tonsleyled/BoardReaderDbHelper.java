package au.edu.flinders.tonsleyled;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BoardReaderDbHelper extends SQLiteOpenHelper {
	public static final int DATABASE_VERSION = 2;
	public static final String DATABASE_NAME = "BoardReader.db";

	public BoardReaderDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		BoardReaderContract.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		BoardReaderContract.onUpgrade(db, oldVersion, newVersion);
	}
}
