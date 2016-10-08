package com.sam_chordas.android.stockhawk;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.service.StocksWidgetService;
import com.sam_chordas.android.stockhawk.ui.StockChartActivity;

public class StocksAppWidgetProvider extends AppWidgetProvider{
    public static final String SHOW_STOCK_DETAILS_ACTION = "com.sam_chordas.android.stockhawk.SHOW_STOCK_DETAILS";
    public static final String EXTRA_STOCK_SYMBOL = "com.sam_chordas.android.stockhawk.EXTRA_STOCK_SYMBOL";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SHOW_STOCK_DETAILS_ACTION)) {
            String stockSymbol = intent.getStringExtra(EXTRA_STOCK_SYMBOL);

            Intent startActivityIntent;

            startActivityIntent = new Intent(context, StockChartActivity.class);
            startActivityIntent.putExtra("STOCK_SYMBOL",stockSymbol);
            context.startActivity(startActivityIntent);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int currentAppWidgetId : appWidgetIds) {
            RemoteViews remoteViews;

            remoteViews = new RemoteViews(context.getPackageName(), R.layout.stocks_app_widget);

            Intent intent = new Intent(context, StocksWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, currentAppWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            remoteViews.setRemoteAdapter(R.id.list_view, intent);
            remoteViews.setEmptyView(R.id.list_view, R.id.empty_view);

            Intent showStockDetailsIntent = new Intent(context, StocksAppWidgetProvider.class);
            showStockDetailsIntent.setAction(SHOW_STOCK_DETAILS_ACTION);
            showStockDetailsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, currentAppWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            PendingIntent showStockDetailsPendingIntent =
                    PendingIntent.getBroadcast(context, 0, showStockDetailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setPendingIntentTemplate(R.id.list_view, showStockDetailsPendingIntent);


            appWidgetManager.updateAppWidget(currentAppWidgetId, remoteViews);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
