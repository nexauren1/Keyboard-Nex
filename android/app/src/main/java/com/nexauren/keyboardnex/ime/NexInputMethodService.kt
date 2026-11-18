package com.nexauren.keyboardnex.ime

import android.app.AlertDialog
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale
import android.text.InputType
import android.inputmethodservice.InputMethodService
import android.widget.CheckBox
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import org.json.JSONArray
import org.json.JSONObject
import com.nexauren.keyboardnex.MainActivity
import com.nexauren.keyboardnex.clipboard.ClipboardStore

class NexInputMethodService : InputMethodService() {
    private val keyColor: Int get() = if (themeName() == "Minimal Light") Color.rgb(244, 246, 252) else Color.rgb(31, 35, 59)
    private val textColor: Int get() = if (themeName() == "Minimal Light") Color.rgb(28, 32, 49) else Color.rgb(245, 246, 255)
    private val muted: Int get() = if (themeName() == "Minimal Light") Color.rgb(91, 99, 124) else Color.rgb(169, 176, 204)
    private var shifted = false
    private var capsLocked = false
    private var lastShiftTapAt = 0L
    private var numberMode = false
    private var lastSpacePressAt = 0L
    private val composingWord = StringBuilder()
    private var suggestionStrip: LinearLayout? = null
    private var currentSuggestions: List<String> = emptyList()
    private var currentWordIsTypo = false
    private var spellCheckerSession: SpellCheckerSession? = null
    private var suggestionRequestSequence = 0
    private var root: LinearLayout? = null
    private val selectedClipIds = linkedSetOf<String>()
    private lateinit var clipboardManager: ClipboardManager
    private var listenerRegistered = false

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        captureCurrentClipboard()
    }

    private val spellCheckerListener = object : SpellCheckerSession.SpellCheckerSessionListener {
        override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
            // Sentence suggestions are used below; this callback is retained for API compatibility.
        }

        override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
            val suggestionInfo = results.orEmpty()
                .asSequence()
                .flatMap { sentence ->
                    (0 until sentence.suggestionsCount).asSequence()
                        .mapNotNull { sentence.getSuggestionsInfoAt(it) }
                }
                .firstOrNull { it.sequence == suggestionRequestSequence }

            val suggestions = if (suggestionInfo == null) {
                emptyList()
            } else {
                (0 until suggestionInfo.suggestionsCount.coerceAtLeast(0))
                    .mapNotNull { index -> suggestionInfo.getSuggestionAt(index) }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(3)
            }
            val isTypo = suggestionInfo != null &&
                suggestionInfo.getSuggestionsAttributes() and
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0

            Handler(Looper.getMainLooper()).post {
                if (suggestionInfo != null && composingWord.isNotEmpty() && !isPrivateEditor()) {
                    currentSuggestions = suggestions
                    currentWordIsTypo = isTypo
                    renderSuggestionStrip()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreateInputView(): View {
        buildKeyboard()
        return root ?: LinearLayout(this)
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!listenerRegistered) {
            clipboardManager.addPrimaryClipChangedListener(clipboardListener)
            listenerRegistered = true
        }
        captureCurrentClipboard()
        numberMode = false
        shifted = false
        capsLocked = false
        lastShiftTapAt = 0L
        composingWord.clear()
        currentSuggestions = emptyList()
        currentWordIsTypo = false
        ensureSpellChecker()
        showKeyboard()
    }

    override fun onWindowShown() {
        super.onWindowShown()
        ensureSpellChecker()
        showKeyboard()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        currentInputConnection?.finishComposingText()
        composingWord.clear()
        currentSuggestions = emptyList()
        currentWordIsTypo = false
        spellCheckerSession?.close()
        spellCheckerSession = null
        if (listenerRegistered) {
            clipboardManager.removePrimaryClipChangedListener(clipboardListener)
            listenerRegistered = false
        }
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        if (listenerRegistered) {
            clipboardManager.removePrimaryClipChangedListener(clipboardListener)
            listenerRegistered = false
        }
        spellCheckerSession?.close()
        spellCheckerSession = null
        super.onDestroy()
    }

    private fun isPrivateEditor(): Boolean {
        val info = currentInputEditorInfo ?: return false
        val inputClass = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        val passwordField =
            (inputClass == InputType.TYPE_CLASS_TEXT && variation in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            )) ||
            (inputClass == InputType.TYPE_CLASS_NUMBER &&
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        val noLearning = info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0
        return passwordField || noLearning
    }

    private fun captureCurrentClipboard() {
        if (isPrivateEditor()) return
        if (!ClipboardStore.isAutoCaptureEnabled(this)) return
        try {
            if (!clipboardManager.hasPrimaryClip()) return
            val clip = clipboardManager.primaryClip ?: return
            if (Build.VERSION.SDK_INT >= 33) {
                val sensitive = clip.description.extras
                    ?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true
                if (sensitive) return
            }
            val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return
            if (text.length > 5000) return
            ClipboardStore.capture(this, text)
        } catch (_: SecurityException) {
            // Clipboard access can be restricted by Android version or device policy.
        } catch (_: RuntimeException) {
            // Ignore clipboard provider failures without interrupting typing.
        }
    }

    private fun ensureSpellChecker() {
        if (spellCheckerSession != null || isPrivateEditor()) return
        val prefs = getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("suggestions_enabled", true)) return
        val manager = getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager ?: return
        val locale: Locale? = when (prefs.getString("keyboard_language", "system")) {
            "pt" -> Locale("pt")
            "en" -> Locale.ENGLISH
            else -> null
        }
        spellCheckerSession = try {
            manager.newSpellCheckerSession(null, locale, spellCheckerListener, true)
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun requestWordSuggestions() {
        val word = composingWord.toString()
        val prefs = getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
        if (word.length < 2 || isPrivateEditor() ||
            !prefs.getBoolean("suggestions_enabled", true)
        ) {
            currentSuggestions = emptyList()
            currentWordIsTypo = false
            renderSuggestionStrip()
            return
        }
        ensureSpellChecker()
        val session = spellCheckerSession
        if (session == null) {
            renderSuggestionStrip()
            return
        }
        val sequence = ++suggestionRequestSequence
        try {
            session.getSentenceSuggestions(arrayOf(TextInfo(word, 0, sequence)), 3)
        } catch (_: RuntimeException) {
            currentSuggestions = emptyList()
            currentWordIsTypo = false
            renderSuggestionStrip()
        }
    }

    private fun renderSuggestionStrip() {
        val strip = suggestionStrip ?: return
        strip.removeAllViews()
        if (composingWord.isEmpty() || isPrivateEditor()) {
            strip.visibility = View.GONE
            return
        }
        strip.visibility = View.VISIBLE
        val choices = (listOf(composingWord.toString()) + currentSuggestions)
            .distinct()
            .take(3)
        choices.forEachIndexed { index, candidate ->
            val item = label(candidate, 14, if (index == 0) textColor else muted, index == 0).apply {
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                setPadding(dp(5), 0, dp(5), 0)
                setOnClickListener { applySuggestion(candidate) }
            }
            strip.addView(item, LinearLayout.LayoutParams(0, dp(32), 1f))
        }
        while (strip.childCount < 3) {
            strip.addView(View(this), LinearLayout.LayoutParams(0, dp(32), 1f))
        }
    }

    private fun applySuggestion(candidate: String) {
        val connection = currentInputConnection ?: return
        connection.commitText(candidate, 1)
        composingWord.clear()
        currentSuggestions = emptyList()
        currentWordIsTypo = false
        renderSuggestionStrip()
    }

    private fun buildKeyboard() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(7))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(themeSurface(), themeBase())
            )
        }
        showKeyboard()
    }

    private fun showKeyboard() {
        val host = root ?: return
        host.animate().cancel()
        host.alpha = 0f
        host.removeAllViews()
        host.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(themeSurface(), themeBase())
        )

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        addTool(toolbar, "▤", "Área de transferência") { showClipboardPanel() }
        addTool(toolbar, "☺", "Emojis") { showEmojiPanel() }
        addTool(toolbar, "Aa", "Ferramentas") { showToolsPanel() }
        addTool(toolbar, "🎨", "Temas") { showThemePanel() }
        addTool(toolbar, "⚙", "Definições") { openSettings() }
        host.addView(toolbar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(38)
        ))
        suggestionStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(4), 0, dp(4), 0)
        }
        host.addView(suggestionStrip, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(32)
        ))
        renderSuggestionStrip()

        // Keep the key area taller by avoiding a static brand row.
        val keyboardPrefs = getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
        val showEmojiKey = keyboardPrefs.getBoolean("emoji_key_enabled", true)
        val showLanguageKey = keyboardPrefs.getBoolean("language_switch_key_enabled", false)
        val showCommaKey = keyboardPrefs.getBoolean("comma_key_enabled", true)
        val showPeriodKey = keyboardPrefs.getBoolean("period_key_enabled", true)
        val bottomUtilityKey = when {
            showEmojiKey -> "☺"
            showLanguageKey -> "🌐"
            else -> null
        }
        val bottomRow = listOfNotNull(
            if (numberMode) "ABC" else "?123",
            bottomUtilityKey,
            if (showCommaKey) "," else null,
            "ESPAÇO",
            if (showPeriodKey) "." else null,
            "↵"
        )

        val rows = if (numberMode) {
            mutableListOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/"),
                listOf("=", "*", "\"", "'", ":", ";", "!", "?", "⌫"),
                bottomRow
            )
        } else {
            val layout = keyboardPrefs.getString("keyboard_layout", "qwerty") ?: "qwerty"
            val letterRows = when (layout) {
                "qwertz" -> listOf(
                    listOf("q", "w", "e", "r", "t", "z", "u", "i", "o", "p"),
                    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                    listOf("⇧", "y", "x", "c", "v", "b", "n", "m", "⌫")
                )
                "azerty" -> listOf(
                    listOf("a", "z", "e", "r", "t", "y", "u", "i", "o", "p"),
                    listOf("q", "s", "d", "f", "g", "h", "j", "k", "l", "m"),
                    listOf("⇧", "w", "x", "c", "v", "b", "n", "⌫")
                )
                else -> listOf(
                    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                    listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫")
                )
            }
            (letterRows + listOf(bottomRow)).toMutableList()
        }
        if (!numberMode && keyboardPrefs.getBoolean("number_row_enabled", true)) {
            rows.add(0, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
        }

        rows.forEachIndexed { index, values ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            val height = when (prefsHeight()) {
                "compact" -> 40
                "tall" -> 53
                else -> 46
            }
            val isNumberRow = !numberMode &&
                keyboardPrefs.getBoolean("number_row_enabled", true) && index == 0
            val rowHeight = when {
                isNumberRow -> (height - 9).coerceAtLeast(30)
                index == rows.lastIndex -> height + 2
                else -> height
            }
            values.forEach { value ->
                val weight = when (value) {
                    "ESPAÇO" -> 3.1f
                    "?123", "ABC" -> 1.4f
                    "⇧", "⌫" -> 1.35f
                    "↵" -> 1.45f
                    "🌐" -> 1.05f
                    "," , "." -> 0.78f
                    else -> 1f
                }
                val button = keyButton(displayKey(value), isSpecial(value))
                button.layoutParams = LinearLayout.LayoutParams(0, dp(rowHeight), weight).apply {
                    setMargins(dp(2), dp(3), dp(2), dp(3))
                }
                button.setOnClickListener {
                    animatePress(button)
                    val hapticsEnabled = getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
                        .getBoolean("haptics_enabled", true)
                    if (hapticsEnabled) button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    val soundEnabled = getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
                        .getBoolean("sound_enabled", false)
                    if (soundEnabled) {
                        (getSystemService(Context.AUDIO_SERVICE) as AudioManager)
                            .playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
                    }
                    handleKey(value)
                }
                if (value == "ESPAÇO" && keyboardPrefs.getBoolean("cursor_gesture_enabled", true)) {
                    var gestureConsumed = false
                    var lastGestureX = 0f
                    button.setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                gestureConsumed = false
                                lastGestureX = event.rawX
                                false
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val delta = event.rawX - lastGestureX
                                val stepSize = dp(18).toFloat()
                                if (abs(delta) >= stepSize) {
                                    val steps = (abs(delta) / stepSize).toInt().coerceAtMost(8)
                                    val direction = if (delta < 0f) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
                                    repeat(steps) { moveCursor(direction) }
                                    lastGestureX += (if (delta < 0f) -1f else 1f) * steps * stepSize
                                    gestureConsumed = true
                                    view.parent?.requestDisallowInterceptTouchEvent(true)
                                    true
                                } else {
                                    gestureConsumed
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> gestureConsumed
                            else -> gestureConsumed
                        }
                    }
                }
                if (value == "⇧") {
                    button.setOnLongClickListener {
                        capsLocked = !capsLocked
                        shifted = capsLocked
                        lastShiftTapAt = 0L
                        showKeyboard()
                        true
                    }
                } else if (value == "⌫") {
                    button.setOnLongClickListener {
                        currentInputConnection?.finishComposingText()
                        composingWord.clear()
                        currentSuggestions = emptyList()
                        currentWordIsTypo = false
                        currentInputConnection?.deleteSurroundingText(30, 0)
                        renderSuggestionStrip()
                        true
                    }
                } else if (keyboardPrefs.getBoolean("long_press_symbols_enabled", true)) {
                    val number = mapOf(
                        "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
                        "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0"
                    )[value.lowercase()]
                    if (number != null) {
                        button.setOnLongClickListener {
                            currentInputConnection?.commitText(number, 1)
                            true
                        }
                    }
                }
                row.addView(button)
            }
            host.addView(row)
        }
        host.animate().alpha(1f).setDuration(150).start()
    }

    private fun handleKey(value: String) {
        val connection = currentInputConnection ?: return
        if (value != "ESPAÇO") lastSpacePressAt = 0L
        when (value) {
            "⇧" -> {
                val now = System.currentTimeMillis()
                if (lastShiftTapAt > 0L && now - lastShiftTapAt in 1L..450L) {
                    capsLocked = !capsLocked
                    shifted = capsLocked
                    lastShiftTapAt = 0L
                } else {
                    if (capsLocked) {
                        capsLocked = false
                        shifted = false
                    } else {
                        shifted = !shifted
                    }
                    lastShiftTapAt = now
                }
                showKeyboard()
            }
            "⌫" -> deleteBeforeCursor(connection)
            "?123" -> {
                numberMode = true
                showKeyboard()
            }
            "ABC" -> {
                numberMode = false
                showKeyboard()
            }
            "☺" -> showEmojiPanel()
            "🌐" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    switchToNextInputMethod(false)
                }
            }
            "ESPAÇO" -> {
                val prefs = getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
                val autoCorrect = prefs.getBoolean("auto_correct_enabled", false)
                val correction = if (autoCorrect && currentWordIsTypo) currentSuggestions.firstOrNull() else null
                if (correction != null && !isPrivateEditor()) {
                    connection.commitText(correction, 1)
                } else {
                    connection.finishComposingText()
                }
                composingWord.clear()
                currentSuggestions = emptyList()
                currentWordIsTypo = false
                renderSuggestionStrip()

                val now = System.currentTimeMillis()
                val previous = connection.getTextBeforeCursor(1, 0)?.toString().orEmpty()
                val doubleSpaceEnabled = prefs.getBoolean("double_space_period_enabled", true)
                if (doubleSpaceEnabled && !isPrivateEditor() &&
                    previous == " " && now - lastSpacePressAt in 1L..500L
                ) {
                    connection.deleteSurroundingText(1, 0)
                    connection.commitText(". ", 1)
                    lastSpacePressAt = 0L
                } else {
                    connection.commitText(" ", 1)
                    lastSpacePressAt = now
                }
                shifted = false
            }
            "↵" -> {
                connection.finishComposingText()
                composingWord.clear()
                currentSuggestions = emptyList()
                currentWordIsTypo = false
                renderSuggestionStrip()
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                    ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE && sendDefaultEditorAction(true)) return
                connection.commitText("\n", 1)
            }
            else -> {
                val inputType = currentInputEditorInfo?.inputType ?: 0
                val sentenceCapsEnabled =
                    inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0
                val beforeCursor = connection.getTextBeforeCursor(2, 0)?.toString().orEmpty()
                val shouldCapitalizeSentence = value.length == 1 && value[0].isLetter() &&
                    sentenceCapsEnabled &&
                    (beforeCursor.isEmpty() || beforeCursor.last() == '\n' ||
                        (beforeCursor.length >= 2 &&
                            beforeCursor[beforeCursor.lastIndex - 1] in ".!?" &&
                            beforeCursor.last() == ' '))
                val output = if (shifted || capsLocked || shouldCapitalizeSentence) value.uppercase() else value
                if (value.length == 1 && value[0].isLetter() && !isPrivateEditor()) {
                    composingWord.append(output)
                    connection.setComposingText(composingWord.toString(), 1)
                    requestWordSuggestions()
                } else {
                    connection.finishComposingText()
                    composingWord.clear()
                    currentSuggestions = emptyList()
                    currentWordIsTypo = false
                    connection.commitText(output, 1)
                    renderSuggestionStrip()
                }
                if (shifted && !capsLocked) {
                    shifted = false
                    showKeyboard()
                }
            }
        }
    }

    private fun deleteBeforeCursor(connection: InputConnection) {
        if (composingWord.isNotEmpty() && !isPrivateEditor()) {
            composingWord.deleteCharAt(composingWord.length - 1)
            if (composingWord.isEmpty()) {
                connection.setComposingText("", 1)
                connection.finishComposingText()
                currentSuggestions = emptyList()
                currentWordIsTypo = false
            } else {
                connection.setComposingText(composingWord.toString(), 1)
                requestWordSuggestions()
            }
            renderSuggestionStrip()
            return
        }
        val selected = connection.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            connection.commitText("", 1)
            return
        }
        val before = connection.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        val count = if (before.length >= 2 &&
            Character.isSurrogatePair(before[before.length - 2], before[before.length - 1])) 2 else 1
        connection.deleteSurroundingText(count, 0)
    }

    private fun showClipboardPanel() {
        val host = root ?: return
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(5))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("Área de transferência", 16, textColor, true), LinearLayout.LayoutParams(0, dp(40), 1f))
        header.addView(smallButton("Voltar") { showKeyboard() })
        panel.addView(header)

        val actions = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actionRow.addView(smallButton("Marcar tudo") {
            val allIds = ClipboardStore.all(this@NexInputMethodService).map { it.id }
            if (selectedClipIds.size == allIds.size) selectedClipIds.clear()
            else selectedClipIds.addAll(allIds)
            showClipboardPanel()
        })
        actionRow.addView(smallButton("Fixar") {
            if (selectedClipIds.isNotEmpty()) {
                val all = ClipboardStore.all(this@NexInputMethodService)
                val selected = all.filter { it.id in selectedClipIds }
                val shouldPin = selected.any { !it.pinned }
                ClipboardStore.setPinned(this@NexInputMethodService, selectedClipIds, shouldPin)
                selectedClipIds.clear()
                showClipboardPanel()
            }
        })
        actionRow.addView(smallButton("Eliminar") {
            if (selectedClipIds.isNotEmpty()) {
                ClipboardStore.delete(this@NexInputMethodService, selectedClipIds)
                selectedClipIds.clear()
                showClipboardPanel()
            }
        })
        actionRow.addView(smallButton("Limpar tudo") {
            AlertDialog.Builder(this@NexInputMethodService)
                .setTitle("Limpar área de transferência?")
                .setMessage("Todos os itens guardados, incluindo os fixados, serão eliminados.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Limpar") { _, _ ->
                    ClipboardStore.clear(this@NexInputMethodService)
                    selectedClipIds.clear()
                    showClipboardPanel()
                }
                .show()
        })
        actions.addView(actionRow)
        panel.addView(actions)

        val list = ScrollView(this)
        val items = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(5), 0, dp(4))
        }
        val clips = ClipboardStore.all(this)
        selectedClipIds.retainAll(clips.map { it.id }.toSet())

        if (clips.isEmpty()) {
            items.addView(label("Ainda não há textos guardados. Copie algum texto e volte a abrir o teclado.", 13, muted).apply {
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(24), dp(18), dp(24))
            })
        } else {
            clips.forEach { clip ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(7), dp(7), dp(7), dp(7))
                    background = rounded(keyColor, 14)
                }
                val check = CheckBox(this).apply {
                    buttonTintList = ColorStateList.valueOf(themeAccent())
                    isChecked = clip.id in selectedClipIds
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) selectedClipIds.add(clip.id) else selectedClipIds.remove(clip.id)
                    }
                }
                card.addView(check)
                val body = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(dp(4), 0, dp(4), 0)
                    isClickable = true
                    setOnClickListener {
                        currentInputConnection?.commitText(clip.text, 1)
                        selectedClipIds.clear()
                        showKeyboard()
                    }
                }
                body.addView(label(clip.text, 13, textColor, true).apply {
                    maxLines = 3
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
                body.addView(label(
                    if (clip.pinned) "Fixado" else expiryLabel(clip.expiresAt),
                    10,
                    if (clip.pinned) themeAccent() else muted
                ).apply { setPadding(0, dp(3), 0, 0) })
                card.addView(body)
                card.addView(smallButton("✎") { showEditPanel(clip.id, clip.text) })
                card.addView(smallButton(if (clip.pinned) "📌" else "＋") {
                    ClipboardStore.setPinned(this@NexInputMethodService, setOf(clip.id), !clip.pinned)
                    showClipboardPanel()
                })
                items.addView(card, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(5) })
            }
        }

        list.addView(items)
        panel.addView(list, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        panel.addView(label("Os itens não fixados são apagados após o tempo definido nas definições.", 10, muted).apply {
            setPadding(dp(3), dp(4), dp(3), dp(2))
        })
        transitionPanel(panel)
    }

    private fun showEditPanel(id: String, original: String) {
        val host = root ?: return
        host.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        panel.addView(label("Editar texto guardado", 16, textColor, true))
        val editor = EditText(this).apply {
            setText(original)
            setTextColor(textColor)
            setHintTextColor(muted)
            textSize = 14f
            gravity = Gravity.TOP
            minLines = 3
            maxLines = 6
            setPadding(dp(10), dp(9), dp(10), dp(9))
            background = rounded(keyColor, 12)
        }
        panel.addView(editor, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { topMargin = dp(8); bottomMargin = dp(8) })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(smallButton("Cancelar") { showClipboardPanel() })
        buttons.addView(smallButton("Guardar") {
            ClipboardStore.updateText(this@NexInputMethodService, id, editor.text.toString())
            showClipboardPanel()
        })
        panel.addView(buttons)
        transitionPanel(panel)
    }

    private fun showEmojiPanel() {
        val host = root ?: return
        val catalog = try {
            EmojiCatalog.load(this)
        } catch (_: Exception) {
            emptyList()
        }
        host.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), dp(4), dp(7), dp(5))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("Emojis", 16, textColor, true),
            LinearLayout.LayoutParams(0, dp(36), 1f))
        header.addView(smallButton("+ Criar") { showCustomEmojiCreator() })
        header.addView(smallButton("Voltar") { showKeyboard() })
        panel.addView(header)

        val search = EditText(this).apply {
            hint = "Pesquisar em português ou inglês…"
            setSingleLine(true)
            textSize = 13f
            setTextColor(textColor)
            setHintTextColor(muted)
            setPadding(dp(12), 0, dp(12), 0)
            background = rounded(keyColor, 13)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_CLASS_TEXT
        }
        panel.addView(search, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(42)
        ).apply { bottomMargin = dp(4) })

        var activeCategory = "Recentes"
        var visibleCount = 192
        var showAllTones = false
        val categoryNames = listOf(
            "Recentes", "Favoritos", "NexaMoji", "Criados",
            "Emojis e emoções", "Pessoas e corpo", "Animais e natureza",
            "Comida e bebidas", "Viagens e lugares", "Atividades",
            "Objetos", "Símbolos", "Bandeiras", "Tons de pele e cabelo"
        )
        val categoryScroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val categoryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        categoryScroller.addView(categoryRow)
        panel.addView(categoryScroller)

        val status = label("${catalog.size} emojis · Unicode 18.0", 10, muted).apply {
            setPadding(dp(4), dp(2), dp(4), dp(3))
        }
        panel.addView(status)

        val contentScroller = ScrollView(this)
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, dp(4))
        }
        contentScroller.addView(grid)
        panel.addView(contentScroller, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val customPack = listOf(
            EmojiEntry("💜✨", "NexaMoji", "Identidade", "Nexauren glow", "Brilho Nexauren", "roxo marca brilho identidade"),
            EmojiEntry("🚀💡", "NexaMoji", "Criatividade", "Idea launch", "Ideia em órbita", "criatividade ideia projeto inovação"),
            EmojiEntry("🎧🔥", "NexaMoji", "Música", "Sound on fire", "Som no máximo", "audio música headphones"),
            EmojiEntry("🧠⚡", "NexaMoji", "Criatividade", "Electric mind", "Mente elétrica", "inteligência foco"),
            EmojiEntry("📚☕", "NexaMoji", "Estudo", "Study time", "Hora de estudar", "livros escola aprendizagem"),
            EmojiEntry("🦋🌸", "NexaMoji", "Natureza", "Bloom", "Florescer", "borboleta flor primavera"),
            EmojiEntry("🎯🏆", "NexaMoji", "Conquistas", "Goal achieved", "Objetivo alcançado", "sucesso vitória"),
            EmojiEntry("🪘🎹🔥", "NexaMoji", "Música", "Amapiano vibe", "Vibe Amapiano", "amapiano música ritmo"),
            EmojiEntry("🛠️💜", "NexaMoji", "Criação", "Made with care", "Feito com cuidado", "criar construir desenvolver"),
            EmojiEntry("🌙✨", "NexaMoji", "Noite", "Night sparkle", "Noite brilhante", "lua estrelas"),
            EmojiEntry("🌍🤝", "NexaMoji", "Pessoas", "One world", "Um só mundo", "mundo união amizade"),
            EmojiEntry("🙏💛", "NexaMoji", "Gratidão", "Thankful", "Gratidão", "obrigado agradecimento"),
            EmojiEntry("😊💫", "NexaMoji", "Alegria", "Good energy", "Boa energia", "feliz alegria"),
            EmojiEntry("💻🚀", "NexaMoji", "Tecnologia", "Build and launch", "Criar e lançar", "tecnologia aplicativo"),
            EmojiEntry("🎉🥳", "NexaMoji", "Festa", "Big celebration", "Grande celebração", "festa parabéns"),
            EmojiEntry("🌧️💙", "NexaMoji", "Emoções", "Blue mood", "Dia azul", "triste saudade"),
            EmojiEntry("❤️‍🔥🌹", "NexaMoji", "Amor", "Passion", "Paixão", "amor coração"),
            EmojiEntry("💡🧩", "NexaMoji", "Ideias", "Piece by piece", "Peça por peça", "solução ideia"),
            EmojiEntry("⚡🎮", "NexaMoji", "Diversão", "Power play", "Jogo poderoso", "gaming jogo"),
            EmojiEntry("🌱🌳", "NexaMoji", "Natureza", "Growing", "A crescer", "crescer natureza")
        )
        val catalogByEmoji = catalog.associateBy { it.emoji }

        fun customEntries(): List<EmojiEntry> {
            val raw = getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
                .getString("custom_emoji_entries", "[]").orEmpty()
            return try {
                val array = JSONArray(raw)
                (0 until array.length()).mapNotNull { index ->
                    val value = array.optJSONObject(index) ?: return@mapNotNull null
                    val emoji = value.optString("emoji")
                    val name = value.optString("name")
                    if (emoji.isBlank() || name.isBlank()) null else EmojiEntry(
                        emoji, "Criados", "Personalizados", name, name, "criado personalizado meu"
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun categoryItems(category: String): List<EmojiEntry> {
            val entries = when (category) {
                "Recentes" -> readStoredEmojiList("recent_emojis").mapNotNull {
                    catalogByEmoji[it] ?: customPack.firstOrNull { item -> item.emoji == it }
                        ?: customEntries().firstOrNull { item -> item.emoji == it }
                }
                "Favoritos" -> readStoredEmojiList("emoji_favorites").mapNotNull {
                    catalogByEmoji[it] ?: customPack.firstOrNull { item -> item.emoji == it }
                        ?: customEntries().firstOrNull { item -> item.emoji == it }
                }
                "NexaMoji" -> customPack
                "Criados" -> customEntries()
                else -> catalog.filter { it.group == category }
            }
            return if (showAllTones || category == "Tons de pele e cabelo") entries
                else entries.filterNot { containsEmojiSkinTone(it.emoji) }
        }

        lateinit var renderGrid: () -> Unit
        fun buildCategoryTabs() {
            categoryRow.removeAllViews()
            categoryNames.forEach { name ->
                categoryRow.addView(smallButton(
                    if (activeCategory == name) "• $name" else name
                ) {
                    activeCategory = name
                    visibleCount = 192
                    renderGrid()
                    buildCategoryTabs()
                })
            }
        }

        renderGrid = {
            grid.removeAllViews()
            val query = search.text?.toString()?.trim()?.lowercase().orEmpty()
            val results = if (query.isNotEmpty()) {
                val all = catalog + customPack + customEntries()
                val matches = all.filter { it.searchText.contains(query) || it.emoji.contains(query) }
                if (showAllTones) matches else matches.filterNot { containsEmojiSkinTone(it.emoji) }
            } else {
                categoryItems(activeCategory)
            }
            status.text = if (query.isNotEmpty()) {
                "${results.size} resultados para “$query”"
            } else {
                when (activeCategory) {
                    "Recentes" -> "Emojis usados recentemente"
                    "Favoritos" -> "Os teus emojis favoritos"
                    "NexaMoji" -> "Combinações criadas pela Nexauren"
                    "Criados" -> "As tuas combinações personalizadas"
                    else -> "${results.size} emojis nesta categoria"
                }
            }
            if (results.isEmpty()) {
                grid.addView(label(
                    when {
                        query.isNotEmpty() -> "Não foram encontrados emojis. Experimenta outra palavra."
                        activeCategory == "Recentes" -> "Ainda não usaste emojis. Começa por escolher um."
                        activeCategory == "Favoritos" -> "Mantém um emoji premido para o guardar nos favoritos."
                        activeCategory == "Criados" -> "Ainda não criaste combinações. Toca em + Criar."
                        else -> "Esta categoria ainda não tem emojis disponíveis."
                    }, 13, muted
                ).apply {
                    gravity = Gravity.CENTER
                    setPadding(dp(18), dp(28), dp(18), dp(28))
                })
            } else {
                val visible = results.take(visibleCount)
                visible.chunked(8).forEach { group ->
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                    }
                    group.forEach { entry ->
                        val button = keyButton(entry.emoji, false).apply {
                            textSize = if (entry.emoji.length > 5) 20f else 23f
                            contentDescription = entry.portugueseName
                            layoutParams = LinearLayout.LayoutParams(0, dp(49), 1f).apply {
                                setMargins(dp(2), dp(2), dp(2), dp(2))
                            }
                            setOnClickListener {
                                rememberEmoji(entry.emoji)
                                currentInputConnection?.commitText(entry.emoji, 1)
                                animatePress(this)
                            }
                            setOnLongClickListener {
                                showEmojiItemOptions(entry, catalog, customPack)
                                true
                            }
                        }
                        row.addView(button)
                    }
                    grid.addView(row)
                }
                if (results.size > visibleCount) {
                    grid.addView(smallButton("Mostrar mais · ${results.size - visibleCount} restantes") {
                        visibleCount = (visibleCount + 192).coerceAtMost(results.size)
                        renderGrid()
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(38)
                        ).apply { topMargin = dp(5) }
                    })
                }
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                visibleCount = 192
                renderGrid()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tools.addView(smallButton("Mostrar todos os tons") {
            showAllTones = !showAllTones
            status.text = if (showAllTones) "A mostrar também as variações de tons de pele" else "A mostrar as versões base dos emojis"
            visibleCount = 192
            renderGrid()
        })
        tools.addView(label("Mantém premido para tons ou favoritos", 10, muted).apply {
            setPadding(dp(5), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, dp(34), 1f))
        panel.addView(tools)

        buildCategoryTabs()
        renderGrid()
        host.addView(panel)
    }

    private fun containsEmojiSkinTone(value: String): Boolean {
        var index = 0
        while (index < value.length) {
            val codePoint = Character.codePointAt(value, index)
            if (codePoint in 0x1F3FB..0x1F3FF) return true
            index += Character.charCount(codePoint)
        }
        return false
    }

    private fun rememberEmoji(emoji: String) {
        val recent = readStoredEmojiList("recent_emojis")
        val updated = (listOf(emoji) + recent.filterNot { it == emoji }).take(40)
        saveStoredEmojiList("recent_emojis", updated)
    }

    private fun toggleEmojiFavorite(emoji: String) {
        val favourites = readStoredEmojiList("emoji_favorites").toMutableList()
        if (emoji in favourites) favourites.remove(emoji) else favourites.add(0, emoji)
        saveStoredEmojiList("emoji_favorites", favourites)
    }

    private fun readStoredEmojiList(key: String): List<String> {
        val raw = getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
            .getString(key, "[]").orEmpty()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { array.optString(it).takeIf { value -> value.isNotBlank() } }
        } catch (_: Exception) {
            raw.split("|").filter { it.isNotBlank() }
        }
    }

    private fun saveStoredEmojiList(key: String, values: List<String>) {
        val array = JSONArray()
        values.distinct().forEach { array.put(it) }
        getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
            .edit().putString(key, array.toString()).apply()
    }

    private fun showEmojiItemOptions(entry: EmojiEntry, catalog: List<EmojiEntry>, customPack: List<EmojiEntry>) {
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()
        options.add("Inserir ${entry.emoji}")
        actions.add {
            rememberEmoji(entry.emoji)
            currentInputConnection?.commitText(entry.emoji, 1)
        }

        val favourite = entry.emoji in readStoredEmojiList("emoji_favorites")
        options.add(if (favourite) "Remover dos favoritos" else "Adicionar aos favoritos")
        actions.add {
            toggleEmojiFavorite(entry.emoji)
            Toast.makeText(
                this,
                if (favourite) "Removido dos favoritos" else "Adicionado aos favoritos",
                Toast.LENGTH_SHORT
            ).show()
        }

        val toneVariants = catalog.filter {
            it.toneBase == entry.toneBase && containsEmojiSkinTone(it.emoji)
        }.distinctBy { it.emoji }.take(30)
        if (toneVariants.isNotEmpty()) {
            options.add("Escolher tom de pele (${toneVariants.size} opções)")
            actions.add {
                val labels = toneVariants.map { "${it.emoji}  ${it.portugueseName}" }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle("Escolher tom de pele")
                    .setItems(labels) { _, which ->
                        val emoji = toneVariants[which].emoji
                        rememberEmoji(emoji)
                        currentInputConnection?.commitText(emoji, 1)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
        if (entry.group == "Criados") {
            options.add("Eliminar combinação")
            actions.add {
                deleteCustomEmoji(entry.emoji, entry.portugueseName)
                showEmojiPanel()
            }
        }
        AlertDialog.Builder(this)
            .setTitle(entry.portugueseName)
            .setItems(options.toTypedArray()) { _, which -> actions[which]() }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun deleteCustomEmoji(emoji: String, name: String) {
        val prefs = getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
        val raw = prefs.getString("custom_emoji_entries", "[]").orEmpty()
        val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        val kept = JSONArray()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            if (item.optString("emoji") != emoji || item.optString("name") != name) kept.put(item)
        }
        prefs.edit().putString("custom_emoji_entries", kept.toString()).apply()
    }

    private fun showEmojiCatalogSelector(onSelected: (String) -> Unit) {
        val catalog = try { EmojiCatalog.load(this) } catch (_: Exception) { emptyList() }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(7), dp(10), dp(7))
        }
        val search = EditText(this).apply {
            hint = "Pesquisar todos os emojis…"
            setSingleLine(true)
            textSize = 13f
            setTextColor(textColor)
            setHintTextColor(muted)
            setPadding(dp(10), 0, dp(10), 0)
            background = rounded(keyColor, 12)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_CLASS_TEXT
        }
        content.addView(search, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(42)
        ))
        val status = label("${catalog.size} emojis disponíveis", 10, muted).apply {
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        content.addView(status)
        val scroll = ScrollView(this)
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(grid)
        content.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(330)
        ))
        var visibleCount = 160
        lateinit var render: () -> Unit
        render = {
            grid.removeAllViews()
            val query = search.text?.toString()?.trim()?.lowercase().orEmpty()
            val results = if (query.isBlank()) catalog else catalog.filter {
                it.searchText.contains(query) || it.emoji.contains(query)
            }
            status.text = if (query.isBlank()) {
                "${results.size} emojis · escolhe um para adicionar"
            } else {
                "${results.size} resultados"
            }
            results.take(visibleCount).chunked(8).forEach { group ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                group.forEach { entry ->
                    val button = keyButton(entry.emoji, false).apply {
                        textSize = 22f
                        contentDescription = entry.portugueseName
                        layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                            setMargins(dp(2), dp(2), dp(2), dp(2))
                        }
                        setOnClickListener {
                            onSelected(entry.emoji)
                            dialog.dismiss()
                        }
                    }
                    row.addView(button)
                }
                grid.addView(row)
            }
            if (results.size > visibleCount) {
                grid.addView(smallButton("Mostrar mais") {
                    visibleCount = (visibleCount + 160).coerceAtMost(results.size)
                    render()
                })
            }
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Escolher do catálogo completo")
            .setView(content)
            .setNegativeButton("Fechar", null)
            .create()
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                visibleCount = 160
                render()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        render()
        dialog.show()
    }

    private fun showCustomEmojiCreator() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        val nameInput = EditText(this).apply {
            hint = "Nome da combinação"
            setSingleLine(true)
            textSize = 14f
            setTextColor(textColor)
            setHintTextColor(muted)
        }
        content.addView(nameInput)
        val selectedEmojis = mutableListOf<String>()
        val preview = TextView(this).apply {
            text = "Escolhe os emojis abaixo"
            textSize = 25f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }
        content.addView(preview)
        val quickEmojis = listOf(
            "😀","😂","🥰","😍","😎","🥳","😭","🤔",
            "👋","👍","👏","🙏","🤝","💪","❤️","💜",
            "💙","💛","🔥","✨","⭐","🌸","🦋","🌍",
            "🎧","🎵","🎹","📚","💡","🚀","🏆","🎉",
            "🌙","☀️","🍀","🌈","💻","🪘","🎮","☕"
        )
        val scroll = ScrollView(this)
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        quickEmojis.chunked(8).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { emoji ->
                val button = keyButton(emoji, false).apply {
                    textSize = 21f
                    layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    }
                    setOnClickListener {
                        if (selectedEmojis.joinToString("").length < 40) selectedEmojis.add(emoji)
                        preview.text = selectedEmojis.joinToString("").ifEmpty { "Escolhe os emojis abaixo" }
                    }
                }
                row.addView(button)
            }
            grid.addView(row)
        }
        scroll.addView(grid)
        content.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(250)
        ))
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(smallButton("Apagar último") {
            if (selectedEmojis.isNotEmpty()) {
                selectedEmojis.removeAt(selectedEmojis.lastIndex)
                preview.text = selectedEmojis.joinToString("").ifEmpty { "Escolhe os emojis abaixo" }
            }
        })
        controls.addView(smallButton("Limpar") {
            selectedEmojis.clear()
            preview.text = "Escolhe os emojis abaixo"
        })
        controls.addView(smallButton("Catálogo completo") {
            showEmojiCatalogSelector { emoji ->
                if (selectedEmojis.joinToString("").length < 40) selectedEmojis.add(emoji)
                preview.text = selectedEmojis.joinToString("").ifEmpty { "Escolhe os emojis abaixo" }
            }
        })
        content.addView(controls)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Criar NexaMoji")
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text.toString().trim()
                val emoji = selectedEmojis.joinToString("")
                if (name.isBlank() || emoji.isBlank()) {
                    Toast.makeText(this, "Escolhe um nome e pelo menos um emoji.", Toast.LENGTH_SHORT).show()
                } else {
                    val prefs = getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
                    val current = try {
                        JSONArray(prefs.getString("custom_emoji_entries", "[]").orEmpty())
                    } catch (_: Exception) {
                        JSONArray()
                    }
                    val updated = JSONArray()
                    for (index in 0 until current.length()) {
                        val item = current.optJSONObject(index) ?: continue
                        if (item.optString("name") != name) updated.put(item)
                    }
                    updated.put(JSONObject().put("name", name).put("emoji", emoji))
                    prefs.edit().putString("custom_emoji_entries", updated.toString()).apply()
                    dialog.dismiss()
                    Toast.makeText(this, "NexaMoji guardado.", Toast.LENGTH_SHORT).show()
                    showEmojiPanel()
                }
            }
        }
        dialog.show()
    }

    private fun showToolsPanel() {
        val host = root ?: return
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(7), dp(12), dp(8))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("Ferramentas rápidas", 16, textColor, true),
            LinearLayout.LayoutParams(0, dp(40), 1f))
        header.addView(smallButton("Voltar") { showKeyboard() })
        panel.addView(header)

        val editRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        editRow.addView(smallButton("Editar texto") { showTextEditingPanel() })
        editRow.addView(smallButton("Área de transferência") { showClipboardPanel() })
        panel.addView(editRow)

        val quickRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        quickRow.addView(smallButton("Emojis") { showEmojiPanel() })
        quickRow.addView(smallButton("Temas") { showThemePanel() })
        quickRow.addView(smallButton("Definições") { openSettings() })
        panel.addView(quickRow)

        panel.addView(label(
            "As ações de edição funcionam no campo de texto ativo. O teclado não recolhe o conteúdo para servidores.",
            11, muted
        ).apply { setPadding(dp(4), dp(10), dp(4), 0) })
        transitionPanel(panel)
    }

    private fun showTextEditingPanel() {
        val host = root ?: return
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(7), dp(12), dp(8))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("Edição de texto", 16, textColor, true),
            LinearLayout.LayoutParams(0, dp(40), 1f))
        header.addView(smallButton("Voltar") { showToolsPanel() })
        panel.addView(header)

        val connection = currentInputConnection
        val selectRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        selectRow.addView(smallButton("Selecionar tudo") {
            currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
        })
        selectRow.addView(smallButton("Copiar") {
            currentInputConnection?.performContextMenuAction(android.R.id.copy)
        })
        selectRow.addView(smallButton("Cortar") {
            currentInputConnection?.performContextMenuAction(android.R.id.cut)
        })
        selectRow.addView(smallButton("Colar") {
            currentInputConnection?.performContextMenuAction(android.R.id.paste)
        })
        panel.addView(selectRow)

        val cursorTitle = label("Mover cursor", 13, muted, true).apply {
            setPadding(dp(4), dp(11), dp(4), dp(3))
        }
        panel.addView(cursorTitle)
        val cursorRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cursorRow.addView(smallButton("← Esquerda") { moveCursor(KeyEvent.KEYCODE_DPAD_LEFT) })
        cursorRow.addView(smallButton("Direita →") { moveCursor(KeyEvent.KEYCODE_DPAD_RIGHT) })
        cursorRow.addView(smallButton("Apagar palavra") {
            currentInputConnection?.deleteSurroundingText(30, 0)
        })
        panel.addView(cursorRow)

        val caseRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        caseRow.addView(smallButton("MAIÚSCULAS") {
            val selected = currentInputConnection?.getSelectedText(0)?.toString()
            if (!selected.isNullOrEmpty()) currentInputConnection?.commitText(selected.uppercase(), 1)
        })
        caseRow.addView(smallButton("minúsculas") {
            val selected = currentInputConnection?.getSelectedText(0)?.toString()
            if (!selected.isNullOrEmpty()) currentInputConnection?.commitText(selected.lowercase(), 1)
        })
        panel.addView(caseRow)

        panel.addView(label(
            if (connection == null) "Nenhum campo de texto está ativo." else
                "Seleciona primeiro um texto para usar as opções de cópia e maiúsculas.",
            11, muted
        ).apply { setPadding(dp(4), dp(9), dp(4), 0) })
        transitionPanel(panel)
    }

    private fun moveCursor(keyCode: Int) {
        val connection = currentInputConnection ?: return
        if (composingWord.isNotEmpty()) {
            connection.finishComposingText()
            composingWord.clear()
            currentSuggestions = emptyList()
            currentWordIsTypo = false
            renderSuggestionStrip()
        }
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun showThemePanel() {
        val host = root ?: return
        host.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("Temas", 16, textColor, true), LinearLayout.LayoutParams(0, dp(40), 1f))
        header.addView(smallButton("Voltar") { showKeyboard() })
        panel.addView(header)
        listOf("Neon Violet", "Midnight Blue", "Ocean Cyan", "Minimal Light").forEach { theme ->
            panel.addView(smallButton(theme) {
                getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
                    .edit().putString("theme_name", theme).apply()
                showKeyboard()
            })
        }
        host.addView(panel)
    }


    private fun transitionPanel(panel: View) {
        val host = root ?: return
        host.animate().cancel()
        host.animate().alpha(0f).setDuration(85).withEndAction {
            host.removeAllViews()
            host.addView(
                panel,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
            host.alpha = 0f
            host.animate().alpha(1f).setDuration(165).start()
        }.start()
    }

    private fun openSettings() {
        val intent = android.content.Intent(this, com.nexauren.keyboardnex.SettingsActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun addTool(row: LinearLayout, symbol: String, description: String, action: () -> Unit) {
        val item = label(symbol, 20, textColor, true).apply {
            gravity = Gravity.CENTER
            contentDescription = description
            isClickable = true
            isFocusable = true
            setOnClickListener {
                animatePress(this)
                action()
            }
        }
        row.addView(item, LinearLayout.LayoutParams(0, dp(40), 1f))
    }

    private fun smallButton(value: String, action: () -> Unit) =
        TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(6), dp(9), dp(6))
            background = rounded(themeAccent(), 11)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                animatePress(this)
                action()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(34)
            ).apply { setMargins(dp(3), dp(2), dp(3), dp(2)) }
        }

    private fun keyButton(value: String, special: Boolean) =
        TextView(this).apply {
            text = value
            textSize = if (value == "ESPAÇO") 11f else 17f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = rounded(if (special) themeAccent() else keyColor, 9)
            isClickable = true
            isFocusable = true
        }

    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

    private fun rounded(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun displayKey(value: String): String = when (value) {
        "ESPAÇO" -> "Espaço"
        else -> if ((shifted || capsLocked) && value.length == 1 && value[0].isLetter()) value.uppercase() else value
    }

    private fun isSpecial(value: String) =
        value in setOf("⇧", "⌫", "?123", "ABC", "↵", "☺", "🌐", "ESPAÇO")

    private fun animatePress(view: View) {
        view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(55).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(85).start()
        }.start()
    }

    private fun expiryLabel(expiresAt: Long?): String {
        if (expiresAt == null) return "Sem expiração"
        val remaining = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val minutes = (remaining / 60_000L).coerceAtLeast(1L)
        return if (minutes < 60L) "Expira em " + minutes + " min" else "Expira em " + (minutes / 60L) + " h"
    }

    private fun prefsHeight(): String =
        getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
            .getString("keyboard_height", "normal") ?: "normal"

    private fun themeName(): String =
        getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
            .getString("theme_name", "Neon Violet") ?: "Neon Violet"

    private fun themeAccent(): Int = when (themeName()) {
        "Midnight Blue" -> Color.rgb(52, 96, 212)
        "Ocean Cyan" -> Color.rgb(0, 157, 183)
        "Minimal Light" -> Color.rgb(104, 111, 135)
        else -> Color.rgb(105, 76, 245)
    }

    private fun themeSurface(): Int = when (themeName()) {
        "Midnight Blue" -> Color.rgb(15, 24, 50)
        "Ocean Cyan" -> Color.rgb(8, 39, 50)
        "Minimal Light" -> Color.rgb(226, 230, 243)
        else -> Color.rgb(27, 20, 62)
    }

    private fun themeBase(): Int = when (themeName()) {
        "Midnight Blue" -> Color.rgb(8, 14, 32)
        "Ocean Cyan" -> Color.rgb(5, 22, 31)
        "Minimal Light" -> Color.rgb(202, 209, 228)
        else -> Color.rgb(12, 10, 31)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
