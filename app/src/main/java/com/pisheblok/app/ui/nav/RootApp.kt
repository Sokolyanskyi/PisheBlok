package com.pisheblok.app.ui.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pisheblok.app.PisheBlokApp
import com.pisheblok.app.R
import com.pisheblok.app.auth.AuthState
import com.pisheblok.app.data.model.Section
import com.pisheblok.app.sync.SyncStatus
import com.pisheblok.app.ui.auth.AuthScreen
import com.pisheblok.app.ui.components.LocalAppContainer
import com.pisheblok.app.ui.locations.LocationsScreen
import com.pisheblok.app.ui.shopping.ShoppingScreen
import androidx.compose.runtime.CompositionLocalProvider

private enum class MainTab(val icon: ImageVector, val labelRes: Int) {
    FOOD(Icons.Rounded.LunchDining, R.string.tab_food),
    SHOPPING(Icons.Rounded.ShoppingCart, R.string.tab_shopping),
    CHEM(Icons.Rounded.Science, R.string.tab_chem),
}

@Composable
fun RootApp() {
    val container = (LocalContext.current.applicationContext as PisheBlokApp).container
    val authState by container.auth.state.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalAppContainer provides container) {
        val needsAuth = authState.cloudReady && authState.user == null
        AnimatedContent(
            targetState = needsAuth,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            label = "root",
        ) { auth ->
            if (auth) {
                AuthScreen(container)
            } else {
                MainScaffold()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {
    val container = LocalAppContainer.current
    val authState by container.auth.state.collectAsStateWithLifecycle()
    val syncStatus by container.sync.status.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    // ask once for notification permission (Android 13+)
    val notifPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) {}
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            MainTopBar(
                authState = authState,
                syncStatus = syncStatus,
                onLanguage = { showLanguageDialog = true },
                onSignOut = { container.auth.signOut() },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!authState.cloudReady) {
                LocalModeBanner()
            }
            Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "tabs",
                ) { idx ->
                    when (idx) {
                        0 -> LocationsScreen(Section.FOOD, stringResource(R.string.food_title))
                        1 -> ShoppingScreen()
                        2 -> LocationsScreen(Section.CHEM, stringResource(R.string.chem_title))
                        else -> {}
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(onDismiss = { showLanguageDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    authState: AuthState,
    syncStatus: SyncStatus,
    onLanguage: () -> Unit,
    onSignOut: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val syncing = syncStatus is SyncStatus.Syncing

    val rotation = if (syncing) {
        val transition = rememberInfiniteTransition(label = "sync")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
            label = "syncRotation",
        ).value
    } else 0f

    val syncIcon = when {
        !authState.cloudReady -> Icons.Rounded.CloudOff
        syncStatus is SyncStatus.Syncing -> Icons.Rounded.CloudSync
        syncStatus is SyncStatus.Ok -> Icons.Rounded.CloudDone
        syncStatus is SyncStatus.Offline -> Icons.Rounded.CloudOff
        syncStatus is SyncStatus.Error -> Icons.Rounded.SyncProblem
        else -> Icons.Rounded.CloudQueue
    }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🥘", fontSize = 15.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            Icon(
                syncIcon,
                contentDescription = stringResource(R.string.cd_sync),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .rotate(rotation),
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.Person, contentDescription = stringResource(R.string.cd_account))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        enabled = false,
                        text = {
                            Text(
                                authState.user?.email ?: stringResource(R.string.sync_mode_off),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {},
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.language_title)) },
                        leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onLanguage()
                        },
                    )
                    if (authState.user != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.account_sign_out)) },
                            leadingIcon = { Icon(Icons.Rounded.Logout, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onSignOut()
                            },
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun LocalModeBanner() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.sync_banner_local), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private enum class LangChoice(val tag: String?) {
    SYSTEM(null), RU("ru"), UK("uk"), EN("en")
}

@Composable
private fun LanguageDialog(onDismiss: () -> Unit) {
    val current = currentLangChoice()
    var selection by rememberSaveable { mutableStateOf(current.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column {
                LangChoice.entries.forEach { choice ->
                    val label = when (choice) {
                        LangChoice.SYSTEM -> stringResource(R.string.language_system)
                        LangChoice.RU -> "Русский"
                        LangChoice.UK -> "Українська"
                        LangChoice.EN -> "English"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selection == choice.name) {
                                selection = choice.name
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selection == choice.name, onClick = { selection = choice.name })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val choice = LangChoice.entries.firstOrNull { it.name == selection } ?: LangChoice.SYSTEM
                AppCompatDelegate.setApplicationLocales(
                    choice.tag?.let { LocaleListCompat.forLanguageTags(it) }
                        ?: LocaleListCompat.getEmptyLocaleList()
                )
                onDismiss()
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun currentLangChoice(): LangChoice {
    val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags().lowercase()
    return when {
        tags.startsWith("ru") -> LangChoice.RU
        tags.startsWith("uk") -> LangChoice.UK
        tags.startsWith("en") -> LangChoice.EN
        else -> LangChoice.SYSTEM
    }
}
