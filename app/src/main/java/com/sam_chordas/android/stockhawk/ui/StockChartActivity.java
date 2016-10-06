package com.sam_chordas.android.stockhawk.ui;


import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

public class StockChartActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final int CURSOR_LOADER_ID = 0;
    private String mSymbol;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Bundle extras;

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_line_graph);

        extras = getIntent().getExtras();
        if(extras != null){
            mSymbol = extras.getString("STOCK_SYMBOL");
            Log.v("aaa", "The stock symbol is... : " + mSymbol);
        }
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args){
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.SYMBOL + " = ?",
                new String[]{mSymbol},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data){
        String[] labels;
        float[] values;

        labels = new String[data.getCount()];
        values = new float[data.getCount()];

        data.moveToFirst();
        for(int i = 0; i < data.getCount(); i++){
            data.moveToPosition(i);
            labels[i] = "";
            values[i] = data.getFloat(data.getColumnIndex(QuoteColumns.BIDPRICE));
        }
        Log.v("aaa", "Cursor count: " + data.getCount());


        LineSet dataset = new LineSet(labels, values);
        dataset.beginAt(0);
        dataset.endAt(dataset.size()-1);

        LineChartView lineChartView = (LineChartView)findViewById(R.id.linechart);
        lineChartView.addData(dataset);
        lineChartView.setAxisBorderValues(findMin(values), findMax(values));

        lineChartView.show();
        //mCursorAdapter.swapCursor(data);
        //mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
        //mCursorAdapter.swapCursor(null);
    }

    private int findMin(float[] values){
        float min;

        min = values[0];
        for(int i = 0; i < values.length; i++){
            if(values[i] < min){
                min = values[i];
            }
        }

        return (int)Math.floor(min);
    }

    private int findMax(float[] values){
        float max;

        max = values[0];
        for(int i = 0; i < values.length; i++){
            if(values[i] > max){
                max = values[i];
            }
        }

        return (int)Math.ceil(max);
    }
}
