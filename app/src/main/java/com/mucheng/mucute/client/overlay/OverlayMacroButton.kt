package com.mucheng.mucute.client.overlay

import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mucheng.mucute.client.game.MacroManager
import kotlin.math.min

/**
 * Nút nổi trên màn hình để chạy một macro bằng một chạm (chạm lần nữa khi đang chạy thì dừng).
 * Kéo nút để đổi chỗ; vị trí được lưu lại trong [MacroManager].
 */
class OverlayMacroButton(
    private val macroName: String
) : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

            windowAnimations = android.R.style.Animation_Toast

            // Nút luôn đã được đăng ký (có vị trí) trước khi cửa sổ được tạo; addButton không làm gì nếu đã có.
            MacroManager.addButton(macroName)
            val saved = MacroManager.buttonPosition(macroName)
            x = saved?.first ?: 0
            y = saved?.second ?: 0
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val width = context.resources.displayMetrics.widthPixels
        val height = context.resources.displayMetrics.heightPixels
        val configuration = LocalConfiguration.current
        val isLandScape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val running = MacroManager.runningName == macroName
        val color by animateColorAsState(
            targetValue = if (running) MaterialTheme.colorScheme.primary else contentColorFor(
                MaterialTheme.colorScheme.surfaceContainerLow
            ),
            label = "animateColor"
        )

        LaunchedEffect(isLandScape) {
            // Xoay màn hình thì giữ nút trong vùng nhìn thấy.
            _layoutParams.x = min(width, _layoutParams.x)
            _layoutParams.y = min(height, _layoutParams.y)
            windowManager.updateViewLayout(composeView, _layoutParams)
            savePosition()
        }

        ElevatedCard(
            onClick = { MacroManager.trigger(macroName) },
            shape = CircleShape,
            modifier = Modifier
                .padding(5.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        _layoutParams.x += dragAmount.x.toInt()
                        _layoutParams.y += dragAmount.y.toInt()
                        windowManager.updateViewLayout(composeView, _layoutParams)
                        savePosition()
                    }
                }
        ) {
            Text(
                (if (running) "■ " else "▶ ") + macroName,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                modifier = Modifier.padding(10.dp)
            )
        }
    }

    private fun savePosition() {
        MacroManager.setButtonPosition(macroName, _layoutParams.x, _layoutParams.y)
    }

}
