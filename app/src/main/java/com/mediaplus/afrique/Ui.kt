package com.mediaplus.afrique

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val bg by animateColorAsState(
        targetValue = if (selected) c.primary else if (focused) c.surfaceVariant.copy(alpha = 0.95f) else c.surface,
        label = "tab-background"
    )
    Box(
        modifier.clip(shape).background(bg)
            .border(if (focused) 2.dp else 1.dp, if (focused) c.secondary else if (selected) c.primary else c.outline, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }.padding(vertical = 11.dp, horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (selected) c.onPrimary else c.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

@Composable
fun Header(showAccount: Boolean, expiryEpochSeconds: Long?, onAccount: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.Start) {
            Image(
                painter = painterResource(R.drawable.media_plus_brand),
                contentDescription = "Logo Media+ Côte d’Ivoire",
                modifier = Modifier.width(116.dp).height(82.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
            val days = expiryEpochSeconds?.let { expiry ->
                val remaining = expiry * 1000L - System.currentTimeMillis()
                if (remaining <= 0L) 0L else (remaining + 86_399_999L) / 86_400_000L
            }
            Text(
                when (days) {
                    null -> "Abonnement : date inconnue"
                    0L -> "Abonnement expiré"
                    1L -> "1 jour restant"
                    else -> "$days jours restants"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (days != null && days <= 7L) Color(0xFFB45309) else c.onSurfaceVariant,
                maxLines = 2,
                lineHeight = 17.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("Media+ Côte d’Ivoire 🇨🇮", Modifier.weight(1f), color = c.onSurface,
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
            maxLines = 1)
        if (false && showAccount) TextButton(onClick = onAccount) {
            Text("Compte", color = c.secondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Tile(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val bg by animateColorAsState(if (focused) Color(0xFFE8F3EA) else Color.White, label = "tile-background")
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(shape)
            .background(bg)
            .border(if (focused) 2.dp else 1.dp, if (focused) c.primary else c.outline, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun Logo(item: Item) {
    val ctx = LocalContext.current
    var ok by remember(item.logo) { mutableStateOf(false) }
    Box(Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0F7F0))) {
        if (!ok) Box(
            Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF08723A), Color(0xFFFF8A00)))),
            contentAlignment = Alignment.Center
        ) { Text(bouquetIcon(item.group), fontSize = 24.sp) }
        if (item.logo.startsWith("http")) {
            AsyncImage(
                model = remember(item.logo) { ImageRequest.Builder(ctx).data(item.logo).size(128).crossfade(true).build() },
                contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                onSuccess = { ok = true }
            )
        }
    }
}

private fun bouquetCountryFlag(name: String): String? {
    val normalized = java.text.Normalizer.normalize(name.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
        .replace(Regex("[\\u0300-\\u036f]"), "")
        .replace(Regex("[^a-z0-9]+"), " ").trim()
    val tokens = normalized.split(" ").filter { it.isNotBlank() }.toSet()
    val countries = listOf(
        "🇨🇮" to listOf("cote d ivoire", "cote ivoire", "ivory coast", "ivoirien", "civ"),
        "🇹🇳" to listOf("tunisie", "tunisia", "tunis", "tounes", "tn"),
        "🇩🇿" to listOf("algerie", "algeria", "alger", "dz"),
        "🇲🇦" to listOf("maroc", "morocco", "maghreb maroc", "ma"),
        "🇪🇬" to listOf("egypte", "egypt", "egyptian", "eg"),
        "🇸🇦" to listOf("arabie saoudite", "saudi", "saudia", "ksa"),
        "🇦🇪" to listOf("emirats", "emirates", "uae", "dubai", "abu dhabi"),
        "🇶🇦" to listOf("qatar", "qatari", "qa"),
        "🇰🇼" to listOf("koweit", "kuwait", "kw"),
        "🇧🇭" to listOf("bahrein", "bahrain"),
        "🇴🇲" to listOf("oman", "omani"),
        "🇯🇴" to listOf("jordanie", "jordan", "jo"),
        "🇱🇧" to listOf("liban", "lebanon", "lb"),
        "🇸🇾" to listOf("syrie", "syria", "sy"),
        "🇮🇶" to listOf("irak", "iraq", "iq"),
        "🇵🇸" to listOf("palestine", "palestinian", "ps"),
        "🇾🇪" to listOf("yemen", "ye"),
        "🇱🇾" to listOf("libye", "libya", "ly"),
        "🇸🇩" to listOf("soudan", "sudan", "sd"),
        "🇲🇷" to listOf("mauritanie", "mauritania", "mr"),
        "🇸🇴" to listOf("somalie", "somalia", "so"),
        "🇩🇯" to listOf("djibouti"),
        "🇰🇲" to listOf("comores", "comoros"),
        "🇫🇷" to listOf("france", "francais", "french"),
        "🇬🇧" to listOf("royaume uni", "united kingdom", "uk", "british"),
        "🇺🇸" to listOf("usa", "united states", "etats unis", "america"),
        "🇨🇦" to listOf("canada", "canadien"),
        "🇸🇳" to listOf("senegal", "sn"),
        "🇲🇱" to listOf("mali"),
        "🇨🇲" to listOf("cameroun", "cameroon"),
        "🇧🇫" to listOf("burkina", "burkina faso"),
        "🇬🇳" to listOf("guinee", "guinea"),
        "🇳🇬" to listOf("nigeria"),
        "🇬🇭" to listOf("ghana"),
        "🇨🇩" to listOf("rdc", "congo kinshasa", "republique democratique du congo"),
        "🇨🇬" to listOf("congo brazzaville")
    )
    return countries.firstOrNull { (_, aliases) ->
        aliases.any { alias -> if (alias.length <= 3) alias in tokens else (" $normalized ").contains(" $alias ") }
    }?.first
}

@Composable
fun GroupRow(name: String, count: Int, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Tile(onClick) {
        Box(
            Modifier.size(54.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF4F8F2))
                .border(1.dp, Color(0xFFDCE8D9), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val football = listOf("foot", "football", "soccer", "ligue", "champions league", "premier league", "sport").any { name.contains(it, ignoreCase = true) }
            val countryFlag = bouquetCountryFlag(name)
            if (countryFlag != null) {
                Text(countryFlag, fontSize = 34.sp)
            } else if (football) {
                Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                    Text("⚽", fontSize = 32.sp)
                }
            } else Text(bouquetIcon(name), fontSize = 26.sp)
        }
        Spacer(Modifier.width(14.dp))
        Text(name, Modifier.weight(1f), color = c.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("$count", color = c.onSurface, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(c.surfaceVariant).padding(horizontal = 11.dp, vertical = 5.dp))
        Text("›", color = c.primary, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
fun ItemRow(item: Item, fav: Boolean, onFav: () -> Unit, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Tile(onClick) {
        Logo(item)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, color = c.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.group.isNotEmpty()) Text(item.group, style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant, maxLines = 1)
        }
        Text(if (fav) "♥" else "♡", color = if (fav) c.tertiary else c.onSurfaceVariant, fontSize = 25.sp,
            modifier = Modifier.clickable { onFav() }.padding(8.dp))
    }
}

@Composable
fun ItemList(
    list: List<Item>, group: String?, query: String, favUrls: Set<String>, flat: Boolean,
    onGroup: (String) -> Unit, onPick: (Item) -> Unit, onFav: (Item) -> Unit, modifier: Modifier = Modifier,
    categories: List<String> = emptyList(), newestFirst: Boolean = false,
    specialGroups: Map<String, List<Item>> = emptyMap(),
    savedPositions: MutableMap<String, Pair<Int, Int>>? = null
) {
    val sorted = remember(list, newestFirst) {
        if (newestFirst) list.sortedWith(compareByDescending<Item> { it.modifiedAt }.thenBy { it.name.lowercase() }) else list
    }
    val groups = remember(sorted) { sorted.groupBy { it.bouquet() } }
    val allCategories = remember(groups, categories, specialGroups) { (specialGroups.keys + categories + groups.keys).distinct() }
    val found = remember(sorted, query, flat) {
        if (query.isNotBlank()) sorted.filter { it.name.contains(query, true) || it.group.contains(query, true) }
        else if (flat) sorted else emptyList()
    }
    val positionKey = if (group == null) "__categories__" else group
    val initial = savedPositions?.get(positionKey) ?: (0 to 0)
    val scrollState = androidx.compose.runtime.key(positionKey) { rememberLazyListState(initial.first, initial.second) }
    LaunchedEffect(positionKey, scrollState, savedPositions) {
        snapshotFlow { scrollState.firstVisibleItemIndex to scrollState.firstVisibleItemScrollOffset }
            .distinctUntilChanged().collectLatest { pos -> savedPositions?.set(positionKey, pos) }
    }
    LazyColumn(modifier, state = scrollState) {
        if (flat || query.isNotBlank()) {
            itemsIndexed(found, key = { i, x -> "$i${x.url}" }) { _, x -> ItemRow(x, x.url in favUrls, { onFav(x) }) { onPick(x) } }
        } else if (group == null) {
            itemsIndexed(allCategories, key = { _, name -> name }) { _, name ->
                GroupRow(name, specialGroups[name]?.size ?: groups[name]?.size ?: 0) { onGroup(name) }
            }
        } else {
            val l = specialGroups[group] ?: groups[group] ?: emptyList()
            itemsIndexed(l, key = { i, x -> "$i${x.url}" }) { _, x -> ItemRow(x, x.url in favUrls, { onFav(x) }) { onPick(x) } }
        }
    }
}

@Composable
fun CinemaFeature(item: Item, onPlay: (Item) -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().height(190.dp).padding(vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF102D58), Color(0xFF07101F))))
            .clickable { onPlay(item) }
    ) {
        if (item.logo.startsWith("http")) {
            AsyncImage(
                model = item.logo,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xEE07101F), Color(0xBB07101F), Color(0x2207101F)))))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text("À LA UNE", color = c.secondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(item.name, color = Color.White, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onPlay(item) }, shape = RoundedCornerShape(12.dp)) { Text("▶  Regarder") }
        }
    }
}

@Composable
fun CinemaSuggestions(items: List<Item>, onPlay: (Item) -> Unit) {
    if (items.isEmpty()) return
    val c = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("À découvrir", color = c.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item ->
                Column(Modifier.width(126.dp).clip(RoundedCornerShape(14.dp))
                    .background(c.surfaceVariant.copy(alpha = 0.45f)).clickable { onPlay(item) }) {
                    Box(Modifier.fillMaxWidth().height(148.dp).background(Color(0xFF183458))) {
                        if (item.logo.startsWith("http")) {
                            AsyncImage(model = item.logo, contentDescription = item.name,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Text("▶", color = c.primary, modifier = Modifier.align(Alignment.Center), fontSize = 32.sp)
                        }
                    }
                    Text(item.name, Modifier.padding(8.dp), color = Color.White, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun EpgGrid(items: List<Item>, load: suspend (String) -> List<Prog>, onPlay: (Item) -> Unit) {
    val c = MaterialTheme.colorScheme
    val hs = rememberScrollState()
    val progs = remember { mutableStateMapOf<String, List<Prog>>() }
    val t0 = remember { (System.currentTimeMillis() / 1800000L) * 1800000L - 1800000L }
    val now = remember { System.currentTimeMillis() }
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val perMin = 5f
    val slots = 12
    Column(Modifier.fillMaxSize()) {
        Row {
            Spacer(Modifier.width(100.dp))
            Row(Modifier.weight(1f).horizontalScroll(hs)) {
                repeat(slots) { i ->
                    Text(fmt.format(Date(t0 + i * 1800000L)), Modifier.width((30 * perMin).dp), color = c.primary, fontSize = 12.sp)
                }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(items, key = { i, x -> "$i${x.id}" }) { _, ch ->
                LaunchedEffect(ch.id) {
                    if (progs[ch.id] == null) progs[ch.id] = try { load(ch.id) } catch (e: Exception) { emptyList() }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(ch.name, Modifier.width(100.dp).clickable { onPlay(ch) }.padding(end = 6.dp), fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.weight(1f).horizontalScroll(hs)) {
                        val list = progs[ch.id]
                        if (list == null) Text("…", color = c.onSurfaceVariant)
                        else if (list.isEmpty()) Text("Pas de guide", color = c.onSurfaceVariant, fontSize = 12.sp)
                        else {
                            var cursor = t0
                            list.forEach { p ->
                                val s = maxOf(p.start, t0)
                                val en = minOf(p.end, t0 + slots * 1800000L)
                                if (en > s && s >= cursor) {
                                    if (s > cursor) Spacer(Modifier.width(((s - cursor) / 60000f * perMin).dp))
                                    Box(
                                        Modifier.width(((en - s) / 60000f * perMin).dp).height(52.dp).padding(horizontal = 1.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (now >= p.start && now < p.end) c.primaryContainer else c.surfaceVariant)
                                            .clickable { onPlay(ch) }.padding(6.dp)
                                    ) {
                                        Column {
                                            Text(p.title, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text(fmt.format(Date(p.start)), fontSize = 10.sp, color = c.onSurfaceVariant)
                                        }
                                    }
                                    cursor = en
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
