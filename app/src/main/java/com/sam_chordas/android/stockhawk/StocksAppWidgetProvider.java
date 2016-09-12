package com.sam_chordas.android.stockhawk;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.service.StocksWidgetService;

public class StocksAppWidgetProvider extends AppWidgetProvider{

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for(int i = 0; i < appWidgetIds.length; i++){
            int currentAppWidgetId = appWidgetIds[i];

            RemoteViews remoteViews;

            remoteViews = new RemoteViews(context.getPackageName(), R.layout.stocks_app_widget);

            Intent intent = new Intent(context, StocksWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            remoteViews.setRemoteAdapter(R.id.list_view, intent);

            remoteViews.setEmptyView(R.id.list_view, R.id.empty_view);

            appWidgetManager.updateAppWidget(currentAppWidgetId, remoteViews);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
