package com.pisheblok.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pisheblok.app.MainActivity
import com.pisheblok.app.PisheBlokApp
import com.pisheblok.app.R
import com.pisheblok.app.data.local.ItemEntity
import com.pisheblok.app.data.local.ShoppingEntity
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class Stats(
    val expired: Int,
    val expiringSoon: Int,
    val expiredSoon: Int,
    val low: Int,
    val toBuy: Int,
)

fun computeStats(items: List<ItemEntity>, shopping: List<ShoppingEntity>, zone: ZoneId, today: LocalDate): Stats {
    var expired = 0
    var expiringSoon = 0
    var low = 0
    for (item in items) {
        if (item.isDeleted) continue
        item.expiryDate?.let { exp ->
            val days = ChronoUnit.DAYS.between(today, Instant.ofEpochMilli(exp).atZone(zone).toLocalDate())
            when {
                days < 0 -> expired++
                days <= 3 -> expiringSoon++
                else -> {}
            }
        }
        if (item.qty <= item.minQty) low++
    }
    val toBuy = shopping.count { !it.isDeleted && !it.done }
    return Stats(expired, expiringSoon, expired + expiringSoon, low, toBuy)
}

suspend fun refreshWidget(context: Context) {
    runCatching { PisheBlokWidget().updateAll(context) }
}

class PisheBlokWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = PisheBlokWidget()
}

class PisheBlokWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as PisheBlokApp).container.repository
        val items = repo.observeAllItems().first()
        val shopping = repo.observeShopping().first()
        val stats = computeStats(items, shopping, ZoneId.systemDefault(), LocalDate.now())

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stats.expiredSoon.toString(),
                                style = TextStyle(color = ColorProvider(Color(0xFFBA1A1A)), fontSize = 22.sp, fontWeight = FontWeight.Bold),
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = context.getString(R.string.widget_expired),
                                style = TextStyle(color = ColorProvider(Color(0xFF53433E)), fontSize = 11.sp),
                                maxLines = 1,
                            )
                        }
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stats.low.toString(),
                                style = TextStyle(color = ColorProvider(Color(0xFF9A6700)), fontSize = 22.sp, fontWeight = FontWeight.Bold),
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = context.getString(R.string.widget_low),
                                style = TextStyle(color = ColorProvider(Color(0xFF53433E)), fontSize = 11.sp),
                                maxLines = 1,
                            )
                        }
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stats.toBuy.toString(),
                                style = TextStyle(color = ColorProvider(Color(0xFF4F6440)), fontSize = 22.sp, fontWeight = FontWeight.Bold),
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = context.getString(R.string.widget_buy),
                                style = TextStyle(color = ColorProvider(Color(0xFF53433E)), fontSize = 11.sp),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
