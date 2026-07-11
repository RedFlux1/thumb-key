package com.dessalines.thumbkey

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.InputMethodService.Insets
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dessalines.thumbkey.db.DEFAULT_CLIPBOARD_HISTORY_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_DISABLE_FULLSCREEN_EDITOR
import com.dessalines.thumbkey.db.DEFAULT_SHOW_ON_SCREEN_KEYBOARD
import com.dessalines.thumbkey.db.DEFAULT_USE_PRIVATE_CLIPBOARD
import com.dessalines.thumbkey.db.FloatingModeConfig
import com.dessalines.thumbkey.utils.KeyboardDefinition
import com.dessalines.thumbkey.utils.KeyboardLayout
import com.dessalines.thumbkey.utils.KeyboardMode
import com.dessalines.thumbkey.utils.TAG
import com.dessalines.thumbkey.utils.ThumbKeyClipboardManager
import com.dessalines.thumbkey.utils.toBool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface FloatingModeController {
    fun onDrag(dx: Float, dy: Float)
    fun onDragEnd()
    fun anchorToBottom()
    fun minimize()
}

class IMEService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    FloatingModeController {

    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var floatingView: ComposeKeyboardView? = null
    private fun setupView(): ComposeKeyboardView {
        val app = application as ThumbkeyApplication
        val settingsRepo = app.appSettingsRepository
        val clipboardRepo = app.clipboardRepository

        val layoutIndex = settingsRepo.appSettings.value?.keyboardLayout
        if (layoutIndex != null) {
            currentKeyboardDefinition = KeyboardLayout.entries[layoutIndex].keyboardDefinition
        }

        // Pass a pending mode (set by emoji/clipboard toggle while floating)
        // so the new docked keyboard starts in the correct mode.
        val initialMode = pendingDockedMode
        pendingDockedMode = null

        val view = ComposeKeyboardView(this, settingsRepo, clipboardRepo, initialMode)
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        view.let {
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
        }
        return view
    }

    var currentKeyboardDefinition: KeyboardDefinition? = null
    private var clipboardManager: ThumbKeyClipboardManager? = null

    internal fun isFloatingModeEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val app = application as ThumbkeyApplication
        val config = app.floatingModeRepository.getConfigSync()
        return (config?.enabled == 1) && Settings.canDrawOverlays(this)
    }

    /** True when floating mode is enabled AND the user chose to keep emoji/clipboard
     *  views inside the floating window instead of temporarily docking. */
    internal fun isExpandInFloatingEnabled(): Boolean {
        if (!isFloatingModeEnabled()) return false
        val app = application as ThumbkeyApplication
        val config = app.floatingModeRepository.getConfigSync()
        return config?.expandInFloating == 1
    }

    /** Non-null when the next docked [KeyboardScreen] should start in a specific mode. */
    internal var pendingDockedMode: KeyboardMode? = null

    /** True when the user was floating before temporarily docking for emoji/clipboard. */
    internal var shouldRevertToFloating: Boolean = false

    private fun showFloating() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (floatingView != null) return

        shouldRevertToFloating = false

        val view = setupView()
        val app = application as ThumbkeyApplication
        val config = app.floatingModeRepository.getConfigSync() ?: FloatingModeConfig()

        val params = WindowManager.LayoutParams(
            if (config.width > 0) config.width else WindowManager.LayoutParams.WRAP_CONTENT,
            if (config.height > 0) config.height else WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = config.x
            y = config.y
            alpha = config.opacity / 100f
        }

        try {
            windowManager.addView(view, params)
            floatingView = view
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating view: $e")
        }
    }

    private fun hideFloating() {
        floatingView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating view: $e")
            }
        }
        floatingView = null
    }

    override fun onDrag(dx: Float, dy: Float) {
        val view = floatingView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        params.x += dx.toInt()
        params.y += dy.toInt()

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        params.x = params.x.coerceIn(0, screenWidth - view.width)
        params.y = params.y.coerceIn(0, screenHeight - view.height)

        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update view layout on drag: $e")
        }
    }

    override fun onDragEnd() {
        val view = floatingView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        val app = application as ThumbkeyApplication
        lifecycleScope.launch(Dispatchers.IO) {
            val repo = app.floatingModeRepository
            val current = repo.getConfigSync() ?: FloatingModeConfig()
            repo.upsert(current.copy(x = params.x, y = params.y))
        }
    }

    override fun anchorToBottom() {
        hideFloating()
        val app = application as ThumbkeyApplication
        lifecycleScope.launch(Dispatchers.IO) {
            val repo = app.floatingModeRepository
            val current = repo.getConfigSync() ?: FloatingModeConfig()
            repo.upsert(current.copy(enabled = 0))
        }
        setInputView(setupView())
    }

    override fun minimize() {
        hideFloating()
        val app = application as ThumbkeyApplication
        lifecycleScope.launch(Dispatchers.IO) {
            val repo = app.floatingModeRepository
            val current = repo.getConfigSync() ?: FloatingModeConfig()
            repo.upsert(current.copy(enabled = 0))
        }
        requestHideSelf(0)
    }

    fun toggleFloatingMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val app = application as ThumbkeyApplication
        if (isFloatingModeEnabled()) {
            // Currently floating -> switch to docked
            hideFloating()
            lifecycleScope.launch(Dispatchers.IO) {
                val repo = app.floatingModeRepository
                val current = repo.getConfigSync() ?: FloatingModeConfig()
                repo.upsert(current.copy(enabled = 0))
            }
            setInputView(setupView())
        } else {
            // Currently docked -> switch to floating
            lifecycleScope.launch(Dispatchers.IO) {
                val repo = app.floatingModeRepository
                val current = repo.getConfigSync() ?: FloatingModeConfig()
                repo.upsert(current.copy(enabled = 1))
            }
            val dummyView = View(this)
            dummyView.layoutParams = ViewGroup.LayoutParams(0, 0)
            setInputView(dummyView)
            showFloating()
        }
    }

    /**
     * This is called every time the keyboard is brought up.
     * You can't use onCreate, because that can't pick up new numeric inputs
     */
    override fun onStartInput(
        attribute: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInput(attribute, restarting)
        if (isFloatingModeEnabled()) {
            val dummyView = View(this)
            dummyView.layoutParams = ViewGroup.LayoutParams(0, 0)
            setInputView(dummyView)
            showFloating()
        } else {
            val view = this.setupView()
            setInputView(view)
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        if (isFloatingModeEnabled()) {
            window?.window?.let { w ->
                w.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                )
            }
            showFloating()
        }
    }

    override fun onWindowHidden() {
        hideFloating()
        clearFloatingWindowFlags()
        currentKeyboardDefinition?.settings?.textProcessor?.handleFinishInput(this)
        super.onWindowHidden()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        hideFloating()
        clearFloatingWindowFlags()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        hideFloating()
        clearFloatingWindowFlags()
    }

    /**
     * Clears the floating-mode window flags set in [onWindowShown] so they don't
     * persist and interfere with the IME's normal hide lifecycle on subsequent
     * input sessions.
     */
    private fun clearFloatingWindowFlags() {
        window?.window?.let { w ->
            w.clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            )
        }
    }

    /**
     * When floating, report zero content insets so the host activity doesn't resize,
     * and set an empty touchable region so all touches pass through to the app below.
     * The keyboard floats in a separate overlay window and doesn't occupy layout
     * space or capture touches in the IME's own window.
     */
    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (isFloatingModeEnabled() && outInsets != null) {
            val screenHeight = resources.displayMetrics.heightPixels
            outInsets.contentTopInsets = screenHeight
            outInsets.visibleTopInsets = screenHeight
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
            outInsets.touchableRegion.setEmpty()
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        if (isFloatingModeEnabled()) return false
        return super.onEvaluateFullscreenMode()
    }

    // Lifecycle Methods
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    private fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)

    override val lifecycle = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // Initialize clipboard manager
        val app = application as ThumbkeyApplication
        clipboardManager = ThumbKeyClipboardManager(this, app.clipboardRepository)
        clipboardManager?.startListening()
        clipboardManager?.clearExpired()
    }

    override fun onDestroy() {
        hideFloating()
        clipboardManager?.stopListening()
        clipboardManager = null
        super.onDestroy()
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    // Cursor update Methods
    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)

        cursorMoved =
            if (ignoreCursorMove) {
                ignoreCursorMove = false
                false
            } else {
                Log.d(TAG, "cursor moved")
                cursorAnchorInfo.selectionStart != selectionStart ||
                    cursorAnchorInfo.selectionEnd != selectionEnd
            }

        currentKeyboardDefinition?.settings?.textProcessor?.handleCursorUpdate(
            this,
            selectionStart,
            selectionEnd,
            cursorAnchorInfo.selectionStart,
            cursorAnchorInfo.selectionEnd,
        )

        selectionStart = cursorAnchorInfo.selectionStart
        selectionEnd = cursorAnchorInfo.selectionEnd
    }

    override fun onEvaluateInputViewShown(): Boolean {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        val showOnScreenKeyboard =
            (settings?.showOnScreenKeyboard ?: DEFAULT_SHOW_ON_SCREEN_KEYBOARD).toBool()

        // Always call super implementation because Android docs says:
        // "If you override this method you must call through to the superclass implementation."
        // https://developer.android.com/reference/android/inputmethodservice/InputMethodService#onEvaluateInputViewShown()
        return super.onEvaluateInputViewShown() ||
            showOnScreenKeyboard
    }

    // Disable the fullscreen text editor if set by the user
    override fun onUpdateExtractingVisibility(ei: EditorInfo) {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        if ((settings?.disableFullscreenEditor ?: DEFAULT_DISABLE_FULLSCREEN_EDITOR).toBool()) {
            ei.imeOptions =
                ei.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        }
        super.onUpdateExtractingVisibility(ei)
    }

    fun didCursorMove(): Boolean = cursorMoved

    fun ignoreNextCursorMove() {
        // This gets reset on the next call to `onUpdateCursorAnchorInfo`
        ignoreCursorMove = true
    }

    private var ignoreCursorMove: Boolean = false
    private var cursorMoved: Boolean = false
    private var selectionStart: Int = 0
    private var selectionEnd: Int = 0

    // ViewModelStore Methods
    override val viewModelStore = ViewModelStore()

    // SaveStateRegistry Methods
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    fun clipboardUsePrivate(): Boolean {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        val clipboardHistoryEnabled = (settings?.clipboardHistoryEnabled ?: DEFAULT_CLIPBOARD_HISTORY_ENABLED).toBool()
        val usePrivateClipboard = (settings?.usePrivateClipboard ?: DEFAULT_USE_PRIVATE_CLIPBOARD).toBool()
        return clipboardHistoryEnabled && usePrivateClipboard
    }

    fun clipboardAddPrivateClip(text: String): Unit? = clipboardManager?.addPrivateClip(text)

    fun clipboardWasLastCopyDoneViaSystem(): Boolean = clipboardManager?.wasLastCopyOperationDoneViaSystem() ?: true

    fun clipboardGetLastClip(): String? = clipboardManager?.getLastClip()
}
