@file:OptIn(UnstableApi::class)

package com.mediaplus.afrique

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

fun bouquetIcon(name: String): String {
    val n = name.lowercase()
    return when {
        "récemment" in n || "recent" in n -> ""
        "côte d’ivoire" in n || "cote d'ivoire" in n || "ivoire" in n || n.startsWith("ci ") -> "🇨🇮"
        n.startsWith("fr") || "france" in n -> "🇫🇷"
        n.startsWith("uk") || "united kingdom" in n || "royaume-uni" in n -> "🇬🇧"
        n.startsWith("us") || "usa" in n || "états-unis" in n -> "🇺🇸"
        n.startsWith("tn") || "tunisie" in n -> "🇹🇳"
        n.startsWith("ma") || "maroc" in n -> "🇲🇦"
        n.startsWith("dz") || "algérie" in n || "algerie" in n -> "🇩🇿"
        n.startsWith("sn") || "sénégal" in n || "senegal" in n -> "🇸🇳"
        n.startsWith("cm") || "cameroun" in n -> "🇨🇲"
        n.startsWith("be") || "belgique" in n -> "🇧🇪"
        n.startsWith("ch") || "suisse" in n -> "🇨🇭"

        else -> ""
    }
}

object PlayerCache {
    private var cache: SimpleCache? = null

    @Synchronized
    fun get(ctx: Context): SimpleCache = cache ?: SimpleCache(
        File(ctx.cacheDir, "media"),
        LeastRecentlyUsedCacheEvictor(256L * 1024 * 1024),
        StandaloneDatabaseProvider(ctx)
    ).also { cache = it }

    fun factory(ctx: Context): CacheDataSource.Factory {
        val http = DefaultHttpDataSource.Factory().setUserAgent("MediaPlus/1.0").setAllowCrossProtocolRedirects(true)
        return CacheDataSource.Factory().setCache(get(ctx)).setUpstreamDataSourceFactory(http)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    // Live streams must never read stale video segments from the disk cache.
    fun liveFactory(): DefaultHttpDataSource.Factory =
        DefaultHttpDataSource.Factory()
            .setUserAgent("MediaPlus/1.0")
            .setAllowCrossProtocolRedirects(true)

    fun sizeMb(ctx: Context): Long = get(ctx).cacheSpace / (1024 * 1024)

    fun clear(ctx: Context) {
        val c = get(ctx)
        for (k in c.keys.toList()) c.removeResource(k)
    }
}
