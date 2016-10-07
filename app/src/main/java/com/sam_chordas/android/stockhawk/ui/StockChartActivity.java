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
import android.widget.TextView;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

public class StockChartActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final int CURSOR_LOADER_ID = 0;
    private String mSymbol;
    private TextView mStockName;
    private TextView mStockBidPrice;
    private TextView mStockBookValue;
    private TextView mStockDaysLow;
    private TextView mStockDaysHigh;
    private TextView mStockYearLow;
    private TextView mStockYearHigh;
    private TextView mStockMarketCap;
    private TextView mStockDividendYield;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Bundle extras;

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_line_graph);

        mStockName = (TextView) findViewById(R.id.stock_name);
        mStockBidPrice = (TextView) findViewById(R.id.stock_bid_price_value);
        mStockBookValue = (TextView) findViewById(R.id.stock_book_value);
        mStockDaysLow = (TextView) findViewById(R.id.stock_days_low_value);
        mStockDaysHigh = (TextView) findViewById(R.id.stock_days_high_value);
        mStockYearLow = (TextView) findViewById(R.id.stock_year_low_value);
        mStockYearHigh = (TextView) findViewById(R.id.stock_year_high_value);
        mStockMarketCap = (TextView) findViewById(R.id.stock_market_cap_value);
        mStockDividendYield = (TextView) findViewById(R.id.stock_dividend_yield_value);

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
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP,
                        QuoteColumns.NAME, QuoteColumns.BIDPRICE, QuoteColumns.BOOK_VALUE,
                        QuoteColumns.DAYS_LOW, QuoteColumns.DAYS_HIGH, QuoteColumns.YEAR_LOW,
                        QuoteColumns.YEAR_HIGH, QuoteColumns.MARKET_CAP, QuoteColumns.DIVIDEND_YIELD},
                QuoteColumns.SYMBOL + " = ?",
                new String[]{mSymbol},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data){
        float[] values;
        String[] labels;
        String stockNameValue;
        String stockBidPriceValue;
        String stockBookValue;
        String stockDaysLowValue;
        String stockDaysHighValue;
        String stockYearLowValue;
        String stockYearHighValue;
        String stockMarketCapValue;
        String stockDividendYieldValue;

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
        dataset.setColor(getResources().getColor(android.R.color.white));
        dataset.setThickness(3);
        Log.v("aa","The thickness is: " + dataset.getThickness());

        LineChartView lineChartView = (LineChartView)findViewById(R.id.linechart);
        lineChartView.addData(dataset);
        lineChartView.setAxisBorderValues(findMin(values), findMax(values));
        lineChartView.setAxisColor(getResources().getColor(android.R.color.white));
        lineChartView.setLabelsColor(getResources().getColor(android.R.color.white));

        lineChartView.show();

        data.moveToLast();

        stockNameValue = getStockValue(data, QuoteColumns.NAME) + " (" + getStockValue(data, QuoteColumns.SYMBOL) + ")";
        stockBidPriceValue = getStockValue(data, QuoteColumns.BIDPRICE);
        stockBookValue = getStockValue(data, QuoteColumns.BOOK_VALUE);
        stockDaysLowValue = getStockValue(data, QuoteColumns.DAYS_LOW);
        stockDaysHighValue = getStockValue(data, QuoteColumns.DAYS_HIGH);
        stockYearLowValue = getStockValue(data, QuoteColumns.YEAR_LOW);
        stockYearHighValue = getStockValue(data, QuoteColumns.YEAR_HIGH);
        stockMarketCapValue = getStockValue(data, QuoteColumns.MARKET_CAP);
        stockDividendYieldValue = getStockValue(data, QuoteColumns.DIVIDEND_YIELD);

        mStockName.setText(stockNameValue);
        mStockBidPrice.setText(stockBidPriceValue);
        mStockBookValue.setText(stockBookValue);
        mStockDaysLow.setText(stockDaysLowValue);
        mStockDaysHigh.setText(stockDaysHighValue);
        mStockYearLow.setText(stockYearLowValue);
        mStockYearHigh.setText(stockYearHighValue);
        mStockMarketCap.setText(stockMarketCapValue);
        mStockDividendYield.setText(stockDividendYieldValue);
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

    private String getStockValue(Cursor cursor, String columnName){
        String value;
        int columnIndex;

        columnIndex = cursor.getColumnIndex(columnName);
        value = cursor.getString(columnIndex);

        if(value.equals("null")){
            value = getResources().getString(R.string.stock_value_not_available);
        }

        return value;
    }
}
