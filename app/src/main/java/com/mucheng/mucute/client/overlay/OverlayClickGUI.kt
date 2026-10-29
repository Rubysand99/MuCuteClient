package com.mucheng.mucute.client.overlay

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.mucheng.mucute.client.game.FavoriteManager
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.game.ModuleContent
import com.mucheng.mucute.client.ui.component.NavigationRailX
import com.mucheng.mucute.client.util.translatedSelf
import kotlinx.coroutines.delay

class OverlayClickGUI : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags =
                flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            if (Build.VERSION.SDK_INT >= 31) {
                blurBehindRadius = 40
            }

            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

            dimAmount = 0.85f
            windowAnimations = android.R.style.Animation_Dialog
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    private var selectedModuleCategory by mutableStateOf(ModuleCategory.Combat)

    private var searchQuery by mutableStateOf("")

    private var favoritesOnly by mutableStateOf(false)

    private var searchFocused by mutableStateOf(false)

    /**
     * Cửa sổ overlay mặc định không nhận focus (để không cướp thao tác của game), nên chỉ bật focus
     * trong lúc người dùng gõ vào ô tìm kiếm để bàn phím hiện được.
     */
    private fun setWindowFocusable(focusable: Boolean) {
        val params = _layoutParams
        val flags = if (focusable) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        if (flags == params.flags) return

        params.flags = flags
        try {
            if (composeView.isAttachedToWindow) {
                windowManager.updateViewLayout(composeView, params)
            }
        } catch (_: Exception) {
            // Cửa sổ đã bị gỡ: cờ đã được cập nhật cho lần hiện sau.
        }
    }

    @Composable
    override fun Content() {
        Column(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    OverlayManager.dismissOverlayWindow(this)
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .padding(40.dp)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRailX(
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        ModuleCategory.entries.fastForEach { moduleCategory ->
                            NavigationRailItem(
                                selected = selectedModuleCategory === moduleCategory,
                                onClick = {
                                    if (selectedModuleCategory !== moduleCategory) {
                                        selectedModuleCategory = moduleCategory
                                    }
                                },
                                icon = {
                                    Icon(
                                        painterResource(moduleCategory.iconResId),
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(stringResource(moduleCategory.labelResId))
                                },
                                alwaysShowLabel = false
                            )
                        }
                    }
                    VerticalDivider()
                    DialogContent()
                }
            }
        }
    }

    @Composable
    private fun DialogContent() {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            SearchBar()
            AnimatedContent(
                targetState = selectedModuleCategory,
                label = "animatedPage",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { moduleCategory ->
                Box(Modifier.fillMaxSize()) {
                    ModuleContent(moduleCategory, searchQuery, favoritesOnly)
                }
            }
        }
    }

    @Composable
    private fun SearchBar() {
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(searchFocused) {
            setWindowFocusable(searchFocused)
            if (searchFocused) {
                // Chờ cửa sổ nhận focus rồi mới yêu cầu hiện bàn phím.
                delay(200)
                keyboardController?.show()
            }
        }

        DisposableEffect(Unit) {
            onDispose { setWindowFocusable(false) }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = { Text("search_modules".translatedSelf) },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { searchFocused = it.isFocused }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = favoritesOnly,
                onClick = { favoritesOnly = !favoritesOnly },
                label = { Text("\u2605") }
            )
            Spacer(Modifier.width(8.dp))
            // Hiện/ẩn nút tắt/bật nhanh trên màn hình cho mọi module yêu thích (★).
            FilterChip(
                selected = FavoriteManager.areAllShortcutsShown(),
                enabled = FavoriteManager.favoriteModules().isNotEmpty(),
                onClick = { FavoriteManager.toggleFavoriteShortcuts() },
                label = { Text("\u26A1") }
            )
        }
    }

}
