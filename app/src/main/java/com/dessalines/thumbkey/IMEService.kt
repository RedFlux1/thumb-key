package com.dessalines.thumbkey

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.InputMethodService.Insets
import android.os.Build
import android.util.Log
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dessalines.thumbkey.db.DEFAULT_CLIPBOARD_HISTORY_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_DISABLE_FULLSCREEN_EDITOR
import com.dessalines.thumbkey.db.DEFAULT_FLOATING_MODE_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_PERSISTENT_FLOATING_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_SHOW_ON_SCREEN_KEYBOARD
import com.dessalines.thumbkey.db.DEFAULT_USE_PRIVATE_CLIPBOARD
import com.dessalines.thumbkey.db.FloatingModeRepository
import com.dessalines.thumbkey.db.OtherSettingsUpdate
import com.dessalines.thumbkey.utils.KeyboardDefinition
import com.dessalines.thumbkey.utils.KeyboardLayout
import com.dessalines.thumbkey.utils.KeyboardMode
import com.dessalines.thumbkey.utils.TAG
import com.dessalines.thumbkey.utils.ThumbKeyClipboardManager
import com.dessalines.thumbkey.utils.toBool

class IMEService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    internal val floatingController by lazy { FloatingKeyboardController(this) }

    internal fun isFloatingModeEnabled(): Boolean {
        val app = application as ThumbkeyApplication
        val settings = app.appSettingsRepository.appSettings.value
        val enabled = (settings?.floatingModeEnabled ?: DEFAULT_FLOATING_MODE_ENABLED).toBool()
        return enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    internal fun isPersistentFloatingEnabled(): Boolean {
        if (!isFloatingModeEnabled()) return false
        val app = application as ThumbkeyApplication
        val settings = app.appSettingsRepository.appSettings.value
        return (settings?.persistentFloatingEnabled ?: DEFAULT_PERSISTENT_FLOATING_ENABLED).toBool()
    }

    internal fun isExpandInFloatingEnabled(): Boolean {
        if (!isFloatingModeEnabled()) return false
        val app = application as ThumbkeyApplication
        return app.floatingModeRepository.isExpandInFloating()
    }

    internal fun maxWidthFraction(): Int {
        val app = application as ThumbkeyApplication
        return app.floatingModeRepository.getMaxWidthFraction()
    }

    internal fun isAutoFloatLandscapeEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val app = application as ThumbkeyApplication
        return app.floatingModeRepository.isAutoFloatLandscape()
    }

    internal fun isDockHintShown(): Boolean = floatingController.isDockHintShown()

    internal fun showFloating() = floatingController.enterFloating()

    internal fun onDrag(
        dx: Float,
        dy: Float,
    ) = floatingController.onDrag(dx, dy)

    internal fun onDragEnd() = floatingController.onDragEnd()

    internal fun onFlingDismiss() {
        floatingController.hideFloating()
        floatingController.markDockHintShown()
        val app = application as ThumbkeyApplication
        val settings = app.appSettingsRepository.appSettings.value
        val isPersistent = (settings?.persistentFloatingEnabled ?: DEFAULT_PERSISTENT_FLOATING_ENABLED).toBool()
        app.appSettingsRepository.updateOtherSettings(
            OtherSettingsUpdate(
                id = 1,
                showOnScreenKeyboard =
                    settings?.showOnScreenKeyboard
                        ?: DEFAULT_SHOW_ON_SCREEN_KEYBOARD,
                floatingModeEnabled = if (isPersistent) 1 else 0,
                persistentFloatingEnabled = 0,
            ),
        )
        if (isPersistent) {
            requestHideSelf(0)
        } else {
            setInputView(setupView())
        }
    }

    internal fun hideFloating() = floatingController.hideFloating()

    internal var shouldRevertToFloating = false
    internal var pendingDockedMode: KeyboardMode? = null

    /** Explicitly disable floating and show docked view for emoji/clipboard. */
    internal fun disableFloatingForDocking() {
        val app = application as ThumbkeyApplication
        val settings = app.appSettingsRepository.appSettings.value
        app.appSettingsRepository.updateOtherSettings(
            OtherSettingsUpdate(
                id = 1,
                showOnScreenKeyboard =
                    settings?.showOnScreenKeyboard
                        ?: DEFAULT_SHOW_ON_SCREEN_KEYBOARD,
                floatingModeEnabled = 0,
                persistentFloatingEnabled =
                    settings?.persistentFloatingEnabled
                        ?: DEFAULT_PERSISTENT_FLOATING_ENABLED,
            ),
        )
        hideFloating()
        setInputView(setupView())
        pendingDockedMode = null
    }

    /** Explicitly re-enable floating after emoji/clipboard docking. */
    internal fun enableFloatingAfterDocking() {
        val app = application as ThumbkeyApplication
        val settings = app.appSettingsRepository.appSettings.value
        app.appSettingsRepository.updateOtherSettings(
            OtherSettingsUpdate(
                id = 1,
                showOnScreenKeyboard =
                    settings?.showOnScreenKeyboard
                        ?: DEFAULT_SHOW_ON_SCREEN_KEYBOARD,
                floatingModeEnabled = 1,
                persistentFloatingEnabled =
                    settings?.persistentFloatingEnabled
                        ?: DEFAULT_PERSISTENT_FLOATING_ENABLED,
            ),
        )
        val dummyView =
            object : View(this) {
                override fun onMeasure(
                    widthMeasureSpec: Int,
                    heightMeasureSpec: Int,
                ) {
                    setMeasuredDimension(0, 0)
                }
            }
        setInputView(dummyView)
        window?.window?.decorView?.requestApplyInsets()
        showFloating()
    }

    internal fun togglePersistentMode() {
        val app = application as ThumbkeyApplication
        val settings = app.appSettingsRepository.appSettings.value
        val currentlyEnabled =
            (
                settings?.persistentFloatingEnabled
                    ?: DEFAULT_PERSISTENT_FLOATING_ENABLED
            ).toBool()

        val newEnabled = if (currentlyEnabled) 0 else 1
        app.appSettingsRepository.updateOtherSettings(
            OtherSettingsUpdate(
                id = 1,
                showOnScreenKeyboard =
                    settings?.showOnScreenKeyboard
                        ?: DEFAULT_SHOW_ON_SCREEN_KEYBOARD,
                floatingModeEnabled =
                    settings?.floatingModeEnabled
                        ?: DEFAULT_FLOATING_MODE_ENABLED,
                persistentFloatingEnabled = newEnabled,
            ),
        )
    }

    internal fun toggleFloatingMode() {
        val app = application as ThumbkeyApplication
        val settings = app.appSettingsRepository.appSettings.value
        val currentlyEnabled =
            (
                settings?.floatingModeEnabled
                    ?: DEFAULT_FLOATING_MODE_ENABLED
            ).toBool()

        setFloatingEnabled(!currentlyEnabled, auto = false)
    }

    /**
     * Shared floating on/off switch. Gboard auto-float parity: [auto] marks
     * controller-entered sessions so rotation back to portrait can restore
     * docked mode without overriding an explicit user toggle.
     */
    internal fun setFloatingEnabled(
        enabled: Boolean,
        auto: Boolean,
    ) {
        val app = application as ThumbkeyApplication
        val settings = app.appSettingsRepository.appSettings.value
        app.appSettingsRepository.updateOtherSettings(
            OtherSettingsUpdate(
                id = 1,
                showOnScreenKeyboard =
                    settings?.showOnScreenKeyboard
                        ?: DEFAULT_SHOW_ON_SCREEN_KEYBOARD,
                floatingModeEnabled = if (enabled) 1 else 0,
                persistentFloatingEnabled =
                    settings?.persistentFloatingEnabled
                        ?: DEFAULT_PERSISTENT_FLOATING_ENABLED,
            ),
        )

        if (enabled) {
            floatingController.autoFloated = auto
            val dummyView =
                object : View(this) {
                    override fun onMeasure(
                        widthMeasureSpec: Int,
                        heightMeasureSpec: Int,
                    ) {
                        setMeasuredDimension(0, 0)
                    }
                }
            setInputView(dummyView)
            // Force the IME window to recompute its size
            window?.window?.decorView?.requestApplyInsets()
            showFloating()
        } else {
            hideFloating()
            floatingController.autoFloated = false
            setInputView(setupView())
        }
    }

    internal fun setupView(): ComposeKeyboardView {
        val app = application as ThumbkeyApplication
        val settingsRepo = app.appSettingsRepository
        val clipboardRepo = app.clipboardRepository

        val initialMode = pendingDockedMode
        val forceDocked = pendingDockedMode != null

        val layoutIndex = settingsRepo.appSettings.value?.keyboardLayout
        if (layoutIndex != null) {
            currentKeyboardDefinition = KeyboardLayout.entries[layoutIndex].keyboardDefinition
        }

        val view = ComposeKeyboardView(this, settingsRepo, clipboardRepo, initialMode, forceDocked)
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
            val dummyView =
                object : View(this) {
                    override fun onMeasure(
                        widthMeasureSpec: Int,
                        heightMeasureSpec: Int,
                    ) {
                        setMeasuredDimension(0, 0)
                    }
                }
            setInputView(dummyView)
        } else {
            val view = this.setupView()
            this.setInputView(view)
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        if (isFloatingModeEnabled()) {
            if (isAutoFloatLandscapeEnabled() &&
                floatingController.autoFloated &&
                floatingController.currentOrientation() == 1
            ) {
                // Auto-float session ended: rotation back to portrait restores docked.
                setFloatingEnabled(false, auto = false)
                return
            }
            // Orientation changed or first show
            floatingController.refreshAfterWindowShown()
        } else if (isAutoFloatLandscapeEnabled() && floatingController.currentOrientation() == 2) {
            // Gboard "auto float in landscape" parity: enter floating automatically.
            setFloatingEnabled(true, auto = true)
        }
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

    override fun onFinishInput() {
        super.onFinishInput()
        // Defer hiding to avoid flicker from rapid start/finish cycles.
        // If onStartInput fires again shortly, the floating view will still be alive.
    }

    override fun onWindowHidden() {
        if (isFloatingModeEnabled() && !isPersistentFloatingEnabled()) {
            hideFloating()
        }
        currentKeyboardDefinition?.settings?.textProcessor?.handleFinishInput(this)
        super.onWindowHidden()
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
        val persistentEnabled =
            (settings?.persistentFloatingEnabled ?: DEFAULT_PERSISTENT_FLOATING_ENABLED).toBool()

        return super.onEvaluateInputViewShown() ||
            showOnScreenKeyboard ||
            persistentEnabled
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

    suspend fun clipboardGetLastClipItem() = clipboardManager?.getLastClipItem()

    fun clipboardSweepImages() = clipboardManager?.sweepOrphanImages()
}
