package au.edu.flinders.tonsleyled;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import au.edu.flinders.tonsleyled.BoardReaderContract.FeedEntry;

public class MainActivity extends Activity {

	private int mSize = 17;
	private int[][] mBoard = new int[mSize][mSize];

	private EditText mEditTextY, mEditTextX;
	private MainActivity mActivity;
	private BoardView mBoardView;
	private static final String TAG = MainActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		clearBoard();
		setContentView(R.layout.activity_main);
		mEditTextX = (EditText) findViewById(R.id.editTextX);
		mEditTextY = (EditText) findViewById(R.id.editTextY);

		mEditTextX.setFilters(new InputFilter[] { new InputFilterMinMax(0,
				mSize) });
		mEditTextY.setFilters(new InputFilter[] { new InputFilterMinMax(0,
				mSize) });
		Button clearButton = (Button) findViewById(R.id.button1);
		clearButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				clearBoard();
				mBoardView.invalidate();
			}
		});
		LinearLayout ll = (LinearLayout) findViewById(R.id.mainView);

		mBoardView = new BoardView(this);

		ll.addView(mBoardView, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mActivity = this;
	}

	protected void clearBoard() {
		for (int i = 0; i < mBoard.length; i++) {
			for (int j = 0; j < mBoard[i].length; j++) {
				mBoard[i][j] = 0;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_save) {
			saveBoard();
			return true;
		} else if (id == R.id.action_load) {
			loadBoard();
			return true;
		} else if (id == R.id.action_send) {
			sendBoard();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private int getX() {
		return Integer.parseInt(mEditTextX.getEditableText().toString());
	}

	private int getY() {
		return Integer.parseInt(mEditTextY.getEditableText().toString());
	}

	private void showToast(final String message) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	/**
	 * Board is sent in the format: x=1,y=2,board=0,1,1,0,/ 0,1,1,1,/ 0,1,1,1,/
	 * .
	 */
	private void sendBoard() {
		(new Thread(new Runnable() {

			@Override
			public void run() {

				PrintWriter out;
				try {
					Socket sock = new Socket("ledsign", 12345);
					out = new PrintWriter(sock.getOutputStream(), true);

					StringBuilder sb = new StringBuilder();
					sb.append("x=").append(getX()).append(",").append("y=")
							.append(getY()).append("board=");
					for (int i = 0; i < mBoard.length; i++) {
						for (int j = 0; j < mBoard[i].length; j++) {
							sb.append(mBoard[i][j] + ",");
						}
						sb.append("/");
					}
					sb.append(".");
					out.write(sb.toString());
					sock.close();
					showToast("Sent successfully");
				} catch (IOException e) {
					showToast("Failed to send");
					e.printStackTrace();
				}

			}
		})).start();
	}

	private void loadBoard() {
		showSaveLoadDialog(false);
	}

	private void saveBoard() {
		showSaveLoadDialog(true);
	}

	private String[] getBoardTitles(final boolean save) {
		ArrayList<String> files = new ArrayList<String>();
		BoardReaderDbHelper mDbHelper = new BoardReaderDbHelper(mActivity);
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor c = db.query(FeedEntry.TABLE_NAME,
				new String[] { FeedEntry.COLUMN_NAME_TITLE }, null, null, null,
				null, FeedEntry.COLUMN_NAME_TITLE + " DESC");
		while (c.moveToNext()) {
			c.moveToFirst();
			files.add(getString(0));
		}
		if (save) {
			files.add("New board");
		}
		return files.toArray(new String[files.size()]);
	}

	private void showSaveLoadDialog(final boolean save) {
		Log.i(TAG, "Showing save dialog");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		int action_save_title = save ? R.string.dialog_save_board
				: R.string.dialog_load_board;
		builder.setTitle(action_save_title);
		builder.setNegativeButton(R.string.dialog_cancel, null);
		final EditText input = new EditText(this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		input.setLayoutParams(lp);
		input.setVisibility(View.GONE);
		builder.setView(input);
		builder.setSingleChoiceItems(getBoardTitles(save), -1,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						ListView lv = ((AlertDialog) dialog).getListView();
						String fname = (String) lv.getAdapter().getItem(
								lv.getCheckedItemPosition());
						if (fname.equals("New File") && save) {
							input.setVisibility(View.VISIBLE);
						} else {
							input.setVisibility(View.GONE);
						}
					}
				});
		int action_save = save ? R.string.action_save : R.string.action_load;
		builder.setPositiveButton(action_save, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ListView lv = ((AlertDialog) dialog).getListView();
				String fname = null;
				try {
					fname = (String) lv.getAdapter().getItem(
							lv.getCheckedItemPosition());
				} catch (IndexOutOfBoundsException e) {
					Toast.makeText(mActivity,
							"No file " + (save ? "saved" : "loaded"),
							Toast.LENGTH_SHORT).show();
					return;
				}
				if (fname != null) {
					if (fname.equals("New board")) {
						fname = input.getText().toString();
					}
					if (save) {
						saveBoardToDb(fname);
					} else {
						loadBoardFromDb(fname);
					}
					dialog.dismiss();
				}
			}
		});
		builder.create().show();
	}

	protected void loadBoardFromDb(String fname) {
		BoardReaderDbHelper mDbHelper = new BoardReaderDbHelper(mActivity);
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor c = db.query(FeedEntry.TABLE_NAME,
				new String[] { FeedEntry.COLUMN_NAME_CONTENT },
				FeedEntry.COLUMN_NAME_TITLE, new String[] { fname }, null,
				null, FeedEntry.COLUMN_NAME_TITLE + " DESC");
		c.moveToFirst();
		String boardString = c.getString(0);
		try {
			mBoard = deserializeBoard(boardString);
			showToast("Loaded board " + fname);
		} catch (JSONException e) {
			showToast("Could not load board");
			e.printStackTrace();
		}
	}

	protected void saveBoardToDb(String fname) {
		BoardReaderDbHelper mDbHelper = new BoardReaderDbHelper(mActivity);
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(FeedEntry.COLUMN_NAME_TITLE, fname);
		try {
			values.put(FeedEntry.COLUMN_NAME_CONTENT, serializeBoard());
			long rowId = db.insert(FeedEntry.TABLE_NAME, "null", values);
			showToast("Saved board " + fname);
		} catch (JSONException e) {
			showToast("Could not save board " + fname);
			e.printStackTrace();
		}

	}

	private int[][] deserializeBoard(String b) throws JSONException {
		JSONArray arr = new JSONArray(b);
		int[][] out = new int[arr.length()][arr.getJSONArray(0).length()];
		for (int i = 0; i < arr.length(); i++) {
			JSONArray subarr = arr.getJSONArray(i);
			for (int j = 0; j < subarr.length(); j++) {
				out[i][j] = subarr.getInt(j);
			}
		}
		return out;
	}

	private String serializeBoard() throws JSONException {
		JSONArray arr = new JSONArray();
		for (int i = 0; i < mBoard.length; i++) {
			JSONArray subarr = new JSONArray();
			for (int j = 0; j < mBoard[i].length; j++) {
				subarr.put(mBoard[i][j]);
			}
			arr.put(subarr);
		}
		return arr.toString();
	}

	private class BoardView extends View {
		private final String BTAG = BoardView.class.getSimpleName();
		private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

		public BoardView(Context context) {
			super(context);
			initView(context, null);
		}

		public BoardView(Context context, AttributeSet attrs) {
			super(context, attrs);
			initView(context, attrs);
		}

		public BoardView(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			initView(context, attrs);
		}

		private void initView(Context context, AttributeSet attrs) {
			mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mPaint.setStrokeWidth(2.0f);
			setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent e) {
					if (e.getAction() == MotionEvent.ACTION_DOWN) {
						int x = (int) (e.getX() / (getWidth() / mSize));
						int y = (int) (e.getY() / (getHeight() / mSize));
						mBoard[x][y] = (~mBoard[x][y] & 1);
						Log.i(BTAG, "Marking cell at: x=" + x + " y=" + y
								+ " as " + mBoard[x][y]);
						invalidate();
						return true;
					}
					return false;

				}
			});
		}

		@Override
		protected void onDraw(Canvas canvas) {
			for (int x = 0; x < mBoard.length; x++) {
				for (int y = 0; y < mBoard[x].length; y++) {
					int cellWidth = getWidth()/mSize;
					int cellHeight= getHeight()/mSize;
					int left = x*cellWidth;
					int top = y*cellHeight;
					int right= left + cellWidth;
					int bottom = top + cellHeight;
					if (mBoard[x][y] == 1) {
						mPaint.setColor(Color.BLACK);
						mPaint.setStyle(Style.FILL);
						canvas.drawRect(left, top, right, bottom, mPaint);
						/*canvas.drawRect(
								x * (getWidth() / mSize),
								y* (getHeight() / mSize),
								getWidth() / mSize,
								getWidth() / mSize,
								mPaint);*/
					}
					mPaint.setStyle(Style.STROKE);
					mPaint.setColor(Color.GRAY);
					canvas.drawRect(left, top, right, bottom, mPaint);
					/*
					canvas.drawRect(
							x * (getWidth() / mSize),
							y* (getHeight() / mSize), 
							getWidth() / mSize,
							getWidth() / mSize, 
							mPaint);*/
				}
			}
		}
	}

}
