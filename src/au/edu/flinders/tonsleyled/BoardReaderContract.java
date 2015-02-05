package au.edu.flinders.tonsleyled;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class BoardReaderContract {

	public BoardReaderContract() {}

	public static abstract class FeedEntry implements BaseColumns {
		public static final String TABLE_NAME = "board";
		public static final String COLUMN_NAME_TITLE = "title";
		public static final String COLUMN_NAME_CONTENT = "content";
		public static final String COLUMN_NAME_NULLABLE = "null";

	}
	private static final String TEXT_TYPE = " TEXT";
	private static final String COMMA_SEP = ",";
	private static final String SQL_CREATE_ENTRIES =
	    "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
	    BaseColumns._ID + " INTEGER PRIMARY KEY," +
	    FeedEntry.COLUMN_NAME_TITLE + TEXT_TYPE +" UNIQUE "+ COMMA_SEP +
	    FeedEntry.COLUMN_NAME_CONTENT + TEXT_TYPE  +
	    " )";

	private static final String SQL_DELETE_ENTRIES =
	    "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;
	public static void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_ENTRIES);
	}
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS "+ FeedEntry.TABLE_NAME);
		onCreate(db);
	}
}
