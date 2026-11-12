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
    private val cacheFile = File(ctx.cacheDir, "live.tsv")
    private var stalker: Stalker? = null

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
    val parentalPinExists get() = prefs.contains("parental_pin_hash")
    fun setParentalPin(pin: String): Boolean {
        if (parentalPinExists || !pin.matches(Regex("[0-9]{4}"))) return false
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString("parental_pin_salt", salt).putString("parental_pin_hash", pinHash(pin, salt)).apply()
        return true
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
        prefs.edit().putString("accounts", array.toString()).apply()
    }
    fun addAccount(name: String, mode: Int, user: String, pass: String, mac: String): Boolean {
        if (name.isBlank() || (mode == 0 && (user.isBlank() || pass.isBlank())) || (mode == 1 && mac.isBlank())) return false
        accounts.add(Account(name.trim(), mode, user.trim(), pass, mac.trim()))
        saveAccounts(); return true
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
    var pass by mutableStateOf(prefs.getString("pass", "") ?: "")
    var mac by mutableStateOf(prefs.getString("mac", null) ?: randomMac().also { prefs.edit().putString("mac", it).apply() })
    var live by mutableStateOf(emptyList<Item>())
    var movies by mutableStateOf<List<Item>?>(null)
    var series by mutableStateOf<List<Item>?>(null)
    var episodes by mutableStateOf<List<Item>?>(null)
    var movieCategories by mutableStateOf(emptyList<String>())
    var seriesCategories by mutableStateOf(emptyList<String>())
    var newestFirst by mutableStateOf(true)
    var section by mutableStateOf(0) // 0 direct, 1 guide, 2 films, 3 séries, 4 favoris
    var group by mutableStateOf<String?>(null)
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
    var previewUrl by mutableStateOf<String?>(null)
    var previewChannelId by mutableStateOf<String?>(null)
    var previewChannelName by mutableStateOf("")
    var showLogin by mutableStateOf(true)
    var cacheMb by mutableStateOf(0L)
    val favs = mutableStateListOf<Item>()
    val recentlyWatched = mutableStateListOf<String>()
    private fun rememberLive(i: Item) {
        recentlyWatched.remove(i.id)
        recentlyWatched.add(0, i.id)
        while (recentlyWatched.size > 10) recentlyWatched.removeAt(recentlyWatched.lastIndex)
        prefs.edit().putString("recent_live_ids", recentlyWatched.joinToString("\n")).apply()
    }

    init {
        runCatching {
            val array = JSONArray(prefs.getString("accounts", "[]") ?: "[]")
            for (i in 0 until array.length()) {
                val a = array.getJSONObject(i)
                accounts.add(Account(a.optString("name"), a.optInt("mode"), a.optString("user"), a.optString("pass"), a.optString("mac")))
            }
        }
        recentlyWatched.addAll((prefs.getString("recent_live_ids", "") ?: "").split("\n").filter { it.isNotBlank() }.take(10))
        favs.addAll(prefs.getStringSet("favs", emptySet())?.mapNotNull { decode(it) } ?: emptyList())
        if (cacheFile.exists()) {
            live = runCatching { cacheFile.readLines().mapNotNull { decode(it) } }.getOrDefault(emptyList())
            if (live.isNotEmpty()) {
                showLogin = false; status = "${live.size} chaînes"
                if (mode == 1) stalker = Stalker(Config.PORTAL, mac.trim().uppercase())
            }
        }
    }

    private fun xt() = Xtream(Config.SERVER, user, pass)

    private fun fetch(block: suspend () -> Unit) {
        status = "Chargement…"
        scope.launch { try { block(); status = "" } catch (e: Exception) { status = "Erreur : ${e.message}" } }
    }

    fun connect() {
        prefs.edit().putInt("mode", mode).putString("user", user).putString("pass", pass).putString("mac", mac).apply()
        status = "Connexion…"
        scope.launch {
            try {
                val l = withContext(Dispatchers.IO) {
                    val r = if (mode == 0) xt().live() else Stalker(Config.PORTAL, mac.trim().uppercase()).also { stalker = it }.channels()
                    cacheFile.writeText(r.joinToString("\n") { encode(it) })
                    r
                }
                live = l; movies = null; series = null; episodes = null; movieCategories = emptyList(); seriesCategories = emptyList()
                section = 0; group = null; query = ""; selectPlaybackBouquet(null, emptyList())
                status = "${l.size} chaînes"; showLogin = false
                refreshSubscription()
            } catch (e: Exception) { status = "Erreur : ${e.message}" }
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
        if (s == 2 && movies == null) refreshCatalog()
        if (s == 3 && series == null) refreshCatalog()
    }

    fun refreshCatalog() {
        if (mode != 0 || (section != 2 && section != 3)) return
        val requested = section
        fetch {
            val catalog = withContext(Dispatchers.IO) {
                if (requested == 2) xt().movieCatalog() else xt().seriesCatalog()
            }
            if (requested == 2) { movies = catalog.items; movieCategories = catalog.categories }
            else { series = catalog.items; seriesCategories = catalog.categories }
        }
    }

    fun clearPreview() {
        previewUrl = null
        previewChannelId = null
        previewChannelName = ""
    }

    fun selectLive(i: Item) {
        rememberLive(i)
        if (previewChannelId == i.id && previewUrl != null) {
            playLive(i)
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

    fun playLive(i: Item) {
        rememberLive(i)
        playingChannel = i
        clearPreview()
        scope.launch {
            try {
                playing = if (mode == 1) withContext(Dispatchers.IO) {
                    (stalker ?: Stalker(Config.PORTAL, mac.trim().uppercase()).also { stalker = it }).resolve(i.url)
                } else i.url
            } catch (e: Exception) { status = "Erreur chaîne : ${e.message}" }
        }
    }

    fun pick(i: Item) {
        if (section == 0 && i.kind == 0) { selectLive(i); return }
        clearPreview()
        playingChannel = null
        if (i.kind == 2) {
            fetch { episodes = withContext(Dispatchers.IO) { xt().episodes(i.url) }; group = null; query = "" }
        } else {
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
    fun clearCache() { PlayerCache.clear(ctx); refreshCache() }

    fun back() {
        when {
            playing != null -> { playing = null; playingChannel = null }
            previewChannelId != null -> clearPreview()
            query.isNotEmpty() -> query = ""
            episodes != null -> { episodes = null; group = null }
            group != null -> group = null
            !showLogin -> showLogin = true
        }
    }
}
