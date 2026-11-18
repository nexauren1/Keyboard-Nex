package com.nexauren.keyboardnex.ime

import android.content.Context
import org.json.JSONObject

internal data class EmojiEntry(
    val emoji: String,
    val group: String,
    val subgroup: String,
    val englishName: String,
    val portugueseName: String,
    val keywords: String
) {
    val searchText: String
        get() = "$portugueseName $keywords $englishName $subgroup $group".lowercase()

    val toneBase: String
        get() {
            val result = StringBuilder()
            var index = 0
            while (index < emoji.length) {
                val codePoint = Character.codePointAt(emoji, index)
                if (codePoint !in 0x1F3FB..0x1F3FF) result.appendCodePoint(codePoint)
                index += Character.charCount(codePoint)
            }
            return result.toString()
        }
}

internal object EmojiCatalog {
    @Volatile
    private var cached: List<EmojiEntry>? = null

    fun load(context: Context): List<EmojiEntry> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val source = context.assets.open("emoji_catalog.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(source)
            val array = root.getJSONArray("items")
            val result = ArrayList<EmojiEntry>(array.length())
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                result.add(
                    EmojiEntry(
                        emoji = item.optString("e"),
                        group = item.optString("g"),
                        subgroup = item.optString("s"),
                        englishName = item.optString("n"),
                        portugueseName = item.optString("p"),
                        keywords = item.optString("k")
                    )
                )
            }
            cached = result
            return result
        }
    }
}
