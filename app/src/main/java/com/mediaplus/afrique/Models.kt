package com.mediaplus.afrique

object Config {
    const val SERVER = "http://vpn.mediafrique.xyz"
    const val PORTAL = "http://vpn.mediafrique.xyz/c/"
}

/** kind: 0 = direct, 1 = film, 2 = série, 3 = épisode */
data class Item(val kind: Int, val id: String, val name: String, val group: String, val url: String, val logo: String = "", val modifiedAt: Long = 0L)

data class Prog(val title: String, val start: Long, val end: Long)

fun Item.bouquet(): String = group.ifBlank { "Autres" }
