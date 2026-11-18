package com.nexauren.keyboardnex.clipboard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ClipboardStore {
    const val SETTINGS = "keyboard_nex_settings"
    private const val STORE = "keyboard_nex_clipboard"
    private const val ITEMS = "items"
    private const val MAX_ITEMS = 100

    data class Clip(
        val id: String,
        val text: String,
        val createdAt: Long,
        val expiresAt: Long?,
        val pinned: Boolean
    )

    fun isAutoCaptureEnabled(context: Context): Boolean =
        context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
            .getBoolean("auto_capture", true)

    fun capture(context: Context, text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty() || !isAutoCaptureEnabled(context)) return

        val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val current = read(context).toMutableList()
        if (current.firstOrNull()?.text == cleanText) return

        val now = System.currentTimeMillis()
        val ttl = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
            .getLong("clip_ttl_ms", 60L * 60L * 1000L)

        current.add(
            0,
            Clip(
                id = "\${now}_\${cleanText.hashCode()}",
                text = cleanText,
                createdAt = now,
                expiresAt = now + ttl,
                pinned = false
            )
        )

        val trimmed = current
            .distinctBy { it.text }
            .let { entries ->
                val pinned = entries.filter { it.pinned }
                val recent = entries.filterNot { it.pinned }.take((MAX_ITEMS - pinned.size).coerceAtLeast(0))
                (pinned + recent).sortedByDescending { it.createdAt }
            }
        write(prefs, trimmed)
    }

    fun all(context: Context): List<Clip> {
        val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val entries = read(context)
        val active = entries.filter { it.pinned || it.expiresAt == null || it.expiresAt > now }
        if (active.size != entries.size) write(prefs, active)
        return active.sortedWith(compareByDescending<Clip> { it.pinned }.thenByDescending { it.createdAt })
    }

    fun updateText(context: Context, id: String, text: String) {
        val value = text.trim()
        if (value.isEmpty()) return
        val updated = read(context).map {
            if (it.id == id) it.copy(text = value) else it
        }
        write(context.getSharedPreferences(STORE, Context.MODE_PRIVATE), updated)
    }

    fun setPinned(context: Context, ids: Set<String>, pinned: Boolean) {
        val now = System.currentTimeMillis()
        val updated = read(context).map {
            if (it.id in ids) {
                it.copy(
                    pinned = pinned,
                    expiresAt = if (pinned) null else now + context
                        .getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
                        .getLong("clip_ttl_ms", 60L * 60L * 1000L)
                )
            } else it
        }
        write(context.getSharedPreferences(STORE, Context.MODE_PRIVATE), updated)
    }

    fun delete(context: Context, ids: Set<String>) {
        val remaining = read(context).filterNot { it.id in ids }
        write(context.getSharedPreferences(STORE, Context.MODE_PRIVATE), remaining)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().remove(ITEMS).apply()
    }

    private fun read(context: Context): List<Clip> {
        val raw = context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getString(ITEMS, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val expiry = if (item.isNull("expiresAt")) null else item.optLong("expiresAt")
                    add(
                        Clip(
                            id = item.optString("id"),
                            text = item.optString("text"),
                            createdAt = item.optLong("createdAt"),
                            expiresAt = expiry,
                            pinned = item.optBoolean("pinned")
                        )
                    )
                }
            }.filter { it.id.isNotBlank() && it.text.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun write(prefs: android.content.SharedPreferences, clips: List<Clip>) {
        val array = JSONArray()
        clips.forEach { clip ->
            array.put(
                JSONObject().apply {
                    put("id", clip.id)
                    put("text", clip.text)
                    put("createdAt", clip.createdAt)
                    put("expiresAt", clip.expiresAt ?: JSONObject.NULL)
                    put("pinned", clip.pinned)
                }
            )
        }
        prefs.edit().putString(ITEMS, array.toString()).apply()
    }
}
