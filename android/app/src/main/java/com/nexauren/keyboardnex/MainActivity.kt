package com.nexauren.keyboardnex

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.nexauren.keyboardnex.clipboard.ClipboardStore
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val channelName = "com.nexauren.keyboardnex/bridge"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                try {
                    when (call.method) {
                        "getState" -> result.success(readAppState())

                        "setTheme" -> {
                            val name = call.argument<String>("name").orEmpty()
                            if (name.isBlank()) {
                                result.error("INVALID_THEME", "Theme name cannot be empty.", null)
                            } else {
                                settings().edit().putString("theme_name", name).apply()
                                result.success(true)
                            }
                        }

                        "setSetting" -> {
                            val key = call.argument<String>("key").orEmpty()
                            val value = call.argument<Any?>("value")
                            if (saveSetting(key, value)) {
                                result.success(true)
                            } else {
                                result.error("INVALID_SETTING", "Unsupported setting or value.", null)
                            }
                        }

                        "updateClip" -> {
                            val id = call.argument<String>("id").orEmpty()
                            val text = call.argument<String>("text").orEmpty()
                            ClipboardStore.updateText(this, id, text)
                            result.success(true)
                        }

                        "setClipsPinned" -> {
                            val ids = call.argument<List<String>>("ids").orEmpty().toSet()
                            val pinned = call.argument<Boolean>("pinned") ?: false
                            ClipboardStore.setPinned(this, ids, pinned)
                            result.success(true)
                        }

                        "deleteClips" -> {
                            val ids = call.argument<List<String>>("ids").orEmpty().toSet()
                            ClipboardStore.delete(this, ids)
                            result.success(true)
                        }

                        "clearClips" -> {
                            ClipboardStore.clear(this)
                            result.success(true)
                        }

                        "openInputMethodSettings" -> {
                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                            result.success(true)
                        }

                        "showInputMethodPicker" -> {
                            val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            manager.showInputMethodPicker()
                            result.success(true)
                        }

                        else -> result.notImplemented()
                    }
                } catch (error: Exception) {
                    result.error("KEYBOARD_NEX_ERROR", error.message ?: "Unexpected native error.", null)
                }
            }
    }

    private fun settings() =
        getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)

    private fun readAppState(): Map<String, Any?> {
        val prefs = settings()
        val clips = ClipboardStore.all(this).map { clip ->
            mapOf(
                "id" to clip.id,
                "text" to clip.text,
                "createdAt" to clip.createdAt,
                "expiresAt" to clip.expiresAt,
                "pinned" to clip.pinned
            )
        }

        return mapOf(
            "themeName" to (prefs.getString("theme_name", "Neon Violet") ?: "Neon Violet"),
            "autoCapture" to prefs.getBoolean("auto_capture", true),
            "clipTtlMs" to prefs.getLong("clip_ttl_ms", 60L * 60L * 1000L),
            "hapticsEnabled" to prefs.getBoolean("haptics_enabled", true),
            "soundEnabled" to prefs.getBoolean("sound_enabled", false),
            "keyboardHeight" to (prefs.getString("keyboard_height", "normal") ?: "normal"),
            "clips" to clips
        )
    }

    private fun saveSetting(key: String, value: Any?): Boolean {
        val editor = settings().edit()
        when (key) {
            "auto_capture", "haptics_enabled", "sound_enabled" -> {
                val boolValue = value as? Boolean ?: return false
                editor.putBoolean(key, boolValue)
            }

            "clip_ttl_ms" -> {
                val number = value as? Number ?: return false
                val ttl = number.toLong()
                if (ttl <= 0L) return false
                editor.putLong(key, ttl)
            }

            "keyboard_height" -> {
                val height = value as? String ?: return false
                if (height !in setOf("compact", "normal", "tall")) return false
                editor.putString(key, height)
            }

            else -> return false
        }
        editor.apply()
        return true
    }
}
