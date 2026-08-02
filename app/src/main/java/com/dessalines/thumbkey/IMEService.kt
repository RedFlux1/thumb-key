package com.dessalines.thumbkey

import android.content.Context
import android.graphics.PixelFormat
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.InputMethodService.Insets
import android.os.Build
import android.util.Log
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
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var floatingView: ComposeKeyboardView? = null
    private var floatingOrientation: Int = 0

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

    internal fun showFloating() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (floatingView != null) return

        val view = setupView()

        // Load saved position for current orientation
        val orientation =
            if (resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
            ) {
                2
            } else {
                1
            }
        val app = application as ThumbkeyApplication
        val saved = app.floatingModeRepository.loadPosition(orientation)
        val opacity = app.floatingModeRepository.getOpacity()

        val params =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = saved?.first ?: 100
                    y = saved?.second ?: 200
                    alpha = opacity / 100f
                }

        try {
            windowManager.addView(view, params)
            floatingView = view
            floatingOrientation = orientation
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating view: $e")
        }
    }

    internal fun onDrag(
        dx: Float,
        dy: Float,
    ) {
        val view = floatingView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        params.x += dx.toInt()
        params.y += dy.toInt()

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Get status bar height to prevent keyboard from hiding behind it
        val statusBarHeight = getStatusBarHeight()
        val minY = statusBarHeight

        params.x = params.x.coerceIn(0, (screenWidth - view.width).coerceAtLeast(0))
        params.y = params.y.coerceIn(minY, (screenHeight - view.height).coerceAtLeast(minY))

        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update view layout on drag: $e")
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    internal fun onDragEnd() {
        val view = floatingView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        val orientation =
            if (resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
            ) {
                2
            } else {
                1
            }
        val app = application as ThumbkeyApplication
        app.floatingModeRepository.savePosition(
            orientation = orientation,
            x = params.x,
            y = params.y,
        )
    }

    internal fun onFlingDismiss() {
        hideFloating()
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

        val newEnabled = if (currentlyEnabled) 0 else 1
        app.appSettingsRepository.updateOtherSettings(
            OtherSettingsUpdate(
                id = 1,
                showOnScreenKeyboard =
                    settings?.showOnScreenKeyboard
                        ?: DEFAULT_SHOW_ON_SCREEN_KEYBOARD,
                floatingModeEnabled = newEnabled,
                persistentFloatingEnabled =
                    settings?.persistentFloatingEnabled
                        ?: DEFAULT_PERSISTENT_FLOATING_ENABLED,
            ),
        )

        if (currentlyEnabled) {
            hideFloating()
            setInputView(setupView())
        } else {
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
        }
    }

    private fun setupView(): ComposeKeyboardView {
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
            val currentOrientation =
                if (resources.configuration.orientation ==
                    android.content.res.Configuration.ORIENTATION_LANDSCAPE
                ) {
                    2
                } else {
                    1
                }
            if (floatingView != null && floatingOrientation == currentOrientation) return
            // Orientation changed or first show
            hideFloating()
            showFloating()
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
}
