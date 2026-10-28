package com.mucheng.mucute.client.router.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Help
import androidx.compose.material.icons.twotone.AccountCircle
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mucheng.mucute.client.R
import com.mucheng.mucute.client.ui.component.NeonTab
import com.mucheng.mucute.client.ui.component.NeonTabBar
import com.mucheng.mucute.client.ui.theme.NeonHorizontalDivider
import com.mucheng.mucute.client.viewmodel.MainScreenViewModel

@Immutable
enum class MainScreenPages(
    val icon: @Composable () -> Unit,
    val label: @Composable () -> Unit,
    val content: @Composable () -> Unit
) {
    HomePage(
        icon = { Icon(Icons.TwoTone.Home, contentDescription = null) },
        label = { Text(stringResource(R.string.home)) },
        content = { HomePageContent() }
    ),
    AccountPage(
        icon = { Icon(Icons.TwoTone.AccountCircle, contentDescription = null) },
        label = { Text(stringResource(R.string.account)) },
        content = { AccountPageContent() }
    ),
    ServerPage(
        icon = { Icon(Icons.TwoTone.Storage, contentDescription = null) },
        label = { Text(stringResource(R.string.server)) },
        content = { ServerPageContent() }
    ),
    AboutPage(
        icon = { Icon(Icons.AutoMirrored.TwoTone.Help, contentDescription = null) },
        label = { Text(stringResource(R.string.about)) },
        content = { AboutPageContent() }
    ),
    SettingsPage(
        icon = { Icon(Icons.TwoTone.Settings, contentDescription = null) },
        label = { Text(stringResource(R.string.settings)) },
        content = { SettingsPageContent() }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen() {
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val selectedPage by mainScreenViewModel.selectedPage.collectAsStateWithLifecycle()
    val bottomInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        AnimatedContent(
            targetState = selectedPage,
            label = "animatedPage",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(bottomInsets)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) { currentPage ->
            Box(Modifier.fillMaxSize()) {
                currentPage.content()
            }
        }
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .windowInsetsPadding(bottomInsets)
        ) {
            NeonHorizontalDivider()
            NeonTabBar {
                MainScreenPages.entries.fastForEach { page ->
                    NeonTab(
                        selected = selectedPage === page,
                        onClick = {
                            if (selectedPage !== page) {
                                mainScreenViewModel.selectPage(page)
                            }
                        },
                        icon = page.icon,
                        label = page.label
                    )
                }
            }
        }
    }
}
