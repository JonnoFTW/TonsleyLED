package au.edu.flinders.tonsleyled;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import au.edu.flinders.tonsleyled.BoardReaderContract.BoardEntry;

public class MainActivity extends Activity {

	private int mSize = 17;
	private int[][] mBoard = new int[mSize][mSize];

	private EditText mEditTextY, mEditTextX;
	private MainActivity mActivity;
	private BoardView mBoardView;
	private static final String TAG = MainActivity.class.getSimpleName();
	private Spinner mSpinner;
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

		ll.addView(mBoardView, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		
		mSpinner = (Spinner) findViewById(R.id.spinnerColour);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.colours_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				mBoardView.invalidate();
			}
	
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		mSpinner.setAdapter(adapter);
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
		mMenu = menu;
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
		} else if (id == R.id.action_play) {
			play();
			return true;
		} else if (id == R.id.action_load) {
			loadBoard();
			return true;
		} else if (id == R.id.action_send) {
			sendBoard();
			return true;
		} else if(id == R.id.action_settings) {
			loadSettings();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	private class GameRunnable implements Runnable {
		
		private boolean running = true;
		private void finish() {
			running = false;
		}
		@Override
		public void run() {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
			ArrayList<Integer> born = new ArrayList<Integer>(2);
			String bornString = prefs.getString("born", "3");
			for (int i = 0; i < bornString.length(); i++) {
				born.add(Integer.valueOf(bornString.charAt(i)-48));
			} 
			ArrayList<Integer> stays = new ArrayList<Integer>(2);
			String staysString = prefs.getString("stays", "23");
			for (int i = 0; i < staysString.length(); i++) {
				stays.add(Integer.valueOf(staysString.charAt(i)-48));
			} 
			Log.i(TAG,"Born:"+StringUtils.join(born,","));
			Log.i(TAG,"Stays:"+StringUtils.join(stays,","));
			while(running) {
				try {
					int [][] newCells = new int[mSize][mSize];
					int totalSum = 0;
					// The array should wrap around so that mBoard[1000][1001] refers to the top right cell
					for (int i = 0; i < mBoard.length; i++) {
						for (int j = 0; j < mBoard[i].length; j++) {
							
							int ip = (((i+1)%mBoard.length)+mBoard.length)%mBoard.length;
							int im = (((i-1)%mBoard.length)+mBoard.length)%mBoard.length;
							int jp = (((j+1)%mBoard[i].length)+mBoard[i].length)%mBoard[i].length;
							int jm = (((j-1)%mBoard[i].length)+mBoard[i].length)%mBoard[i].length;
							
							int[] neighbours = {
								mBoard[im][j],
								mBoard[ip][j],
								mBoard[i][jp],
								mBoard[i][jm],
								mBoard[ip][jp],
								mBoard[ip][jm],
								mBoard[im][jp],
								mBoard[im][jm]
							};
							//System.out.println(Arrays.toString(neighbours));
							int sum = 0;
							for(int b: neighbours) {
								sum += b==0?0:1;
							}
							totalSum += sum;
							if(mBoard[i][j] == 1){
								// Dealing with live cells
								newCells[i][j] = stays.contains(sum)?1:0;
							} else {
								newCells[i][j] = born.contains(sum)?1:0;
							}
						}
					}
					boolean stopped = true;
					for (int i = 0; i < newCells.length; i++) {
						stopped &= Arrays.equals(newCells[i], mBoard[i]);
					}
					if(totalSum == 0 || stopped) {
						Log.i(TAG,"All cells are dead or game has stabilised , stopping!");
						stopGame();
						playing = false;
					}
					mBoard = newCells;
					runOnUiThread(new Runnable() {
						public void run() {
							mBoardView.invalidate();
						}
					});
					
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	private void stopGame() {
		if(playRunnable != null) {
			playRunnable.finish();
		}
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				mMenu.findItem(R.id.action_play).setTitle("Start");
				mMenu.findItem(R.id.action_play).setIcon(R.drawable.ic_action_play);
			}
		});
		
	}
	private Menu mMenu;
	private boolean playing = false;
	private GameRunnable playRunnable = null;
	private void play() {
		if(playing) {
			playing = false;
			stopGame();
			
		} else {
			startGame();
		}
	}
	@Override
	protected void onResume() {
		super.onResume();
		if(playing) {
			startGame();
		} 
	}
	@Override
	protected void onPause() {
		super.onPause();
		if(playing) {
			stopGame();
		}
	}
	private void startGame() {
		if(playRunnable != null)
			playRunnable.finish();
		playing = true;
		playRunnable = new GameRunnable();
		(new Thread(playRunnable)).start();
		mMenu.findItem(R.id.action_play).setTitle("Stop");
		mMenu.findItem(R.id.action_play).setIcon(R.drawable.ic_action_stop);
	}

	private void loadSettings() {
		Intent i = new Intent(this,SettingsActivity.class);
		startActivity(i);
	}

	private int getEtVal(EditText et) {
		String text = et.getEditableText().toString();
		if(text.isEmpty())
			return 0;
		return Integer.parseInt(text);
	}
	private int getX() {
		return getEtVal(mEditTextX);
	}

	private int getY() {
		return getEtVal(mEditTextY);
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
				if(arraySum(mBoard)==0) {
					showToast("Nothing to send!");
					return;
				}
					
				PrintWriter out;
				try {
					int[][] smallestBoard = getSmallestBoard(mBoard);
					//Log.i(TAG,"Sending: "+ArrayUtils.toString(smallestBoard));

					StringBuilder sb = new StringBuilder();
					sb.append("x=").append(getX()).append(",y=")
							.append(getY()).append(",z=").append(getZ()).append(",board=");
					
					for (int i = 0; i < smallestBoard.length; i++) {
						sb.append(StringUtils.join(smallestBoard[i],',')).append("-");
					}
					sb.deleteCharAt(sb.length()-1);
					sb.append(".");
					Log.i(TAG,"Sending: "+sb.toString());
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
					String host = settings.getString("hostname", "ledsign");
					int port = Integer.valueOf(settings.getString("port", "12345"));
					Log.i(TAG,"Connecting to: "+host+":"+port);
					Socket sock = new Socket(host, port);
					out = new PrintWriter(sock.getOutputStream(), true);
					
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
	protected int getZ() {
		return mSpinner.getSelectedItemPosition();
	}

	private static int[] arrayColumn(final int[][] arr, final int col) {
		int[] column = new int[arr.length];
		for (int i = 0; i < arr.length; i++) {
			column[i] = arr[i][col];
		}
		return column;
	}
	private static int arraySum(int[][] board) {
		int sum =0;
		for (int[] is : board) {
			for (int i : is) {
				sum +=i;
			}
		}
		return sum;
	}
	private static int[][] getSmallestBoard(int[][] mBoard) {
		int maxX=0,minX=mBoard.length,maxY=0,minY=mBoard.length;
		for (int i = 0; i < mBoard.length; i++) {
			maxX = Math.max(maxX, lastNonZero(mBoard[i]));
			minX = Math.min(minX, firstNonZero(mBoard[i]));
			int[] column = arrayColumn(mBoard, i);
			maxY = Math.max(maxY, lastNonZero(column));
			minY = Math.min(minY, firstNonZero(column));
			
		}
		System.out.printf("maxX=%d,maxY=%d,minX=%d,minY=%d%n",maxX,maxY,minX,minY);
		int[][] out  = new int[maxY-minY+1][];
		for(int i = 0;i < maxY-minY+1; i++ ) {
			out[i] = Arrays.copyOfRange(mBoard[i+minY], minX, maxX+1);
		}
		return out;
	}

	private static int firstNonZero(int[] is) {
		for (int i = 0; i < is.length; i++) {
			if(is[i]!=0)
				return i;
		}
		return is.length;
	}
	private static int lastNonZero(int[] is) {
		for (int i = is.length-1; i > 0; i--) {
			if(is[i]!=0)
				return i;
		}
		return 0;
	}

	private void loadBoard() {
		showSaveLoadDialog(false);
	}

	private void saveBoard() {
		showSaveLoadDialog(true);
	}
	private static final String NEW_BOARD = "New board";
	private String[] getBoardTitles(final boolean save) {
		ArrayList<String> files = new ArrayList<String>();
		BoardReaderDbHelper mDbHelper = new BoardReaderDbHelper(mActivity);
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor c = db.query(
				BoardEntry.TABLE_NAME,
				new String[] { BoardEntry.COLUMN_NAME_TITLE },
				null,
				null, 
				null,
				null, 
				BoardEntry.COLUMN_NAME_TITLE + " DESC");
		Log.i(TAG,"Rows"+(c.getCount()));
		while (c!= null && c.moveToNext()) {
			files.add(c.getString(0));
		}
		if (save) {
			files.add(NEW_BOARD);
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
						if (fname.equals(NEW_BOARD) && save) {
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
					if (fname.equals(NEW_BOARD)) {
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
		Cursor c = db.query(BoardEntry.TABLE_NAME,
				new String[] { BoardEntry.COLUMN_NAME_CONTENT },
				BoardEntry.COLUMN_NAME_TITLE+" = ?", 
				new String[] { fname },
				null,
				null, 
				BoardEntry.COLUMN_NAME_TITLE + " DESC");
		c.moveToFirst();
		String boardString = c.getString(0);
		try {
			mBoard = deserializeBoard(boardString);
			mBoardView.invalidate();
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
		values.put(BoardEntry.COLUMN_NAME_TITLE, fname);
		try {
			values.put(BoardEntry.COLUMN_NAME_CONTENT, serializeBoard());
			long rowId = db.insert(BoardEntry.TABLE_NAME, "null", values);
			showToast("Saved board " + fname);
		} catch (JSONException e) {
			showToast("Could not save board " + fname);
			e.printStackTrace();
		}

	}

	private static int[][] deserializeBoard(String b) throws JSONException {
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
		private int[] colors = {
				Color.parseColor("#F44336"),
				Color.parseColor("#4CAF50"),
				Color.parseColor("#2196F3"),
				Color.parseColor("#607D8B")
				};
		@Override
		protected void onDraw(Canvas canvas) {
			int color = colors[mActivity.getZ()];
			for (int x = 0; x < mBoard.length; x++) {
				for (int y = 0; y < mBoard[x].length; y++) {
					int cellWidth = getWidth()/mSize;
					int cellHeight= getHeight()/mSize;
					int left = x*cellWidth;
					int top = y*cellHeight;
					int right= left + cellWidth;
					int bottom = top + cellHeight;
					if (mBoard[x][y] == 1) {
						
						mPaint.setColor(color);
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
					mPaint.setColor(colors[3]);
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
