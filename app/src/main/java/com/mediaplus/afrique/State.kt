package com.mediaplus.afrique

import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import org.json.JSONArray
import org.json.JSONObject

fun randomMac(): String = "00:1A:79:" + List(3) { "%02X".format((0..255).random()) }.joinToString(":")

private fun encode(i: Item) = listOf(i.kind, i.id, i.name, i.group, i.url, i.logo).joinToString("\t")

private fun decode(l: String): Item? {
    val p = l.split("\t")
    return if (p.size == 6 && p[0].toIntOrNull() != null) Item(p[0].toInt(), p[1], p[2], p[3], p[4], p[5]) else null
}

class AppState(private val ctx: Context, private val scope: CoroutineScope) {
    private val prefs = ctx.getSharedPreferences("p", 0)
    private val secure = SecureStore(ctx)
    private val cacheFile = File(ctx.cacheDir, "live.tsv")
    private var stalker: Stalker? = null
    // Display-only code; not used for login or authentication.
    fun macDisplayKey(): String {
        val period = System.currentTimeMillis() / 2592000000L
        val raw = mac.trim().uppercase() + "|" + period + "|MediaPlusAfrique"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        val value = (((bytes[0].toInt() and 255) * 65536) + ((bytes[1].toInt() and 255) * 256) + (bytes[2].toInt() and 255)) % 1000000
        return "%06d".format(java.util.Locale.US, value)
    }


    private val vodTtlMs = 24L * 60 * 60 * 1000
    private val vodLoading = mutableSetOf<Int>()
    private fun vodAccountKey(): String {
        val raw = "$mode:$user:$pass:$mac"
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(20)
    }
    private fun vodFile(kind: Int) = File(ctx.filesDir, "vod_${vodAccountKey()}_$kind.json")
    private fun vodTimestamp(kind: Int) = prefs.getLong("vod_time_${vodAccountKey()}_$kind", 0L)
    private fun readVod(kind: Int): Xtream.Catalog? = runCatching {
        val root = JSONObject(vodFile(kind).readText())
        val arr = root.getJSONArray("items")
        val items = List(arr.length()) { index ->
            val o = arr.getJSONObject(index)
            Item(o.getInt("kind"), o.getString("id"), o.getString("name"),
                o.getString("group"), o.getString("url"), o.optString("logo"), o.optLong("modifiedAt"))
        }
        val cats = root.getJSONArray("categories")
        Xtream.Catalog(items, List(cats.length()) { cats.getString(it) })
    }.getOrNull()
    private fun saveVod(kind: Int, catalog: Xtream.Catalog, key: String) {
        val root = JSONObject()
        val arr = JSONArray()
        catalog.items.forEach { item ->
            arr.put(JSONObject().put("kind", item.kind).put("id", item.id)
                .put("name", item.name).put("group", item.group).put("url", item.url)
                .put("logo", item.logo).put("modifiedAt", item.modifiedAt))
        }
        val categories = JSONArray()
        catalog.categories.forEach { categories.put(it) }
        root.put("items", arr).put("categories", categories)
        val target = File(ctx.filesDir, "vod_${key}_$kind.json")
        val temp = File(ctx.filesDir, "vod_${key}_$kind.tmp")
        temp.writeText(root.toString())
        if (!temp.renameTo(target)) { target.writeText(temp.readText()); temp.delete() }
        prefs.edit().putLong("vod_time_${key}_$kind", System.currentTimeMillis()).apply()
    }
    private fun loadVodCache() {
        if (mode != 0) return
        val key = vodAccountKey()
        scope.launch {
            val result = withContext(Dispatchers.IO) { readVod(2) to readVod(3) }
            if (key != vodAccountKey()) return@launch
            result.first?.let { movies = it.items; movieCategories = it.categories }
            result.second?.let { series = it.items; seriesCategories = it.categories }
            updateVod(2)
            updateVod(3)
        }
    }
    private fun updateVod(kind: Int, force: Boolean = false) {
        if (mode != 0 || kind !in 2..3 || !vodLoading.add(kind)) return
        val key = vodAccountKey()
        if (!force && vodFile(kind).exists() && vodTimestamp(kind) > 0 &&
            System.currentTimeMillis() - vodTimestamp(kind) < vodTtlMs) {
            vodLoading.remove(kind)
            return
        }
        val client = xt()
        scope.launch {
            try {
                val catalog = withContext(Dispatchers.IO) {
                    val c = if (kind == 2) client.movieCatalog() else client.seriesCatalog()
                    saveVod(kind, c, key)
                    c
                }
                if (key == vodAccountKey()) {
                    if (kind == 2) { movies = catalog.items; movieCategories = catalog.categories }
                    else { series = catalog.items; seriesCategories = catalog.categories }
                }
            } catch (e: Exception) {
                if (key == vodAccountKey() && (if (kind == 2) movies == null else series == null))
                    status = "Catalogue indisponible : ${e.message}"
            } finally { vodLoading.remove(kind) }
        }
    }


    var mode by mutableStateOf(prefs.getInt("mode", 0)) // 0 = identifiants, 1 = MAC
    var subscriptionExpirySeconds by mutableStateOf(prefs.getLong("subscription_expiry_seconds", 0L).takeIf { it > 0L })
    fun refreshSubscription() {
        if (mode != 0 || user.isBlank() || pass.isBlank()) {
            subscriptionExpirySeconds = null
            prefs.edit().remove("subscription_expiry_seconds").apply()
            return
        }
        scope.launch {
            val expiry = withContext(Dispatchers.IO) { runCatching { xt().expiryEpochSeconds() }.getOrNull() }
            subscriptionExpirySeconds = expiry
            prefs.edit().putLong("subscription_expiry_seconds", expiry ?: 0L).apply()
        }
    }
    // A salted PIN hash avoids storing the parental PIN in clear text.
    private fun pinHash(pin: String, salt: String): String = MessageDigest.getInstance("SHA-256")
        .digest((salt + ":" + pin).toByteArray()).joinToString("") { "%02x".format(it) }
    init { if (!prefs.contains("parental_pin_hash")) setParentalPin("0000") }
    val parentalPinExists get() = prefs.contains("parental_pin_hash")
    fun setParentalPin(pin: String): Boolean {
        if (parentalPinExists || !pin.matches(Regex("[0-9]{4}"))) return false
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString("parental_pin_salt", salt).putString("parental_pin_hash", pinHash(pin, salt)).apply()
        return true
    }
    fun changeParentalPin(oldPin: String, newPin: String): Boolean {
        if (!verifyParentalPin(oldPin) || !newPin.matches(Regex("[0-9]{4}"))) return false
        prefs.edit().remove("parental_pin_hash").remove("parental_pin_salt").commit()
        adultUnlocked = false
        return setParentalPin(newPin)
    }
    fun verifyParentalPin(pin: String): Boolean {
        val salt = prefs.getString("parental_pin_salt", "") ?: ""
        val expected = prefs.getString("parental_pin_hash", "") ?: ""
        return expected.isNotEmpty() && MessageDigest.isEqual(pinHash(pin, salt).toByteArray(), expected.toByteArray())
    }
    var adultUnlocked by mutableStateOf(false)
    var pendingAdultGroup by mutableStateOf<String?>(null)
    var pendingAdultItem by mutableStateOf<Item?>(null)
    fun isAdultGroup(name: String): Boolean = listOf("adult", "adulte", "xxx", "18+", "porn", "erotic", "érotique").any { name.contains(it, ignoreCase = true) }
    fun requestGroup(name: String) {
        if (isAdultGroup(name) && !adultUnlocked) pendingAdultGroup = name else group = name
    }
    fun requestAdultItem(item: Item, onAllowed: () -> Unit) {
        if (isAdultGroup(item.group) && !adultUnlocked) pendingAdultItem = item else onAllowed()
    }
    fun unlockAdult(pin: String): Boolean {
        if (!verifyParentalPin(pin)) return false
        adultUnlocked = true
        pendingAdultGroup?.let { group = it }
        pendingAdultGroup = null
        return true
    }
    fun lockAdult() { adultUnlocked = false; pendingAdultGroup = null; pendingAdultItem = null; group = null; clearPreview(); playing = null }

    // Multiple profiles reuse the existing Xtream/MAC login flow.
    data class Account(val name: String, val mode: Int, val user: String, val pass: String, val mac: String)
    val accounts = mutableStateListOf<Account>()
    private fun saveAccounts() {
        val array = JSONArray()
        accounts.forEach { a -> array.put(JSONObject().put("name", a.name).put("mode", a.mode).put("user", a.user).put("pass", a.pass).put("mac", a.mac)) }
        if (secure.put("accounts", array.toString())) prefs.edit().remove("accounts").apply()
    }
    fun addAccount(name: String, mode: Int, user: String, pass: String, mac: String): Boolean {
        if (name.isBlank() || (mode == 0 && (user.isBlank() || pass.isBlank())) || (mode == 1 && mac.isBlank())) return false
        accounts.add(Account(name.trim(), mode, user.trim(), pass, mac.trim()))
        saveAccounts(); return true
    }
    fun disconnect() {
        playing = null; playingChannel = null; clearPreview(); adultUnlocked = false
        group = null; section = 0; query = ""; episodes = null
        pass = ""
        secure.remove("pass")
        secure.remove("biometric_pass")
        prefs.edit().remove("pass").remove("session_active").remove("biometric_saved_user").apply()
        cacheFile.delete()
        live = emptyList(); movies = null; series = null
        showLogin = true; status = ""
    }
    fun removeAccount(index: Int) { if (index in accounts.indices) { accounts.removeAt(index); saveAccounts() } }
    fun activateAccount(index: Int) {
        val a = accounts.getOrNull(index) ?: return
        mode = a.mode; user = a.user; pass = a.pass; mac = a.mac
        adultUnlocked = false; group = null; clearPreview(); playing = null; live = emptyList(); movies = null; series = null
        cacheFile.delete()
        showLogin = true; status = "Appuyez sur Se connecter pour activer ${a.name}"
    }
    var user by mutableStateOf(prefs.getString("user", "") ?: "")
    fun saveUsername(value: String) { user = value; prefs.edit().putString("user", value).apply() }
    fun hasSavedBiometricAccount(): Boolean = mode == 0 && user.isNotBlank() &&
        prefs.getString("biometric_saved_user", "") == user && !secure.get("biometric_pass").isNullOrBlank()
    fun connectWithBiometric() {
        val saved = if (hasSavedBiometricAccount()) secure.get("biometric_pass") else null
        if (!saved.isNullOrBlank()) { pass = saved; connect() }
    }
    var biometricOptIn by mutableStateOf(prefs.getBoolean("biometric_opt_in", true))
    var rememberPassword by mutableStateOf(prefs.getBoolean("remember_password", false))
    var pass by mutableStateOf("")
    var mac by mutableStateOf(prefs.getString("mac", null) ?: randomMac().also { prefs.edit().putString("mac", it).apply() })
    var live by mutableStateOf(emptyList<Item>())
    var movies by mutableStateOf<List<Item>?>(null)
    var series by mutableStateOf<List<Item>?>(null)
    var episodes by mutableStateOf<List<Item>?>(null)
    var movieCategories by mutableStateOf(emptyList<String>())
    var seriesCategories by mutableStateOf(emptyList<String>())
    var newestFirst by mutableStateOf(true)
    var vodSort by mutableIntStateOf(0)
    var section by mutableStateOf(0) // 0 direct, 1 guide, 2 films, 3 séries, 4 favoris
    var group by mutableStateOf<String?>(null)
    val bouquetPositions = mutableStateMapOf<String, Pair<Int, Int>>()
    // Playback navigation keeps the selected virtual bouquet, not the source category.
    var playbackBouquetName by mutableStateOf<String?>(null)
    var playbackBouquetChannels by mutableStateOf<List<Item>>(emptyList())
    fun selectPlaybackBouquet(name: String?, channels: List<Item>) {
        playbackBouquetName = name
        playbackBouquetChannels = channels
    }
    var query by mutableStateOf("")
    var status by mutableStateOf("")
    var playing by mutableStateOf<String?>(null)
    var playingChannel by mutableStateOf<Item?>(null)
    // Shared ExoPlayer keeps the live stream running when the preview expands.
    private val livePlayerDelegate = lazy {
        androidx.media3.exoplayer.ExoPlayer.Builder(ctx)
            .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(PlayerCache.liveFactory()))
            .setLoadControl(androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(15000, 50000, 1500, 3000).build())
            .build()
    }
    val livePlayer get() = livePlayerDelegate.value
    fun isLivePlayerInitialized() = livePlayerDelegate.isInitialized()
    var previewUrl by mutableStateOf<String?>(null)
    var previewChannelId by mutableStateOf<String?>(null)
    var previewChannelName by mutableStateOf("")
    var showLogin by mutableStateOf(true)
    var cacheMb by mutableStateOf(0L)
    val favs = mutableStateListOf<Item>()
    val recentlyWatched = mutableStateListOf<String>()
    val continueWatching = mutableStateListOf<Item>()
    private var currentSeriesForResume: Item? = null
    private fun rememberVod(item: Item) {
        val selected = if (item.kind == 3) currentSeriesForResume ?: return else item
        if (selected.kind !in 1..2) return
        continueWatching.removeAll { it.kind == selected.kind && it.id == selected.id }
        continueWatching.add(0, selected)
        while (continueWatching.size > 30) continueWatching.removeAt(continueWatching.lastIndex)
        prefs.edit().putString("continue_watching", JSONArray().apply {
            continueWatching.forEach { put(JSONObject().put("kind", it.kind).put("id", it.id)
                .put("name", it.name).put("group", it.group).put("url", it.url)
                .put("logo", it.logo)) }
        }.toString()).apply()
    }
    private fun rememberLive(i: Item) {
        recentlyWatched.remove(i.id)
        recentlyWatched.add(0, i.id)
        while (recentlyWatched.size > 10) recentlyWatched.removeAt(recentlyWatched.lastIndex)
        prefs.edit().putString("recent_live_ids", recentlyWatched.joinToString("\n")).apply()
    }

    init {
        prefs.getString("pass", null)?.let { old ->
            pass = old
            if (prefs.getBoolean("remember_password", false)) secure.put("pass", old)
            prefs.edit().remove("pass").apply()
        }
        prefs.getString("accounts", null)?.let { old ->
            secure.put("accounts", old)
            if (android.os.Build.VERSION.SDK_INT >= 23) prefs.edit().remove("accounts").apply()
        }
        runCatching {
            val array = JSONArray(secure.get("accounts") ?: prefs.getString("accounts", "[]") ?: "[]")
            for (i in 0 until array.length()) {
                val a = array.getJSONObject(i)
                accounts.add(Account(a.optString("name"), a.optInt("mode"), a.optString("user"), a.optString("pass"), a.optString("mac")))
            }
        }
        recentlyWatched.addAll((prefs.getString("recent_live_ids", "") ?: "").split("\n").filter { it.isNotBlank() }.take(10))
        favs.addAll(prefs.getStringSet("favs", emptySet())?.mapNotNull { decode(it) } ?: emptyList())
        runCatching {
            val saved = JSONArray(prefs.getString("continue_watching", "[]") ?: "[]")
            for (index in 0 until minOf(saved.length(), 30)) {
                val o = saved.getJSONObject(index)
                continueWatching.add(Item(o.getInt("kind"), o.getString("id"), o.getString("name"),
                    o.optString("group"), o.getString("url"), o.optString("logo")))
            }
        }
        // Resume only sessions whose credentials were explicitly saved securely.
        val savedPassword = if (rememberPassword) secure.get("pass") else null
        if (prefs.getBoolean("session_active", false) && (mode == 1 || (mode == 0 && !savedPassword.isNullOrBlank()))) {
            if (mode == 0) pass = savedPassword.orEmpty()
            showLogin = true
            connect()
        } else showLogin = true

    }

    private fun xt() = Xtream(Config.SERVER, user, pass)

    private fun fetch(block: suspend () -> Unit) {
        status = "Chargement…"
        scope.launch { try { block(); status = "" } catch (e: Exception) { status = "Erreur : ${e.message}" } }
    }

    fun connect() {
        section = 0; group = null; episodes = null; query = ""; clearPreview(); playing = null
        prefs.edit().putInt("mode", mode).putString("user", user).putString("mac", mac).remove("pass").apply()
        val savedSecurely = mode == 0 && rememberPassword && pass.isNotBlank() && secure.put("pass", pass)
        if (!savedSecurely) secure.remove("pass")
        prefs.edit().putBoolean("remember_password", rememberPassword).apply()
        status = "Connexion…"
        scope.launch {
            try {
                val l = withContext(Dispatchers.IO) {
                    if (mode == 0 && xt().isSubscriptionExpired()) {
                        throw IllegalStateException("Abonnement Expiré")
                    }
                    val r = if (mode == 0) xt().live() else Stalker(Config.PORTAL, mac.trim().uppercase()).also { stalker = it }.channels()
                    cacheFile.writeText(r.joinToString("\n") { encode(it) })
                    r
                }
                live = l; movies = null; series = null; episodes = null; movieCategories = emptyList(); seriesCategories = emptyList()
                section = 0; group = null; query = ""; selectPlaybackBouquet(null, emptyList())
                if (mode == 0 && rememberPassword && biometricOptIn && pass.isNotBlank()) {
                    secure.put("biometric_pass", pass)
                    prefs.edit().putString("biometric_saved_user", user).putBoolean("biometric_opt_in", true).apply()
                } else if (mode == 0) {
                    secure.remove("biometric_pass")
                    prefs.edit().remove("biometric_saved_user").putBoolean("biometric_opt_in", false).apply()
                }
                prefs.edit().putBoolean("session_active", mode == 1 || savedSecurely).apply()
                status = "${l.size} chaînes"; showLogin = false
                refreshSubscription()
                loadVodCache()
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                status = if (msg == "Abonnement Expiré" ||
                    msg.contains("expired", ignoreCase = true) ||
                    msg.contains("expiré", ignoreCase = true))
                    "Abonnement Expiré" else "Erreur de récupération"
                prefs.edit().putBoolean("session_active", false).apply()
            }
        }
    }

    fun current(): List<Item> = episodes ?: when (section) {
        0, 1 -> live
        2 -> movies ?: emptyList()
        3 -> series ?: emptyList()
        else -> favs.toList()
    }

    fun pickSection(s: Int) {
        section = s; group = null; query = ""; episodes = null; status = ""
        selectPlaybackBouquet(null, emptyList())
        if (s != 0) clearPreview()
        if ((s == 2 || s == 3) && mode != 0) { status = "Films et séries : connexion par identifiants requise"; return }
        if (s == 2 || s == 3) updateVod(s)
    }

    fun refreshCatalog() {
        if (section == 2 || section == 3) updateVod(section, force = true)
    }

    fun clearPreview() {
        if (playing == null && isLivePlayerInitialized()) livePlayer.stop()
        previewUrl = null
        previewChannelId = null
        previewChannelName = ""
    }

    fun selectLive(i: Item) {
        rememberLive(i)
        if (previewChannelId == i.id && previewUrl != null) {
            playLive(i, fromPreview = true)
            return
        }
        clearPreview()
        previewChannelId = i.id
        previewChannelName = i.name
        scope.launch {
            try {
                val url = if (mode == 1) withContext(Dispatchers.IO) {
                    (stalker ?: Stalker(Config.PORTAL, mac.trim().uppercase()).also { stalker = it }).resolve(i.url)
                } else i.url
                if (previewChannelId == i.id && section == 0) previewUrl = url
            } catch (e: Exception) {
                if (previewChannelId == i.id) { clearPreview(); status = "Aperçu impossible : ${e.message}" }
            }
        }
    }

    private var liveRequestId = 0L
    fun stopLivePlayback() {
        ++liveRequestId // Cancel any pending channel URL resolution.
        if (isLivePlayerInitialized()) {
            livePlayer.playWhenReady = false
            livePlayer.stop()
            livePlayer.clearMediaItems()
        }
        playing = null
        playingChannel = null
        clearPreview()
    }

    fun playLive(i: Item, fromPreview: Boolean = false) {
        val request = ++liveRequestId
        rememberLive(i)
        playingChannel = i
        playing = null
        if (!fromPreview) clearPreview()
        scope.launch {
            try {
                val resolved = if (mode == 1) withContext(Dispatchers.IO) {
                    (stalker ?: Stalker(Config.PORTAL, mac.trim().uppercase()).also { stalker = it }).resolve(i.url)
                } else i.url
                if (request == liveRequestId) {
                    playing = resolved
                    if (fromPreview) clearPreview()
                }
            } catch (e: Exception) {
                if (request == liveRequestId) { playingChannel = null; status = "Erreur chaîne : ${e.message}" }
            }
        }
    }

    fun pick(i: Item) {
        if (section == 0 && i.kind == 0) { selectLive(i); return }
        clearPreview()
        playingChannel = null
        if (i.kind == 2) {
            currentSeriesForResume = i
            rememberVod(i)
            fetch { episodes = withContext(Dispatchers.IO) { xt().episodes(i.url) }; group = null; query = "" }
        } else {
            if (i.kind == 1 || i.kind == 3) rememberVod(i)
            scope.launch {
                try {
                    playing = if (mode == 1 && i.kind == 0) withContext(Dispatchers.IO) { stalker!!.resolve(i.url) } else i.url
                } catch (e: Exception) { status = "Erreur : ${e.message}" }
            }
        }
    }

    fun toggleFav(i: Item) {
        val idx = favs.indexOfFirst { it.url == i.url }
        if (idx >= 0) favs.removeAt(idx) else favs.add(i)
        prefs.edit().putStringSet("favs", favs.map { encode(it) }.toSet()).apply()
    }

    suspend fun epg(id: String): List<Prog> = withContext(Dispatchers.IO) { xt().epg(id) }

    fun onPlayerError(msg: String) {
        val u = playing ?: previewUrl ?: return
        if (u.endsWith(".m3u8")) {
            if (playing != null) playing = u.dropLast(5) + ".ts"
            else previewUrl = u.dropLast(5) + ".ts"
        } else {
            playing = null; clearPreview(); status = "Lecture impossible : $msg"
        }
    }

    fun refreshCache() { cacheMb = PlayerCache.sizeMb(ctx) }
    fun clearCache() {
        PlayerCache.clear(ctx)
        recentlyWatched.clear()
        continueWatching.clear()
        prefs.edit().remove("recent_live_ids").remove("continue_watching").apply()
        // Remove cached channel and VOD catalogs without affecting accounts or favorites.
        cacheFile.delete()
        ctx.filesDir.listFiles()?.filter { it.name.startsWith("vod_") && it.name.endsWith(".json") }?.forEach { it.delete() }
        prefs.edit().apply {
            prefs.all.keys.filter { it.startsWith("vod_time_") }.forEach { remove(it) }
        }.apply()
        refreshCache()
    }

    fun back() {
        when {
            playing != null -> stopLivePlayback()
            previewChannelId != null -> clearPreview()
            query.isNotEmpty() -> query = ""
            episodes != null -> { episodes = null; group = null }
            group != null -> group = null
            !showLogin -> showLogin = true
        }
    }
}
