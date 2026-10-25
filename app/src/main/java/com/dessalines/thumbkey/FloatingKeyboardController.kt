package com.dessalines.thumbkey

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import com.dessalines.thumbkey.db.FloatingModeRepository
import com.dessalines.thumbkey.utils.TAG

/**
 * Gboard-parity floating keyboard controller.
 *
 * Mirrors Gboard 18.4.4's floating mode architecture (static analysis):
 * - `AccessoryInputModeManager.switchToFloatingKeyboardMode` (defpackage.mjz.y)
 *   is a mode switch that builds a capped overlay plus a dismiss accessory
 *   (`close_accessory_floating_keyboard`), not a separate Activity.
 * - `WidgetDragToMoveMotionEventHandler.g` tracks raw touch deltas with a
 *   `VelocityTracker` (1000 px/s unit) and moves via `njt.a(dx,dy)` /
 *   fling via `njt.b(vx,vy)`.
 *
 * This controller owns all overlay window mechanics. [IMEService] keeps mode
 * toggles, DB writes, and IME lifecycle wiring.
 */
class FloatingKeyboardController(
    private val ime: IMEService,
) {
    companion object {
        /** Down-fling velocity that dismisses floating (px/s, kept from e01). */
        const val FLING_DISMISS_VELOCITY_Y = 2500f

        /** Downward drag distance that arms the dock target (px). */
        const val DOCK_ARM_DISTANCE_PX = 320f
    }

    private val windowManager by lazy { ime.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var floatingView: ComposeKeyboardView? = null
    private var floatingOrientation: Int = 0

    /** True when floating was entered automatically (auto-float landscape). */
    internal var autoFloated: Boolean = false

    private fun repo(): FloatingModeRepository = (ime.application as ThumbkeyApplication).floatingModeRepository

    fun isShowing(): Boolean = floatingView != null

    fun currentOrientation(): Int =
        if (ime.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            2
        } else {
            1
        }

    /** Gboard `switchToFloatingKeyboardMode` parity: build the capped overlay. */
    fun enterFloating() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (floatingView != null) return

        val orientation = currentOrientation()
        val view = ime.setupView()
        val saved = repo().loadPosition(orientation)
        val opacity = repo().getOpacity()

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
        clampToScreen(params, view)

        try {
            windowManager.addView(view, params)
            floatingView = view
            floatingOrientation = orientation
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating view: $e")
        }
    }

    fun hideFloating() {
        floatingView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating view: $e")
            }
        }
        floatingView = null
        autoFloated = false
    }

    /** Rebuild the overlay after rotation (Gboard `r()` max-size parity). */
    fun refreshAfterWindowShown() {
        val current = currentOrientation()
        if (floatingView != null && floatingOrientation == current) return
        hideFloating()
        enterFloating()
    }

    /** Gboard `njt.a(dx,dy)` parity: move with screen-margin clamping. */
    fun onDrag(
        dx: Float,
        dy: Float,
    ) {
        val view = floatingView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        params.x += dx.toInt()
        params.y += dy.toInt()
        clampToScreen(params, view)

        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update view layout on drag: $e")
        }
    }

    fun onDragEnd() {
        val view = floatingView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        repo().savePosition(
            orientation = currentOrientation(),
            x = params.x,
            y = params.y,
        )
    }

    fun markDockHintShown() = repo().markDockHintShown()

    fun isDockHintShown(): Boolean = repo().isDockHintShown()

    fun resetDockHint() = repo().resetDockHint()

    private fun getStatusBarHeight(): Int {
        val resourceId = ime.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) ime.resources.getDimensionPixelSize(resourceId) else 0
    }

    /** Gboard `r()` parity: keep the window fully on-screen below the status bar. */
    private fun clampToScreen(
        params: WindowManager.LayoutParams,
        view: ComposeKeyboardView,
    ) {
        val displayMetrics = ime.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val minY = getStatusBarHeight()
        params.x = params.x.coerceIn(0, (screenWidth - view.width).coerceAtLeast(0))
        params.y = params.y.coerceIn(minY, (screenHeight - view.height).coerceAtLeast(minY))
    }
}
