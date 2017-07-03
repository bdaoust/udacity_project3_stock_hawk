package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence mTitle;
  private Intent mServiceIntent;
  private ItemTouchHelper mItemTouchHelper;
  private static final int CURSOR_LOADER_ID = 0;
  private QuoteCursorAdapter mCursorAdapter;
  private Context mContext;
  private ConnectivityManager mConnectivityManager;
  private boolean isConnected;
  private StockNotFoundBroadcastReceiver mStockNotFoundBroadcastReceiver;
  private ContentLoadingProgressBar mContentLoadingProgressBar;
  private TextView mLastUpdatedTextView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    mStockNotFoundBroadcastReceiver = new StockNotFoundBroadcastReceiver();
    mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

    isConnected = checkIsConnected(mConnectivityManager);
    setContentView(R.layout.activity_my_stocks);

    mContentLoadingProgressBar = (ContentLoadingProgressBar)findViewById(R.id.progress);
    mLastUpdatedTextView = (TextView)findViewById(R.id.last_updated);

    // The intent service is for executing immediate pulls from the Yahoo API
    // GCMTaskService can only schedule tasks, they cannot execute immediately
    mServiceIntent = new Intent(this, StockIntentService.class);
    if (savedInstanceState == null){
      // Run the initialize task service so that some stocks appear upon an empty database
      mServiceIntent.putExtra("tag", "init");
      if (isConnected){

        startService(mServiceIntent);
      } else{
        networkToast();
      }
    }
    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);



    mCursorAdapter = new QuoteCursorAdapter(this, null);
    recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
            new RecyclerViewItemClickListener.OnItemClickListener() {
              @Override public void onItemClick(View v, int position) {
                Intent intent;
                TextView stockTextView;
                String symbol;

                stockTextView = (TextView)v.findViewById(R.id.stock_symbol);
                symbol = stockTextView.getText().toString();

                intent = new Intent(mContext, StockChartActivity.class);
                intent.putExtra("STOCK_SYMBOL",symbol);
                startActivity(intent);
              }
            }));
    recyclerView.setAdapter(mCursorAdapter);


    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.attachToRecyclerView(recyclerView);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (isConnected){
          new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                  .content(R.string.symbol_search_content)
                  .backgroundColor(getResources().getColor(android.R.color.black))
                  .inputType(InputType.TYPE_CLASS_TEXT)
                  .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                @Override public void onInput(MaterialDialog dialog, CharSequence input) {
                  // On FAB click, receive user input. Make sure the stock doesn't already exist
                  // in the DB and proceed accordingly
                  Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                      new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                      new String[] { input.toString() }, null);
                  if (c.getCount() != 0) {
                    Toast toast =
                        Toast.makeText(MyStocksActivity.this, R.string.stock_is_already_saved_toast,
                            Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                    toast.show();
                  } else {
                    // Add the stock to DB
                    mServiceIntent.putExtra("tag", "add");
                    mServiceIntent.putExtra("symbol", input.toString());
                    startService(mServiceIntent);
                  }
                  c.close();
                }
              })
              .show();
        } else {
          networkToast();
        }

      }
    });

    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
    mItemTouchHelper = new ItemTouchHelper(callback);
    mItemTouchHelper.attachToRecyclerView(recyclerView);

    mTitle = getTitle();
    if (isConnected){
      long period = 3600L;
      long flex = 10L;
      String periodicTag = "periodic";

      // create a periodic task to pull stocks once every hour after the app has been opened. This
      // is so Widget data stays up to date.
      PeriodicTask periodicTask = new PeriodicTask.Builder()
          .setService(StockTaskService.class)
          .setPeriod(period)
          .setFlex(flex)
          .setTag(periodicTag)
          .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
          .setRequiresCharging(false)
          .build();
      // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
      // are updated.
      GcmNetworkManager.getInstance(this).schedule(periodicTask);
    }
  }


  @Override
  public void onResume() {
    super.onResume();
    IntentFilter intentFilter;

    intentFilter = new IntentFilter("com.sam_chordas.android.stockhawk.NOTIFY_STOCK_NOT_FOUND");
    registerReceiver(mStockNotFoundBroadcastReceiver, intentFilter);
    getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);

    isConnected = checkIsConnected(mConnectivityManager);
  }

  @Override
  protected void onPause() {
    unregisterReceiver(mStockNotFoundBroadcastReceiver);
    super.onPause();
  }

  public void networkToast(){
    Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.my_stocks, menu);
      restoreActionBar();
      return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    if (id == R.id.action_change_units){
      // this is for changing stock changes from percent value to dollar value
      Utils.showPercent = !Utils.showPercent;
      this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args){
    mContentLoadingProgressBar.show();
    // This narrows the return to only the stocks that are most current.
    return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
        new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
        QuoteColumns.ISCURRENT + " = ?",
        new String[]{"1"},
        null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data){
    mCursorAdapter.swapCursor(data);

    mContentLoadingProgressBar.hide();

    SharedPreferences preferences;

    preferences = getSharedPreferences("STOCK_HAWK_PREFS", MODE_PRIVATE);
    long stocksUpdatedTimestamp = preferences.getLong("stocksUpdatedTimestamp", 0);

    if(stocksUpdatedTimestamp > 0){
      mLastUpdatedTextView.setVisibility(View.VISIBLE);

      String lastUpdated;
      lastUpdated = getLastUpdated(System.currentTimeMillis() - stocksUpdatedTimestamp);
      mLastUpdatedTextView.setText(lastUpdated);

    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader){
    mCursorAdapter.swapCursor(null);
  }


  private class StockNotFoundBroadcastReceiver extends BroadcastReceiver{
    /** Custom BroadcastReceiver based on http://hmkcode.com/android-sending-receiving-custom-broadcasts/ **/
    @Override
    public void onReceive(Context context, Intent intent) {
      Toast toast;

      toast = Toast.makeText(mContext, R.string.stock_not_found_toast, Toast.LENGTH_SHORT);
      toast.show();
    }
  }

  private boolean checkIsConnected(ConnectivityManager connectivityManager){
    NetworkInfo networkInfo;

    networkInfo = connectivityManager.getActiveNetworkInfo();

    return networkInfo != null && networkInfo.isConnectedOrConnecting();
  }

  public String getLastUpdated(long elapsedTime){
    String lastUpdated;
    String days;
    String hours;
    String minutes;

    long one_minute_ms = 60*1000;
    long sixty_minutes_ms = 60*one_minute_ms;
    long twenty_four_hours_ms = 24*sixty_minutes_ms;
    long numb;

    days = getString(R.string.time_days);
    hours = getString(R.string.time_hours);
    minutes = getString(R.string.time_minutes);

    if(elapsedTime >= twenty_four_hours_ms){
      numb = elapsedTime/(twenty_four_hours_ms);

      lastUpdated = getString(R.string.last_updated, numb, days);
    } else if(elapsedTime >= sixty_minutes_ms){
      numb = elapsedTime/(sixty_minutes_ms);

      lastUpdated = getString(R.string.last_updated, numb, hours);
    } else {
      numb = elapsedTime/one_minute_ms;

      lastUpdated =  getString(R.string.last_updated, numb, minutes);
    }

    return lastUpdated;
  }
}
