package com.mediaplus.afrique

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
fun fixUrl(u: String): String = u.trim().let { if (it.startsWith("http")) it else "http://$it" }

fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String {
    val c = URL(url).openConnection() as HttpURLConnection
    c.connectTimeout = 10000; c.readTimeout = 60000
    headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
    return c.inputStream.bufferedReader().readText()
}

private fun b64(s: String): String = try {
    String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8).ifBlank { s }
} catch (e: Exception) { s }

class Xtream(server: String, user: String, pass: String) {
    private val base = fixUrl(server).trimEnd('/')
    private val auth = "${enc(user)}/${enc(pass)}"
    private val api = "$base/player_api.php?username=${enc(user)}&password=${enc(pass)}"

    // Xtream user_info.exp_date is a Unix timestamp in seconds; null/0 means unknown.
    fun expiryEpochSeconds(): Long? {
        val info = JSONObject(httpGet(api)).optJSONObject("user_info") ?: return null
        val raw = info.optString("exp_date").trim()
        return raw.toLongOrNull()?.takeIf { it > 0L }
    }

    fun isSubscriptionExpired(): Boolean {
        val info = JSONObject(httpGet(api)).optJSONObject("user_info") ?: return false
        val expiry = info.optString("exp_date").trim().toLongOrNull()
        return (expiry != null && expiry > 0L && expiry <= System.currentTimeMillis() / 1000L) ||
            info.optString("status").equals("Expired", ignoreCase = true)
    }

    data class Catalog(val items: List<Item>, val categories: List<String>)

    private fun catalog(catAction: String, listAction: String, build: (JSONObject, String) -> Item): Catalog {
        val ct = httpGet("$api&action=$catAction")
        if (!ct.trimStart().startsWith("[")) throw Exception("Identifiants incorrects ou serveur indisponible")
        val cats = LinkedHashMap<String, String>()
        val ca = JSONArray(ct)
        for (i in 0 until ca.length()) ca.getJSONObject(i).let {
            cats[it.optString("category_id")] = it.optString("category_name").ifBlank { "Autres" }
        }
        val a = JSONArray(httpGet("$api&action=$listAction"))
        val items = List(a.length()) { i ->
            val o = a.getJSONObject(i)
            build(o, cats[o.optString("category_id")] ?: "Autres")
        }
        return Catalog(items, cats.values.distinct() + if (items.any { it.group == "Autres" } && "Autres" !in cats.values) listOf("Autres") else emptyList())
    }

    private fun load(catAction: String, listAction: String, build: (JSONObject, String) -> Item): List<Item> =
        catalog(catAction, listAction, build).items

    private fun modified(o: JSONObject): Long = listOf("last_modified", "added", "release_date").firstNotNullOfOrNull { key ->
        o.optString(key).toLongOrNull()?.let { if (it > 100000000000L) it / 1000 else it }
    } ?: 0L

    fun movieCatalog() = catalog("get_vod_categories", "get_vod_streams") { o, g ->
        val id = o.optString("stream_id")
        val ext = o.optString("container_extension").ifEmpty { "mp4" }
        Item(1, id, o.optString("name"), g, "$base/movie/$auth/$id.$ext", o.optString("stream_icon"), modified(o))
    }

    fun seriesCatalog() = catalog("get_series_categories", "get_series") { o, g ->
        val id = o.optString("series_id")
        Item(2, id, o.optString("name"), g, id, o.optString("cover"), modified(o))
    }

    fun live() = load("get_live_categories", "get_live_streams") { o, g ->
        val id = o.optString("stream_id")
        Item(0, id, o.optString("name"), g, "$base/live/$auth/$id.m3u8", o.optString("stream_icon"))
    }

    fun movies() = movieCatalog().items
    fun series() = seriesCatalog().items

    fun episodes(seriesId: String): List<Item> {
        val eps = JSONObject(httpGet("$api&action=get_series_info&series_id=$seriesId")).optJSONObject("episodes")
            ?: return emptyList()
        val out = ArrayList<Item>()
        eps.keys().asSequence().sortedBy { it.toIntOrNull() ?: 0 }.forEach { s ->
            val arr = eps.optJSONArray(s) ?: return@forEach
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                val id = e.optString("id")
                val ext = e.optString("container_extension").ifEmpty { "mp4" }
                val n = e.optString("episode_num").padStart(2, '0')
                val img = e.optJSONObject("info")?.optString("movie_image") ?: ""
                out.add(Item(3, id, "E$n ${e.optString("title")}", "Saison $s", "$base/series/$auth/$id.$ext", img))
            }
        }
        return out
    }

    fun epg(streamId: String): List<Prog> {
        val arr = JSONObject(httpGet("$api&action=get_short_epg&stream_id=$streamId&limit=10"))
            .optJSONArray("epg_listings") ?: return emptyList()
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Prog(b64(o.optString("title")), o.optLong("start_timestamp") * 1000, o.optLong("stop_timestamp") * 1000)
        }.filter { it.end > it.start }
    }
}

class Stalker(portal: String, private val mac: String) {
    private val load: String
    private var token: String? = null
    init {
        val p = fixUrl(portal).trimEnd('/')
        load = if (p.endsWith("load.php")) p else p.removeSuffix("/c") + "/server/load.php"
    }
    private fun h() = mapOf(
        "Cookie" to "mac=$mac; stb_lang=en; timezone=Europe/Paris",
        "Authorization" to "Bearer ${token ?: ""}",
        "User-Agent" to "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3",
        "X-User-Agent" to "Model: MAG250; Link: WiFi")
    private fun js(q: String) = JSONObject(httpGet("$load?$q&JsHttpRequest=1-xml", h())).getJSONObject("js")

    private fun connect() {
        token = js("type=stb&action=handshake&token=").getString("token")
        try { js("type=stb&action=get_profile&hd=1&stb_type=MAG250&client_type=STB&auth_second_step=1&not_valid_token=0") } catch (_: Exception) {}
    }

    fun channels(): List<Item> {
        connect()
        val g = HashMap<String, String>()
        try {
            val a = JSONObject(httpGet("$load?type=itv&action=get_genres&JsHttpRequest=1-xml", h())).getJSONArray("js")
            for (i in 0 until a.length()) a.getJSONObject(i).let { g[it.optString("id")] = it.optString("title") }
        } catch (_: Exception) {}
        val d = js("type=itv&action=get_all_channels").getJSONArray("data")
        return List(d.length()) { i ->
            val o = d.getJSONObject(i)
            Item(0, o.optString("id"), o.optString("name"), g[o.optString("tv_genre_id")] ?: "", o.optString("cmd"), o.optString("logo"))
        }
    }

    fun resolve(cmd: String): String {
        if (token == null) connect()
        val r = js("type=itv&action=create_link&cmd=${enc(cmd)}&series=&forced_storage=undefined&disable_ad=0&download=0").getString("cmd")
        return r.trim().removePrefix("ffmpeg").trim()
    }
}
