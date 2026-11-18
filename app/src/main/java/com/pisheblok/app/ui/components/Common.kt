package com.pisheblok.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pisheblok.app.AppContainer
import com.pisheblok.app.R
import com.pisheblok.app.data.model.ExpiryState
import com.pisheblok.app.data.model.ItemCategory
import com.pisheblok.app.data.model.QuantityUnit
import com.pisheblok.app.data.model.formatQty
import com.pisheblok.app.ui.motion.components.ExpiryRing
import com.pisheblok.app.ui.motion.components.JellyScale
import com.pisheblok.app.ui.motion.pressScale
import com.pisheblok.app.ui.motion.rememberPressSource
import com.pisheblok.app.ui.theme.Blok
import com.pisheblok.app.ui.theme.LocalDarkTheme
import com.pisheblok.app.ui.theme.blokCardShadow
import com.pisheblok.app.ui.theme.blokTabular
import com.pisheblok.app.ui.theme.blokTintBrush
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

val LocalAppContainer = androidx.compose.runtime.staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

// ---------- Библиотека эмодзи (статичные картинки, без анимаций) ----------

// ---------- Helpers ----------

fun parseQty(text: String): Double? =
    text.replace(',', '.').trim().toDoubleOrNull()?.takeIf { it >= 0 }

/** Разобранное количество для списка покупок: число или текстовая заметка. */
data class BuyQtyInput(val qty: Double, val note: String?)

/**
 * Количество для списка покупок — свободный ввод: число («2», «1,5»),
 * любой текст («пару пачек») или пусто. Число > 0 превращается в [BuyQtyInput.qty],
 * всё остальное (включая пустую строку) — в [BuyQtyInput.note].
 */
fun parseBuyQtyInput(text: String): BuyQtyInput {
    val trimmed = text.trim()
    val parsed = parseQty(trimmed)
    return if (parsed != null && parsed > 0.0) BuyQtyInput(parsed, null)
    else BuyQtyInput(0.0, trimmed.ifBlank { null })
}

fun expiryStateOf(expiry: Long?, today: LocalDate = LocalDate.now()): ExpiryState {
    if (expiry == null) return ExpiryState.None
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(expiry).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0 -> ExpiryState.Expired
        days <= 3 -> ExpiryState.Soon
        else -> ExpiryState.Ok(days)
    }
}

fun formatDate(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}

@Composable
fun QuantityUnit.label(): String = when (this) {
    QuantityUnit.PCS -> stringResource(R.string.unit_pcs)
    QuantityUnit.PACK -> stringResource(R.string.unit_pack)
    QuantityUnit.BOTTLE -> stringResource(R.string.unit_bottle)
    QuantityUnit.BOX -> stringResource(R.string.unit_box)
    QuantityUnit.KG -> stringResource(R.string.unit_kg)
    QuantityUnit.G -> stringResource(R.string.unit_g)
    QuantityUnit.L -> stringResource(R.string.unit_l)
    QuantityUnit.ML -> stringResource(R.string.unit_ml)
}

@Composable
fun ItemCategory.label(): String = when (this) {
    ItemCategory.VEG -> stringResource(R.string.cat_veg)
    ItemCategory.MEAT -> stringResource(R.string.cat_meat)
    ItemCategory.DAIRY -> stringResource(R.string.cat_dairy)
    ItemCategory.FROZEN -> stringResource(R.string.cat_frozen)
    ItemCategory.GRAINS -> stringResource(R.string.cat_grains)
    ItemCategory.CANNED -> stringResource(R.string.cat_canned)
    ItemCategory.DRINKS -> stringResource(R.string.cat_drinks)
    ItemCategory.SPICES -> stringResource(R.string.cat_spices)
    ItemCategory.SWEETS -> stringResource(R.string.cat_sweets)
    ItemCategory.SNACKS -> stringResource(R.string.cat_snacks)
    ItemCategory.DISH -> stringResource(R.string.cat_dish)
    ItemCategory.CLEAN -> stringResource(R.string.cat_clean)
    ItemCategory.LAUNDRY -> stringResource(R.string.cat_laundry)
    ItemCategory.HYGIENE -> stringResource(R.string.cat_hygiene)
    ItemCategory.CARE -> stringResource(R.string.cat_care)
    ItemCategory.OTHER -> stringResource(R.string.cat_other)
}

// ---------- Small chips & badges ----------

@Composable
fun SmallChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    // Пилюля — как бейджи референса («−40%» на тарифной карточке).
    Surface(
        modifier = modifier,
        shape = Blok.pill,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ExpiryBadge(expiry: Long?, modifier: Modifier = Modifier) {
    // Тикающий «сегодня»: смена дня рекомпозирует только бейджи, не списки.
    val today by ExpiryTicker.today.collectAsStateWithLifecycle()
    val state = remember(expiry, today) { expiryStateOf(expiry, today) }
    when (state) {
        is ExpiryState.None -> {}
        is ExpiryState.Expired -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier,
        ) {
            ExpiryRing(
                progress = 1f,
                color = MaterialTheme.colorScheme.error,
                track = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
            )
            SmallChip(
                stringResource(R.string.expired_badge),
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        is ExpiryState.Soon -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier,
        ) {
            ExpiryRing(
                progress = 0.85f,
                color = MaterialTheme.colorScheme.tertiary,
                track = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
            )
            SmallChip(
                stringResource(R.string.expires_soon_badge),
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        is ExpiryState.Ok -> {
            // Доля «сгоревшего» 30-дневного окна — кольцо догорает к сроку.
            val burn = (1f - state.daysLeft.coerceAtMost(30) / 30f).coerceIn(0.15f, 0.8f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = modifier,
            ) {
                ExpiryRing(
                    progress = burn,
                    color = MaterialTheme.colorScheme.secondary,
                    track = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                )
                SmallChip(
                    stringResource(R.string.expires_days_fmt, state.daysLeft),
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

// ---------- Библиотека эмодзи ----------

/**
 * Библиотека эмодзи приложения (~118 шт.). Рендерятся готовыми картинками
 * Twemoji (CC-BY 4.0) из ресурсов — одинаковый плоский вид на всех
 * устройствах, без анимаций.
 */
val EMOJI_LIBRARY: List<String> = listOf(
    "🍎", "🥖", "🍞", "🥕", "🍅", "🧺", "📦", "🧊", "🍋", "🍇", "🍏", "🥔", "🥬", "🫐",
    "🍊", "🎃", "🌽", "🥦", "🍌", "🍉",
    "🧹", "🧽", "🪥", "🧅", "🧄", "🍄", "🔧", "🖌️", "🪒", "🧼", "🥒", "🫑", "🍆", "🥄",
    "🪝", "🔑", "🧷", "🥁",
    "🧀", "🍫", "🍩", "🥐", "🍪", "🥜", "🍯", "🍰", "🧁", "🍬", "🍭", "🥞", "🧇", "🍓",
    "🍒", "🥟", "🍜", "🥯", "🍡",
    "🥫", "🧃", "🥤", "🪣", "🧯", "♻️", "🥛", "☕", "🍵", "🥣", "🍺", "🥃", "🧉", "🫙",
    "🧂", "🪙", "🥮",
    "🫧", "💧", "🍶", "🚿", "🛁", "🧴", "🫗", "🚰", "🌊", "☁️", "🪶", "🎈", "🕊️", "🎐",
    "🪁", "🏠",
    "🚽", "🧻", "🔌", "🪛", "🍽", "🔔", "⏰", "🧲", "🪫", "🔋", "📼", "🪞", "🍳", "🚪",
    "📚", "🥩", "💊", "🛏", "🧸", "🪑", "💻", "📺", "🎮", "🪴", "🪜", "🚗", "🍷",
    "🥚",
    "🌭",
    "🍖",
    "🎁",
    "📓",
    "🖇",
    "⌨️",
    "🍟",
    "🧾",
    "📋",
    "📖",
    "🖊",
    "🎻",
    "🌷",
    "🥀",
    "🌱",
    "🌵",
    "🌾",
    "🛍",
    "🍝",
    "🍣",
    "🍤",
    "🍙",
    "🍹",
    "🥙",
    "🥪",
    "🌮",
    "🌯",
    "🥨",
    "🍿",
    "🍧",
    "🍨",
    "🧋",
    "🫖",
    "🐟",
    "🍱",
    "🍲",
    "🍛",
    "🥡",
    "🍗",
    "🍾",
    "🥂",
    "🍑",
    "🥝",
    "🍍",
    "🥭",
    "🌻",
    "❄️",
    "fridge",
)

/** Совместимое имя для вызовов из экранов. */
val ICONS_ANIMATED: List<String> = EMOJI_LIBRARY

/** Ключ нестандартной иконки «холодильник» (рисунка нет в Unicode). */
const val FRIDGE_ICON = "fridge"

/** Пустая иконка: место/секция без картинки. */
const val NO_ICON = ""

/** Группа иконок в пикере: подпись + набор. */
data class IconGroup(val labelRes: Int, val emojis: List<String>)

/**
 * Каталог иконок по назначению — вместо одной простыни из ~110 штук.
 * Наборы ссылаются на [EMOJI_LIBRARY]; «Все» в пикере показывает её целиком.
 */
val ICON_GROUPS: List<IconGroup> = listOf(
    IconGroup(
        R.string.icon_cat_food,
        listOf(
            "🍎", "🍏", "🍋", "🍊", "🍌", "🍉", "🍇", "🍓", "🍒", "🍑", "🥝", "🍍", "🥭",
            "🥕", "🍅", "🥔", "🥬", "🫐", "🎃", "🌽", "🥦", "🥒", "🫑", "🍆", "🧅", "🧄",
            "🍄", "🥚", "🥩", "🍗", "🌭", "🍖", "🐟", "🥛", "🧀", "🥐", "🍞", "🥖", "🥯",
            "🍪", "🍫", "🍩", "🍰", "🧁", "🍬", "🍭", "🥞", "🧇", "🍯", "🥜", "🍿", "🍧",
            "🍨", "🥟", "🍜", "🍝", "🍣", "🍤", "🍙", "🍱", "🍲", "🍛", "🥡", "🥙", "🥪",
            "🌮", "🌯", "🥨", "🍟", "🥮", "🍡", "🥫", "🍷", "🍺", "🥃", "🍹", "🍾", "🥂",
        ),
    ),
    IconGroup(
        R.string.icon_cat_kitchen,
        listOf(
            "🍳", "🥄", "🍽", "🧺", "🧊", "☕", "🍵", "🥣", "🧉",
            "🥤", "🧃", "🍶", "🫖", "🫙", "🧂", "🪙", "🧯", "🪣", "♻️", "🧴", "💧", "🫗",
            "🚰", "🥁",
        ),
    ),
    IconGroup(
        R.string.icon_cat_house,
        listOf(
            "🧹", "🧽", "🧼", "🪥", "🪒", "🫧", "🧻", "🚿", "🛁", "🚽", "🔌", "🪛", "🔧",
            "🖌️", "🧲", "🪫", "🔋", "📼", "🔔", "⏰", "📓", "🧾", "📋", "📖", "🖊", "🖇",
            "⌨️", "📚", "🛍", "🎁", "💊", "🪶", "🎈", "🕊️", "🎐", "🪝", "🔑", "🧷",
        ),
    ),
    IconGroup(
        R.string.icon_cat_rooms,
        listOf(
            "🏠", "🚪", "🪞", "🛏", "🪑", "🪴", "🪜", "🚗", "💻", "📺", "🎮", "🧸", "🎻",
            "🌷", "🥀", "🌱", "🌵", "🌾", "🌻", "☁️", "🌊", "🪁", "❄️", FRIDGE_ICON,
        ),
    ),
)

/**
 * Перенос старых статичных эмодзи (до библиотеки) на ближайшие
 * из библиотеки. Эмодзи, добавленные в библиотеку напрямую (🍳 и др.),
 * в миграции не нуждаются — они уже входят в набор.
 */
val OLD_ICON_MAP: Map<String, String> = mapOf(
    "❄️" to "🧊", "🥶" to "🧊",
    "🌿" to "🪴", "🛋" to "🛏", "🪟" to "🚪", "🧵" to "🧷", "🛠" to "🔧", "🖼" to "🪞",
    "🥡" to "📦", "🗄️" to "📦", "🧰" to "🔧",
    "🏢" to "🏠", "🏡" to "🏠", "🏬" to "🏠", "🏭" to "🏠", "🏫" to "🏠",
    "🏥" to "🏠", "🏨" to "🏠", "🏛" to "🏠", "🗼" to "🏠", "⛺" to "🏠",
    "🛖" to "🏠", "🏰" to "🏠", "🛗" to "🚪",
)

/**
 * Эмодзи-картинка из библиотеки (Twemoji-ресурс); при отсутствии ресурса —
 * системный шрифт. Статичная, без анимаций.
 */
@Composable
private fun EmojiArt(emoji: String, fontSize: TextUnit) {
    val resId = EMOJI_DRAWABLES[emoji]
    if (resId != null) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            modifier = Modifier.size(fontSize.value.dp),
        )
    } else {
        Text(emoji, fontSize = fontSize)
    }
}

/**
 * Эмодзи-глиф для карточек: плоская картинка, при желании — на подложке.
 */
@Composable
fun EmojiGlyph(
    emoji: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 64.sp,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        EmojiArt(emoji, fontSize)
    }
}

// ---------- Location card (grid) ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocationCard(
    icon: String,
    name: String,
    itemCount: Int,
    lowCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    height: Dp? = null,
) {
    val src = rememberPressSource()
    val dark = LocalDarkTheme.current
    // Контраст к фону: тонкая рамка — чтобы сетка читалась в обеих темах.
    val borderColor = if (dark) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.outlineVariant
    // Подпись внизу не ложится на иллюстрацию: мягкий скрим слева
    // и мягкая подложка только под текстовой зоной.
    val scrimBase = if (dark) MaterialTheme.colorScheme.background
    else MaterialTheme.colorScheme.surface
    Card(
        modifier = modifier
            .then(if (height != null) Modifier.height(height) else Modifier.aspectRatio(0.92f))
            .pressScale(src)
            .blokCardShadow(Blok.card)
            .combinedClickable(
                interactionSource = src,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = Blok.card,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, borderColor),
    ) {
        // Tutorial-карточка: label сверху, жирный заголовок, мета снизу;
        // «персонаж»-эмодзи выполняет за правый нижний край, текст — поверх.
        Box(
            Modifier
                .fillMaxSize()
                .clip(Blok.card)
                .background(blokTintBrush(), Blok.card)
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 6.dp)
            ) {
                EmojiGlyph(emoji = icon, fontSize = 122.sp)
            }
            // Скрим лежит ПОВЕРХ глифа: виньетка из левого нижнего угла.
            // Мягкие края — без плашки и шва, подпись читается даже на белой
            // иллюстрации, правый край глифа остаётся чистым.
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val scrimCenter = Offset(size.width * 0.10f, size.height * 1.02f)
                        drawRect(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to scrimBase.copy(alpha = 0.95f),
                                    0.45f to scrimBase.copy(alpha = 0.80f),
                                    0.78f to scrimBase.copy(alpha = 0.30f),
                                    1.00f to Color.Transparent,
                                ),
                                center = scrimCenter,
                                radius = size.width * 1.02f,
                            )
                        )
                    }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (lowCount > 0) {
                        Spacer(Modifier.width(Blok.Space.s))
                        SmallChip(
                            pluralStringResource(R.plurals.low_inside, lowCount, lowCount),
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(Modifier.height(Blok.Space.xs))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (itemCount == 0) stringResource(R.string.items_count_zero)
                    else pluralStringResource(R.plurals.items_count, itemCount, itemCount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.62f),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp? = null,
) {
    val src = rememberPressSource()
    Card(
        modifier = modifier
            .then(
                if (height != null) Modifier.height(height)
                else Modifier.aspectRatio(0.92f)
            )
            .pressScale(src)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        shape = Blok.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Blok.Space.s),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Компактное добавление вложенной секции: строка у заголовка «Продукты»,
 * когда большая плитка съедала бы место до первого товара.
 */
@Composable
fun CompactAddButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val src = rememberPressSource()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(Blok.pill)
            .pressScale(src)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------- Item row ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    name: String,
    qty: Double,
    unit: QuantityUnit,
    category: ItemCategory,
    expiry: Long?,
    isLow: Boolean,
    locationPath: String?,
    onBuy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    deleted: Boolean = false,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .blokCardShadow(Blok.cardSmall),
        shape = Blok.cardSmall,
        colors = CardDefaults.cardColors(
            containerColor = if (deleted) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        val rowSrc = rememberPressSource()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(rowSrc)
                .combinedClickable(
                    interactionSource = rowSrc,
                    indication = null,
                    onClick = onEdit,
                )
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                // Мета-строка сверху — как «дата» на карточках референса.
                Text(
                    category.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (deleted) {
                        Spacer(Modifier.width(8.dp))
                        SmallChip(
                            stringResource(R.string.search_deleted_badge),
                            MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isLow) {
                        Spacer(Modifier.width(8.dp))
                        SmallChip(
                            stringResource(R.string.low_stock_badge),
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "${formatQty(qty)} ${unit.label()}",
                        style = blokTabular(MaterialTheme.typography.titleMedium),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (locationPath != null) {
                        Text(
                            locationPath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpiryBadge(expiry)
                }
            }
            if (!deleted) {
                // Действия строки живут в «…»: корзина в каждой строке весила
                // больше самого товара и ловила промахи пальцем.
                Box {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.row_actions),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_list)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onBuy()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.delete),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}

// ---------- Верхняя сводка ----------

/** Фильтр сводки: истекает / мало. «Купить» уводит на вкладку списка. */
enum class SummaryFilter { EXPIRED, LOW }

/**
 * Карточка-сводка со строками: цветной кружок + подпись + жирный счётчик.
 * Обычная карточка темы (surfaceContainerLow) — как соседние карточки.
 * Клик по строке — прежние фильтры; активный фильтр подсвечивается
 * primaryContainer-фоном строки.
 */
@Composable
fun SummaryStrip(
    expiredCount: Int,
    lowCount: Int,
    toBuyCount: Int,
    selected: SummaryFilter?,
    onExpired: () -> Unit,
    onLow: () -> Unit,
    onToBuy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Blok.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Blok.Space.m),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SummaryRow(
                count = expiredCount,
                label = stringResource(R.string.summary_expired),
                icon = Icons.Rounded.Schedule,
                circle = MaterialTheme.colorScheme.error,
                onCircle = MaterialTheme.colorScheme.onError,
                selected = selected == SummaryFilter.EXPIRED,
                onClick = onExpired,
            )
            SummaryRow(
                count = lowCount,
                label = stringResource(R.string.summary_low),
                icon = Icons.Rounded.Inventory2,
                circle = MaterialTheme.colorScheme.tertiary,
                onCircle = MaterialTheme.colorScheme.onTertiary,
                selected = selected == SummaryFilter.LOW,
                onClick = onLow,
            )
            SummaryRow(
                count = toBuyCount,
                label = stringResource(R.string.summary_buy),
                icon = Icons.Rounded.ShoppingCart,
                circle = MaterialTheme.colorScheme.secondary,
                onCircle = MaterialTheme.colorScheme.onSecondary,
                selected = false,
                onClick = onToBuy,
            )
        }
    }
}

@Composable
private fun SummaryRow(
    count: Int,
    label: String,
    icon: ImageVector,
    circle: Color,
    onCircle: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val active = count > 0
    val rowBg = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        active -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> Color.Transparent
    }
    val labelColor = when {
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        active -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(Blok.cardSmall)
            .background(rowBg)
            .clickable(enabled = active || selected, onClick = onClick)
            .padding(horizontal = Blok.Space.m, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        if (active) circle else MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (active) onCircle else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        Spacer(Modifier.width(Blok.Space.m))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(Blok.Space.s))
        Text(
            count.toString(),
            style = blokTabular(MaterialTheme.typography.titleLarge),
            fontWeight = FontWeight.ExtraBold,
            color = labelColor,
        )
    }
}

// ---------- Dialogs ----------

/** Urgent / later selector chips for shopping entries. */
@Composable
fun PriorityChips(
    urgent: Boolean,
    onUrgentChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.material3.FilterChip(
            selected = urgent,
            onClick = { onUrgentChange(true) },
            label = { Text(stringResource(R.string.shopping_urgent)) },
        )
        androidx.compose.material3.FilterChip(
            selected = !urgent,
            onClick = { onUrgentChange(false) },
            label = { Text(stringResource(R.string.shopping_later)) },
        )
    }
}

/** "How many to buy?" dialog with a quantity field + jelly scale. */
@Composable
fun QtyDialog(
    title: String,
    subtitle: String,
    unit: QuantityUnit,
    confirmText: String,
    onConfirm: (BuyQtyInput) -> Unit,
    onDismiss: () -> Unit,
    urgent: Boolean = true,
    onUrgentChange: (Boolean) -> Unit = {},
) {
    // Поле пустое изначально: количество необязательно — можно число,
    // текст («пару пачек») или оставить пустым.
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = parseQty(text)

    // Пределы желе-шкалы — от единицы измерения.
    val (scaleMax, scaleStep) = when (unit) {
        QuantityUnit.PCS, QuantityUnit.PACK, QuantityUnit.BOTTLE, QuantityUnit.BOX -> 12f to 1f
        QuantityUnit.KG, QuantityUnit.L -> 10f to 0.5f
        QuantityUnit.G, QuantityUnit.ML -> 2000f to 50f
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.qty_or_note)) },
                    // Единица показывается только при числовом вводе —
                    // к текстовой заметке «шт» не клеится.
                    suffix = { if (parsed != null) Text(unit.label()) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qty_field"),
                )
                JellyScale(
                    value = parsed?.toFloat(),
                    onValueChange = { text = formatQty(it.toDouble()) },
                    min = 0f,
                    max = scaleMax,
                    step = scaleStep,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(10.dp))
                PriorityChips(urgent = urgent, onUrgentChange = onUrgentChange)
            }
        },
        confirmButton = {
            // Пустое количество разрешено — добавляем и без него.
            TextButton(onClick = { onConfirm(parseBuyQtyInput(text)) }) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// ---------- Pickers ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmojiPicker(
    emojis: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Каталог разбит по назначению: еда, кухня, быт, комнаты + «Все».
    var groupIndex by rememberSaveable { mutableStateOf(0) }
    val groups = remember(emojis) {
        val allowed = emojis.toSet()
        buildList {
            add(IconGroup(R.string.icon_cat_all, emojis))
            ICON_GROUPS.forEach { g ->
                val part = g.emojis.filter { it in allowed }
                if (part.isNotEmpty()) add(IconGroup(g.labelRes, part))
            }
        }
    }
    val shown = groups.getOrNull(groupIndex)?.emojis ?: emojis

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            groups.forEachIndexed { index, group ->
                androidx.compose.material3.FilterChip(
                    selected = groupIndex == index,
                    onClick = { groupIndex = index },
                    label = { Text(stringResource(group.labelRes), maxLines = 1) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 232.dp)
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // «Без иконки» — первым и всегда: утилитарный выбор важнее набора.
            val noneSelected = selected.isBlank()
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (noneSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.clickable { onSelect(NO_ICON) },
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Block,
                        contentDescription = stringResource(R.string.icon_none),
                        tint = if (noneSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            shown.forEach { emoji ->
                val isSelected = emoji == selected
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clickable { onSelect(emoji) },
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmojiArt(emoji, 24.sp)
                    }
                }
            }
        }
    }
}

/** Generic dropdown field used for unit & category selection. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = MaterialTheme.shapes.small,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ---------- Empty state ----------

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Те же картинки, что на карточках: системный эмодзи ломал единый вид.
        EmojiGlyph(emoji = emoji, fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
