package com.sam_chordas.android.stockhawk.service;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.StocksAppWidgetProvider;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

public class StocksWidgetService extends RemoteViewsService{
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {

        return new StockRemoteViewsFactory(this.getApplicationContext());
    }

    private class StockRemoteViewsFactory implements RemoteViewsFactory{

        private Context mContext;
        private Cursor mCursor;

        StockRemoteViewsFactory(Context context){
            mContext = context;

            mCursor = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);
        }


        @Override
        public void onCreate() {

        }

        @Override
        public void onDataSetChanged() {
        }

        @Override
        public void onDestroy() {

        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews remoteView = new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);

            mCursor.moveToPosition(position);

            remoteView.setTextViewText(R.id.stock_symbol, mCursor.getString(mCursor.getColumnIndex("symbol")));
            remoteView.setTextViewText(R.id.bid_price, mCursor.getString(mCursor.getColumnIndex("bid_price")));
            if (Utils.showPercent){
                remoteView.setTextViewText(R.id.change, mCursor.getString(mCursor.getColumnIndex("percent_change")));
            } else{
                remoteView.setTextViewText(R.id.change, mCursor.getString(mCursor.getColumnIndex("change")));
            }

            /** Solution for changing the background based on http://stackoverflow.com/questions/6333774/change-remoteview-imageview-background */
            if(mCursor.getInt(mCursor.getColumnIndex("is_up")) == 1){
                remoteView.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
            } else {
                remoteView.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
            }

            Bundle extras = new Bundle();
            extras.putString(StocksAppWidgetProvider.EXTRA_STOCK_SYMBOL, mCursor.getString(mCursor.getColumnIndex("symbol")));

            Intent fillIntent = new Intent();
            fillIntent.putExtras(extras);
            remoteView.setOnClickFillInIntent(R.id.list_item_quote, fillIntent);

            return remoteView;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
