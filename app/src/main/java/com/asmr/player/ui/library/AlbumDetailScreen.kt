package com.asmr.player.ui.library

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.asmr.player.data.local.db.AppDatabaseProvider
import com.asmr.player.data.local.db.entities.LocalTreeCacheEntity
import com.asmr.player.data.remote.auth.DlsiteAuthStore
import com.asmr.player.data.remote.api.AsmrOneTrackNodeResponse
import com.asmr.player.data.remote.scraper.DlsiteRecommendedWork
import com.asmr.player.data.remote.scraper.DlsiteRecommendations
import com.asmr.player.domain.model.Album
import com.asmr.player.domain.model.Track
import com.asmr.player.playback.MediaItemFactory
import com.asmr.player.data.remote.NetworkHeaders
import com.asmr.player.cache.CacheImageModel
import com.asmr.player.data.remote.dlsite.DlsiteLanguageEdition
import com.asmr.player.ui.dlsite.DlsitePlayViewModel
import com.asmr.player.util.DlsiteAntiHotlink
import com.asmr.player.util.SmartSortKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import com.asmr.player.data.lyrics.deriveLyricsRelativePathNoExt
import com.asmr.player.ui.common.SubtitleStamp
import com.asmr.player.ui.common.DiscPlaceholder
import com.asmr.player.ui.common.AsmrAsyncImage
import com.asmr.player.ui.common.AsmrShimmerPlaceholder
import com.asmr.player.ui.common.EaraLogoLoadingIndicator
import com.asmr.player.ui.common.ImagePreviewDialog
import com.asmr.player.ui.common.ImagePreviewRequest
import com.asmr.player.ui.common.collapsibleHeaderUiState
import com.asmr.player.ui.common.rememberCollapsibleHeaderState
import com.asmr.player.ui.playlists.PlaylistPickerScreen
import com.asmr.player.ui.theme.AsmrTheme
import com.asmr.player.ui.common.LocalBottomOverlayPadding
import com.asmr.player.ui.common.thinScrollbar
import com.asmr.player.ui.theme.AsmrPlayerTheme
import com.asmr.player.ui.theme.dynamicPageContainerColor
import com.asmr.player.util.Formatting
import com.asmr.player.util.MessageManager
import com.asmr.player.util.RemoteSubtitleSource
import java.util.UUID

private enum class AlbumPrimaryAction {
    Download,
    Save
}

private enum class OnlineDownloadSource {
    AsmrOne,
    DlsitePlay,
    DlsiteTrial
}

internal data class PreparedTrackPlayback(
    val tracks: List<Track>,
    val startTrack: Track,
    val onlineLyrics: Map<String, List<RemoteSubtitleSource>> = emptyMap()
)

internal data class PreparedMediaPlayback(
    val items: List<MediaItem>,
    val startIndex: Int
)

private val AlbumDetailTabContentGap = 0.dp
private val AlbumDetailScrolledContentFadeSpan = 56.dp
private const val AlbumDetailHeroThemeGradientStartFraction = 0.42f
private const val AlbumDetailHeroThemeGradientSolidFraction = 0.86f
private const val AlbumDetailInitialIntroDurationMs = 1200L
internal val AlbumDetailHorizontalPadding = 8.dp

private val DlsiteElasticResizeSpring = spring<IntSize>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow
)

internal fun dlsiteElasticItemModifier(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
): Modifier {
    return if (enabled) {
        modifier.animateContentSize(animationSpec = DlsiteElasticResizeSpring)
    } else {
        modifier
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumDetailScreen(
    windowSizeClass: WindowSizeClass,
    albumId: Long? = null,
    rjCode: String? = null,
    refreshToken: Long = 0L,
    onConsumeRefreshToken: (() -> Unit)? = null,
    onPlayTracks: (Album, List<Track>, Track) -> Unit,
    onPlayMediaItems: (List<MediaItem>, Int) -> Unit = { _, _ -> },
    onAddToQueue: (Album, Track) -> Boolean = { _, _ -> false },
    onAddMediaItemsToQueue: (List<MediaItem>) -> Unit = {},
    onAddMediaItemsToFavorites: (List<MediaItem>) -> Unit = {},
    onOpenPlaylistPicker: (MediaItem) -> Unit = {},
    onOpenGroupPicker: (albumId: Long) -> Unit = { _ -> },
    onOpenDlsiteLogin: () -> Unit = {},
    onOpenAlbumByRj: (String) -> Unit = {},
    initialTab: Int? = null,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cloudSyncSelectionDialogState by viewModel.cloudSyncSelectionDialogState.collectAsState()
    val colorScheme = AsmrTheme.colorScheme
    val screenKey = remember(albumId, rjCode) {
        val idPart = albumId?.takeIf { it > 0 }?.toString().orEmpty()
        val rjPart = rjCode?.trim().orEmpty().uppercase()
        if (rjPart.isNotBlank()) "album:$rjPart" else "albumId:$idPart"
    }
    val introSessionKey = remember(screenKey) { "intro:${UUID.randomUUID()}" }
    // 入口决定固定展示的二级页面：本地库->本地，在线/搜索->DL，preferDlsitePlay->DL Play。
    // 不再提供页内 tab 切换与左右滑动。
    val selectedTab = remember(albumId, initialTab) {
        initialTab?.coerceIn(0, 2) ?: if (albumId != null && albumId > 0) 0 else 1
    }
    var initialIntroSettled by remember(screenKey) { mutableStateOf(false) }
    var showAsmrDownloadDialog by remember { mutableStateOf(false) }
    var showOnlineSaveDialog by remember { mutableStateOf(false) }
    var pendingOnlineSaveSelection by remember { mutableStateOf<Set<String>?>(null) }
    var batchPlaylistItems by remember { mutableStateOf<List<MediaItem>?>(null) }
    var downloadSource by remember { mutableStateOf(OnlineDownloadSource.AsmrOne) }

    LaunchedEffect(albumId, rjCode) {
        viewModel.loadAlbum(albumId, rjCode, force = false)
    }
    LaunchedEffect(refreshToken) {
        if (refreshToken == 0L) return@LaunchedEffect
        viewModel.loadAlbum(albumId, rjCode, force = true)
        onConsumeRefreshToken?.invoke()
    }
    LaunchedEffect(pendingOnlineSaveSelection) {
        val selected = pendingOnlineSaveSelection ?: return@LaunchedEffect
        pendingOnlineSaveSelection = null
        viewModel.saveOnlineSelectedToLibrary(selected)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AsmrTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter // 仅用于平板适配：居中显示内容
    ) {
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        
        Column(
            modifier = if (isCompact) {
                Modifier.fillMaxSize()
            } else {
                // 仅用于平板适配：限制内容区域最大宽度并填充可用空间
                Modifier
                    .fillMaxHeight()
                    .widthIn(max = 800.dp)
                    .fillMaxWidth()
            }
        ) {
            when (val state = uiState) {
                is AlbumDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EaraLogoLoadingIndicator(tint = AsmrTheme.colorScheme.primary)
                    }
                }
                is AlbumDetailUiState.Success -> {
                    LaunchedEffect(screenKey, initialIntroSettled) {
                        if (initialIntroSettled) return@LaunchedEffect
                        delay(AlbumDetailInitialIntroDurationMs)
                        initialIntroSettled = true
                    }
                    val model = state.model
                    val album = model.displayAlbum
                    val asmrOneTree = model.asmrOneTree
                    val trialDownloadTree = remember(model.dlsiteTrialTracks) {
                        buildDlsiteTrialDownloadTree(model.dlsiteTrialTracks)
                    }
                    val shouldPlayInitialAnimations = !initialIntroSettled
                    val shouldAnimateHeaderIntro = true
                    val availableTags by viewModel.availableTags.collectAsState()
                    val userTagsByTrackId by viewModel.userTagsByTrackId.collectAsState()
                    val libraryViewModel: LibraryViewModel = hiltViewModel()
                    var showTagManager by remember { mutableStateOf(false) }
                    var tagManageTrack by remember { mutableStateOf<Track?>(null) }
                    val context = LocalContext.current
                    val coverPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                        onResult = { uri ->
                            if (uri == null) return@rememberLauncherForActivityResult
                            runCatching {
                                context.contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }
                            viewModel.setLocalCoverPath(uri.toString())
                        }
                    )
                    var localPreviewFile by remember { mutableStateOf<LocalTreeUiEntry.File?>(null) }
                    var onlinePreviewFile by remember { mutableStateOf<AsmrTreeUiEntry.File?>(null) }
                    var imagePreviewRequest by remember { mutableStateOf<ImagePreviewRequest?>(null) }
                    // tab 标签栏已移除，但各二级页面组件仍需要一个折叠头状态用于嵌套滚动协调。
                    // 由于不再渲染 chrome，其 heightPx 始终为 0，相关调用均为安全空操作。
                    val tabChromeState = rememberCollapsibleHeaderState()

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val pageContainerColor = dynamicPageContainerColor(colorScheme)
                        val heroHeightLimit = if (isCompact) {
                            maxHeight * 0.62f
                        } else {
                            maxHeight * 0.64f
                        }
                        val heroMinHeight = if (isCompact) 280.dp else 360.dp
                        val heroPreferredHeight = if (isCompact) {
                            maxWidth
                        } else {
                            maxWidth * 0.88f
                        }
                        val heroHeight = heroPreferredHeight
                            .coerceAtLeast(heroMinHeight)
                            .coerceAtMost(heroHeightLimit.coerceAtLeast(heroMinHeight))
                        val heroContentOverlap = (heroHeight * if (isCompact) 0.30f else 0.26f)
                            .coerceIn(
                                minimumValue = if (isCompact) 104.dp else 124.dp,
                                maximumValue = if (isCompact) 148.dp else 184.dp
                            )
                        val tabChromeTop = (heroHeight - heroContentOverlap).coerceAtLeast(0.dp)
                        val tabContentTopPadding = tabChromeTop + AlbumDetailTabContentGap
                        val contentFadeEndY = tabChromeTop
                        val contentFadeStartY = (contentFadeEndY - AlbumDetailScrolledContentFadeSpan).coerceAtLeast(0.dp)

                        fun headerAlbumForTab(tab: Int): Album {
                            return if (tab == 0) (model.localAlbum ?: album) else album
                        }

                        fun shouldShowCoverLoading(tab: Int, headerAlbum: Album): Boolean {
                            val headerHasCover = headerAlbum.coverPath.trim().isNotBlank() ||
                                headerAlbum.coverUrl.trim().isNotBlank()
                            return !headerHasCover && when (tab) {
                                0 -> false
                                1 -> model.isLoadingDlsite ||
                                    model.isLoadingAsmrOne ||
                                    !model.hasResolvedInitialDlsiteTarget
                                else -> model.isLoadingDlsite ||
                                    model.isLoadingDlsitePlay ||
                                    !model.hasResolvedInitialDlsiteTarget
                            }
                        }

                        val activeHeroAlbum = headerAlbumForTab(selectedTab)
                        val pickLocalCoverAction = if (selectedTab == 0 && model.localAlbum != null) {
                            { coverPicker.launch(arrayOf("image/*")) }
                        } else {
                            null
                        }
                        val showHeroCoverLoadingState = shouldShowCoverLoading(selectedTab, activeHeroAlbum)

                        val headerContent: @Composable (Int) -> Unit = { tab ->
                            val isLocalTab = tab == 0
                            val canSaveOnlineForTab = tab == 1 && asmrOneTree.isNotEmpty()
                            val headerAlbum = headerAlbumForTab(tab)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = pageContainerColor,
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            ) {
                                AlbumHeader(
                                album = headerAlbum,
                                listenTogetherRjListenerCount = model.listenTogetherRjListenerCount,
                                dlsiteUrl = model.dlsiteWorkno.takeIf { it.isNotBlank() }?.let { "https://www.dlsite.com/maniax/work/=/product_id/$it.html" }.orEmpty(),
                                asmrOneUrl = model.asmrOneWorkId?.takeIf { it.isNotBlank() }?.let { "https://asmr.one/work/$it" }.orEmpty(),
                                dlsiteEditions = if (isLocalTab) emptyList() else model.dlsiteEditions,
                                dlsiteSelectedLang = model.dlsiteSelectedLang,
                                onDlsiteLangSelected = { viewModel.selectDlsiteLanguage(it) },
                                canSaveOnline = canSaveOnlineForTab,
                                onDownloadClick = {
                                    downloadSource = if (tab == 2) {
                                        OnlineDownloadSource.DlsitePlay
                                    } else {
                                        OnlineDownloadSource.AsmrOne
                                    }
                                    showAsmrDownloadDialog = true
                                },
                                showDlsitePlayLossless = tab == 2,
                                onLosslessDownloadClick = {
                                    viewModel.downloadDlsitePlayLosslessArchive()
                                },
                                onSaveClick = {
                                    showOnlineSaveDialog = true
                                },
                                downloadEnabled = when (tab) {
                                    1 -> asmrOneTree.isNotEmpty()
                                    2 -> model.dlsitePlayTree.isNotEmpty()
                                    else -> false
                                },
                                losslessDownloadEnabled = tab == 2 && model.dlsitePlayTree.isNotEmpty(),
                                saveEnabled = canSaveOnlineForTab,
                                showGroupButton = isLocalTab && model.localAlbum != null,
                                onOpenGroupPicker = onOpenGroupPicker,
                                introSessionKey = introSessionKey,
                                animateIntro = shouldAnimateHeaderIntro,
                                messageManager = viewModel.messageManager
                            )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(pageContainerColor)
                                .clipToBounds()
                                .zIndex(0f)
                        ) {
                            AlbumDetailHeroBackground(
                                album = activeHeroAlbum,
                                height = heroHeight,
                                pageContainerColor = pageContainerColor,
                                showCoverLoadingState = showHeroCoverLoadingState,
                                onPickLocalCover = pickLocalCoverAction,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )

                            LaunchedEffect(
                                selectedTab,
                                model.rjCode,
                                model.dlsiteWorkno,
                                model.hasResolvedInitialDlsiteTarget
                            ) {
                                when (selectedTab) {
                                    1 -> {
                                        viewModel.ensureDlsiteLoaded()
                                        viewModel.ensureAsmrOneLoaded()
                                    }
                                    2 -> {
                                        viewModel.ensureDlsiteLoaded()
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .albumDetailScrolledContentFade(
                                        fadeStartY = contentFadeStartY,
                                        fadeEndY = contentFadeEndY
                                    )
                            ) {
                                when (selectedTab) {
                                    0 -> {
                                        val local = model.localAlbum
                                        if (local != null) {
                                            val localTreeStateKey = remember(albumId, rjCode, local.id) {
                                                val rjNorm = rjCode?.trim().orEmpty().uppercase()
                                                when {
                                                    albumId != null && albumId > 0 -> "localTree:id:$albumId"
                                                    rjNorm.isNotBlank() -> "localTree:rj:$rjNorm"
                                                    else -> "localTree:localId:${local.id}"
                                                }
                                            }
                                            AlbumLocalBreadcrumbTabV2(
                                                stateKey = localTreeStateKey,
                                                initialCurrentPath = viewModel.getPreferredTreeCurrentPath(localTreeStateKey)
                                                    .ifBlank { viewModel.getTreeCurrentPath(localTreeStateKey) },
                                                onPersistCurrentPath = { path ->
                                                    viewModel.persistTreeCurrentPath(localTreeStateKey, path)
                                                },
                                                initialScroll = viewModel.getListScrollPosition("scroll:$localTreeStateKey"),
                                                onPersistScroll = { index, offset ->
                                                    viewModel.persistListScrollPosition("scroll:$localTreeStateKey", index, offset)
                                                },
                                                topContentPadding = tabContentTopPadding,
                                                chromeState = tabChromeState,
                                                album = local,
                                                header = { headerContent(0) },
                                                onPlayMediaItems = onPlayMediaItems,
                                                onAddToQueue = { track ->
                                                    onAddToQueue(local, track)
                                                },
                                                onAddMediaItemsToQueue = onAddMediaItemsToQueue,
                                                onAddMediaItemsToFavorites = onAddMediaItemsToFavorites,
                                                onOpenBatchPlaylistPicker = { items -> batchPlaylistItems = items },
                                                preferredCurrentPath = viewModel.getPreferredTreeCurrentPath(localTreeStateKey),
                                                onTogglePreferredCurrentPath = { path, enabled ->
                                                    if (enabled) {
                                                        viewModel.persistPreferredTreeCurrentPath(localTreeStateKey, path)
                                                        viewModel.messageManager.showSuccess("已设为默认打开目录")
                                                    } else {
                                                        viewModel.clearPreferredTreeCurrentPath(localTreeStateKey)
                                                    }
                                                },
                                                onAddToPlaylist = { track ->
                                                    val target = PlaylistAddTarget.fromTrack(local, track)
                                                    onOpenPlaylistPicker(target.toMediaItem())
                                                },
                                                onManageTrackTags = { track ->
                                                    tagManageTrack = track
                                                },
                                                onRemoveTrack = { track ->
                                                    if (track.id > 0L) libraryViewModel.removeTrackFromAlbum(track.id)
                                                },
                                                onSetCoverFromImage = { pathOrUri ->
                                                    viewModel.setLocalCoverPath(pathOrUri)
                                                },
                                                onPreviewImages = { request -> imagePreviewRequest = request },
                                                onPreviewFile = { localPreviewFile = it },
                                                animateIntro = shouldPlayInitialAnimations
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(top = tabContentTopPadding),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("未下载到本地")
                                            }
                                        }
                                    }
                                    1 -> AlbumDlsiteInfoBreadcrumbTabV2(
                                        album = album,
                                        header = { headerContent(1) },
                                        dlsiteInfo = model.dlsiteInfo,
                                        galleryUrls = model.dlsiteGalleryUrls,
                                        trialTracks = model.dlsiteTrialTracks,
                                        trialDownloadEnabled = trialDownloadTree.isNotEmpty(),
                                        isLoading = model.isLoadingDlsite,
                                        asmrOneTree = asmrOneTree,
                                        isLoadingAsmrOne = model.isLoadingAsmrOne,
                                        isLoadingTrial = model.isLoadingDlsiteTrial,
                                        onRefreshAsmrOne = { viewModel.refreshAsmrOneSection() },
                                        onRefreshTrial = { viewModel.refreshDlsiteTrialSection() },
                                        onDownloadTrial = {
                                            downloadSource = OnlineDownloadSource.DlsiteTrial
                                            showAsmrDownloadDialog = true
                                        },
                                        onPlayTracks = onPlayTracks,
                                        onPlayMediaItems = onPlayMediaItems,
                                        onAddToQueue = { track ->
                                            onAddToQueue(album, track)
                                        },
                                        onAddMediaItemsToQueue = onAddMediaItemsToQueue,
                                        onAddMediaItemsToFavorites = onAddMediaItemsToFavorites,
                                        onOpenBatchPlaylistPicker = { items -> batchPlaylistItems = items },
                                        onDownloadOne = { relPath ->
                                            viewModel.downloadAsmrOneSelected(setOf(relPath))
                                        },
                                        onAddToPlaylistOne = { relPath ->
                                            val target = PlaylistAddTarget.fromAsmrOne(album, asmrOneTree, relPath) ?: return@AlbumDlsiteInfoBreadcrumbTabV2
                                            onOpenPlaylistPicker(target.toMediaItem())
                                        },
                                        onAddToPlaylist = { track ->
                                            val target = PlaylistAddTarget.fromTrack(album, track)
                                            onOpenPlaylistPicker(target.toMediaItem())
                                        },
                                        onPreviewImages = { request -> imagePreviewRequest = request },
                                        onPreviewFile = { onlinePreviewFile = it },
                                        treeStateKey = "tree:asmrOne:${model.rjCode.trim().uppercase()}",
                                        initialCurrentPath = viewModel.getTreeCurrentPath("tree:asmrOne:${model.rjCode.trim().uppercase()}"),
                                        topContentPadding = tabContentTopPadding,
                                        chromeState = tabChromeState,
                                        animateIntro = shouldPlayInitialAnimations,
                                        onPersistCurrentPath = { path ->
                                            val rj = model.rjCode.trim().uppercase()
                                            viewModel.persistTreeCurrentPath("tree:asmrOne:$rj", path)
                                        },
                                        initialScroll = viewModel.getListScrollPosition("scroll:tree:asmrOne:${model.rjCode.trim().uppercase()}"),
                                        onPersistScroll = { index, offset ->
                                            viewModel.persistListScrollPosition("scroll:tree:asmrOne:${model.rjCode.trim().uppercase()}", index, offset)
                                        },
                                        dlsiteRecommendations = model.dlsiteRecommendations,
                                        onOpenAlbumByRj = onOpenAlbumByRj,
                                        loadRemoteFileSize = { viewModel.loadRemoteFileSize(it) }
                                    )
                                    else -> AlbumDlsitePlayBreadcrumbTabV2(
                                        header = { headerContent(2) },
                                        album = album,
                                        rjCode = model.rjCode,
                                        tree = model.dlsitePlayTree,
                                        isLoading = model.isLoadingDlsitePlay,
                                        shouldAutoLoad = selectedTab == 2,
                                        onOpenLogin = onOpenDlsiteLogin,
                                        onEnsureLoaded = { viewModel.ensureDlsitePlayLoaded() },
                                        onPlayMediaItems = onPlayMediaItems,
                                        onAddToQueue = { track ->
                                            onAddToQueue(album, track)
                                        },
                                        onAddMediaItemsToQueue = onAddMediaItemsToQueue,
                                        onAddMediaItemsToFavorites = onAddMediaItemsToFavorites,
                                        onOpenBatchPlaylistPicker = { items -> batchPlaylistItems = items },
                                        onDownloadOne = { relPath ->
                                            viewModel.downloadDlsitePlaySelected(setOf(relPath))
                                        },
                                        onPreviewImages = { request -> imagePreviewRequest = request },
                                        onPreviewFile = { onlinePreviewFile = it },
                                        prepareImagePreview = viewModel::prepareDlsitePlayImagePreview,
                                        treeStateKey = "tree:dlsitePlay:${model.baseRjCode.ifBlank { model.rjCode }.trim().uppercase()}",
                                        initialCurrentPath = viewModel.getTreeCurrentPath("tree:dlsitePlay:${model.baseRjCode.ifBlank { model.rjCode }.trim().uppercase()}"),
                                        topContentPadding = tabContentTopPadding,
                                        chromeState = tabChromeState,
                                        animateIntro = shouldPlayInitialAnimations,
                                        onPersistCurrentPath = { path ->
                                            val rj = model.baseRjCode.ifBlank { model.rjCode }.trim().uppercase()
                                            viewModel.persistTreeCurrentPath("tree:dlsitePlay:$rj", path)
                                        },
                                        initialScroll = viewModel.getListScrollPosition("scroll:tree:dlsitePlay:${model.baseRjCode.ifBlank { model.rjCode }.trim().uppercase()}"),
                                        onPersistScroll = { index, offset ->
                                            viewModel.persistListScrollPosition("scroll:tree:dlsitePlay:${model.baseRjCode.ifBlank { model.rjCode }.trim().uppercase()}", index, offset)
                                        },
                                        loadRemoteFileSize = { viewModel.loadRemoteFileSize(it) }
                                    )
                                }
                            }
                    }
                }

                val canSaveOnline = selectedTab == 1 && asmrOneTree.isNotEmpty()
                if (showAsmrDownloadDialog) {
                    val downloadTree = when (downloadSource) {
                        OnlineDownloadSource.AsmrOne -> asmrOneTree
                        OnlineDownloadSource.DlsitePlay -> model.dlsitePlayTree
                        OnlineDownloadSource.DlsiteTrial -> trialDownloadTree
                    }
                    AsmrOneDownloadDialog(
                        albumTitle = album.title,
                        trackTree = downloadTree,
                        onDismiss = { showAsmrDownloadDialog = false },
                        onConfirm = { selected ->
                            when (downloadSource) {
                                OnlineDownloadSource.AsmrOne -> viewModel.downloadAsmrOneSelected(selected)
                                OnlineDownloadSource.DlsitePlay -> viewModel.downloadDlsitePlaySelected(selected)
                                OnlineDownloadSource.DlsiteTrial -> viewModel.downloadDlsiteTrialSelected(selected)
                            }
                            showAsmrDownloadDialog = false
                        }
                    )
                }

                if (showOnlineSaveDialog && canSaveOnline) {
                    val saveTree = if (model.dlsitePlayTree.isNotEmpty()) model.dlsitePlayTree else asmrOneTree
                    OnlineSaveDialog(
                        albumTitle = album.title,
                        trackTree = saveTree,
                        onDismiss = { showOnlineSaveDialog = false },
                        onConfirm = { selected ->
                            pendingOnlineSaveSelection = selected
                            showOnlineSaveDialog = false
                        }
                    )
                }

                batchPlaylistItems?.let { items ->
                    Dialog(
                        onDismissRequest = { batchPlaylistItems = null },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = colorScheme.background.copy(alpha = 0.96f),
                            contentColor = colorScheme.textPrimary
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp)
                            ) {
                                PlaylistPickerScreen(
                                    windowSizeClass = windowSizeClass,
                                    items = items,
                                    onBack = { batchPlaylistItems = null },
                                    embeddedInDialog = true
                                )
                            }
                        }
                    }
                }

                if (localPreviewFile != null) {
                    FilePreviewDialog(
                        title = localPreviewFile!!.title,
                        absolutePath = localPreviewFile!!.absolutePath,
                        fileType = localPreviewFile!!.fileType,
                        messageManager = viewModel.messageManager,
                        loadOnlineText = viewModel::loadOnlineTextPreview,
                        onDismiss = { localPreviewFile = null }
                    )
                }

                if (onlinePreviewFile != null) {
                    FilePreviewDialog(
                        title = onlinePreviewFile!!.title,
                        absolutePath = onlinePreviewFile!!.url ?: "",
                        fileType = onlinePreviewFile!!.fileType,
                        messageManager = viewModel.messageManager,
                        loadOnlineText = viewModel::loadOnlineTextPreview,
                        onDismiss = { onlinePreviewFile = null }
                    )
                }

                imagePreviewRequest?.let { request ->
                    ImagePreviewDialog(
                        request = request,
                        messageManager = viewModel.messageManager,
                        onDismiss = { imagePreviewRequest = null }
                    )
                }

                val track = tagManageTrack
                if (track != null && track.id > 0L) {
                    TagAssignDialog(
                        title = track.title,
                        inheritedTags = album.tags,
                        userTags = userTagsByTrackId[track.id].orEmpty(),
                        allTags = availableTags,
                        onApplyUserTags = { list ->
                            viewModel.setUserTagsForTrack(track.id, list)
                            tagManageTrack = null
                        },
                        onDismiss = { tagManageTrack = null },
                        onOpenTagManager = { showTagManager = true }
                    )
                }

                if (showTagManager) {
                    Dialog(
                        onDismissRequest = { showTagManager = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            TagManagerSheet(
                                tags = availableTags,
                                onRename = { tagId, newName -> libraryViewModel.renameUserTag(tagId, newName) },
                                onDelete = { tagId -> libraryViewModel.deleteUserTag(tagId) },
                                onClose = { showTagManager = false }
                            )
                        }
                    }
                }
            }
                is AlbumDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        cloudSyncSelectionDialogState?.let { dialogState ->
            CloudSyncSelectionDialog(
                state = dialogState,
                onSelect = viewModel::confirmCloudSyncSelection,
                onCancel = viewModel::cancelCloudSyncSelection
            )
        }
    }
}

@Composable
private fun AlbumDetailHeroBackground(
    album: Album,
    height: Dp,
    pageContainerColor: Color,
    showCoverLoadingState: Boolean,
    onPickLocalCover: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val imageModel = rememberAlbumCoverImageModel(album)
    val blurModifier = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                val blurPx = 52.dp.toPx()
                renderEffect = RenderEffect
                    .createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
        } else {
            Modifier.blur(40.dp)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
    ) {
        AsmrAsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            placeholderCornerRadius = 0,
            modifier = Modifier.fillMaxSize(),
            placeholder = { m -> DiscPlaceholder(modifier = m, cornerRadius = 0) },
            loading = { m -> AsmrShimmerPlaceholder(modifier = m, cornerRadius = 0) },
            empty = { m ->
                if (showCoverLoadingState) {
                    AsmrShimmerPlaceholder(modifier = m, cornerRadius = 0)
                } else {
                    DiscPlaceholder(modifier = m, cornerRadius = 0)
                }
            },
        )
        // 渐进式毛玻璃：在锐利封面之上叠加一层模糊副本，并用 smoothstep 缓动的垂直遮罩
        // 让模糊从中部开始、向下逐渐加强，形成自上而下平滑过渡的高级质感。
        AsmrAsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            placeholderCornerRadius = 0,
            modifier = Modifier
                .fillMaxSize()
                .then(blurModifier)
                .drawWithCache {
                    val rampStart = 0.18f
                    val rampEnd = 0.86f
                    val stops = (0..6).map { i ->
                        val t = i / 6f
                        val pos = rampStart + (rampEnd - rampStart) * t
                        val eased = t * t * (3f - 2f * t)
                        pos to Color.White.copy(alpha = eased)
                    }.toTypedArray()
                    val mask = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            rampStart to Color.Transparent,
                            *stops,
                            1f to Color.White
                        )
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush = mask, blendMode = BlendMode.DstIn)
                    }
                },
            placeholder = {},
            loading = {},
            empty = {},
        )
        // 顶部深色蒙版，保证返回按钮等控件的可读性
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.44f),
                            0.42f to Color.Black.copy(alpha = 0.16f),
                            0.70f to Color.Transparent
                        )
                    )
                )
        )
        // 底部色彩渐变，让 hero 平滑融入页面底色。
        // 在 SolidFraction 处即达到完全不透明的容器色，并保持到底部，
        // 从而彻底遮住封面被裁切的底部边缘，消除割裂感。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            AlbumDetailHeroThemeGradientStartFraction to Color.Transparent,
                            ((AlbumDetailHeroThemeGradientStartFraction + AlbumDetailHeroThemeGradientSolidFraction) / 2f) to
                                pageContainerColor.copy(alpha = 0.70f),
                            AlbumDetailHeroThemeGradientSolidFraction to pageContainerColor,
                            1f to pageContainerColor
                        )
                    )
                )
        )
        if (onPickLocalCover != null) {
            IconButton(
                onClick = onPickLocalCover,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 14.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.38f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Photo,
                    contentDescription = "选择封面",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun rememberAlbumCoverImageModel(album: Album): Any {
    val data = album.coverPath.trim().ifEmpty { album.coverUrl.trim() }
    return remember(data) {
        val headers = if (data.startsWith("http", ignoreCase = true)) {
            DlsiteAntiHotlink.headersForImageUrl(data)
        } else {
            emptyMap()
        }
        if (headers.isEmpty()) {
            data
        } else {
            CacheImageModel(data = data, headers = headers, keyTag = "dlsite")
        }
    }
}

private fun Modifier.albumDetailScrolledContentFade(
    fadeStartY: Dp,
    fadeEndY: Dp
): Modifier {
    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()
            val fadeStartPx = fadeStartY.toPx().coerceAtLeast(0f)
            val fadeEndPx = fadeEndY.toPx().coerceAtLeast(fadeStartPx + 1f)
            val rampStart = (fadeStartPx / fadeEndPx).coerceIn(0f, 1f)
            val rampSpan = (1f - rampStart).coerceAtLeast(0.0001f)
            // 用 smoothstep 缓动的多段渐变替代线性裁切，使内容向上滚入 hero 区域时
            // 平滑自然地溶解消失，而不是生硬地一刀切。
            fun stopAt(t: Float): Pair<Float, Color> {
                val eased = t * t * (3f - 2f * t)
                return (rampStart + rampSpan * t) to Color.White.copy(alpha = eased)
            }
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        rampStart to Color.Transparent,
                        stopAt(0.2f),
                        stopAt(0.4f),
                        stopAt(0.6f),
                        stopAt(0.8f),
                        1f to Color.White
                    ),
                    startY = 0f,
                    endY = fadeEndPx
                ),
                blendMode = BlendMode.DstIn
            )
        }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AlbumHeader(
    album: Album,
    listenTogetherRjListenerCount: Int?,
    dlsiteUrl: String,
    asmrOneUrl: String,
    dlsiteEditions: List<DlsiteLanguageEdition>,
    dlsiteSelectedLang: String,
    onDlsiteLangSelected: (String) -> Unit,
    canSaveOnline: Boolean,
    onDownloadClick: () -> Unit,
    showDlsitePlayLossless: Boolean,
    onLosslessDownloadClick: () -> Unit,
    onSaveClick: () -> Unit,
    downloadEnabled: Boolean,
    losslessDownloadEnabled: Boolean,
    saveEnabled: Boolean,
    showGroupButton: Boolean,
    onOpenGroupPicker: (albumId: Long) -> Unit,
    introSessionKey: String,
    animateIntro: Boolean,
    messageManager: MessageManager
) {
    val context = LocalContext.current
    val colorScheme = AsmrTheme.colorScheme
    val copyMeta = rememberAlbumMetaCopyAction(messageManager)

    val rj = album.rjCode.ifBlank { album.workId }
    val normalizedTitle = album.title.trim()
    val isPlaceholderTitle = normalizedTitle.isBlank() ||
        (normalizedTitle.equals(rj, ignoreCase = true) && album.id <= 0L && album.path.isBlank())
    val headerAnimationScopeKey = remember(introSessionKey) { "albumHeader:$introSessionKey" }
    var headerIntroPlayed by rememberSaveable(headerAnimationScopeKey) { mutableStateOf(false) }
    LaunchedEffect(headerAnimationScopeKey, animateIntro) {
        if (headerIntroPlayed) return@LaunchedEffect
        if (!animateIntro) {
            headerIntroPlayed = true
            return@LaunchedEffect
        }
        delay(700)
        headerIntroPlayed = true
    }

    val headerContainerModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AlbumDetailHorizontalPadding)
    val langCandidates = remember(dlsiteEditions) {
        dlsiteEditions
            .filter { it.lang in setOf("JPN", "CHI_HANS", "CHI_HANT") }
            .distinctBy { it.lang }
            .sortedWith(compareBy({ it.displayOrder }, { it.lang }))
    }
    val selectedLangLabel = remember(dlsiteSelectedLang, langCandidates) {
        langCandidates.firstOrNull { it.lang.equals(dlsiteSelectedLang, ignoreCase = true) }
            ?.let { dlsiteLanguageButtonLabel(it.lang) }
            ?: dlsiteLanguageButtonLabel(dlsiteSelectedLang)
    }
    var languageMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = dlsiteElasticItemModifier(
            modifier = headerContainerModifier,
            enabled = animateIntro && !headerIntroPlayed
        )
            .padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AlbumHeaderInfoReveal(
            revealKey = "$headerAnimationScopeKey:title",
            delayMillis = 0,
            enabled = animateIntro,
            ready = !isPlaceholderTitle
        ) {
            Text(
                text = album.title,
                modifier = Modifier.clickable { copyMeta("标题", album.title) },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        val circle = album.circle.trim()
        val showMetaRow = rj.isNotBlank() || circle.isNotBlank() || listenTogetherRjListenerCount != null
        if (showMetaRow) {
            AlbumHeaderInfoReveal(
                revealKey = "$headerAnimationScopeKey:meta",
                delayMillis = 40,
                enabled = animateIntro
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumPrimaryMetaRow(
                        rjCode = rj,
                        circle = circle,
                        modifier = Modifier.weight(1f),
                        rjOnClick = { copyMeta("RJ", rj) },
                        circleOnClick = { copyMeta("社团", circle) },
                        leadingVisual = AlbumMetaLeadingVisual.Icon,
                    )
                    if (listenTogetherRjListenerCount != null && rj.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AlbumHeaderInfoReveal(
                            revealKey = "$headerAnimationScopeKey:listenTogetherRjCount",
                            delayMillis = 60,
                            enabled = animateIntro
                        ) {
                            Surface(
                                color = colorScheme.primary.copy(alpha = if (colorScheme.isDark) 0.16f else 0.10f),
                                contentColor = colorScheme.primary,
                                shape = RoundedCornerShape(999.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 0.5.dp,
                                    color = colorScheme.primary.copy(alpha = 0.18f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = com.asmr.player.R.drawable.ic_users_round),
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${listenTogetherRjListenerCount.coerceAtLeast(0)} 人正在听",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
                if (album.cv.isNotBlank()) {
                    AlbumHeaderInfoReveal(
                        revealKey = "$headerAnimationScopeKey:cv",
                        delayMillis = 80,
                        enabled = animateIntro
                    ) {
                        AlbumCvChipsFlow(
                            cvText = album.cv,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            onCvClick = { cv -> copyMeta("CV", cv) },
                            leadingVisual = AlbumMetaLeadingVisual.Icon,
                        )
                    }
                }

                if (album.tags.isNotEmpty()) {
                    AlbumHeaderInfoReveal(
                        revealKey = "$headerAnimationScopeKey:tags",
                        delayMillis = 120,
                        enabled = animateIntro
                    ) {
                        AlbumTagsFlow(
                            tags = album.tags,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            onTagClick = { tag -> copyMeta("标签", tag) },
                            leadingVisual = AlbumMetaLeadingVisual.Icon,
                        )
                    }
                }

                AlbumHeaderInfoReveal(
                    revealKey = "$headerAnimationScopeKey:actions",
                    delayMillis = 160,
                    enabled = animateIntro
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        val compact = maxWidth < 400.dp
                        val ultraCompact = maxWidth < 340.dp
                        val actionGap = if (compact) 8.dp else 10.dp
                        val primaryButtonPadding = when {
                            ultraCompact -> 6.dp
                            compact -> 8.dp
                            else -> 12.dp
                        }
                        val smallButtonPadding = when {
                            ultraCompact -> 6.dp
                            compact -> 8.dp
                            else -> 12.dp
                        }
                        val primaryIconSize = if (compact) 16.dp else 18.dp
                        val primaryIconGap = if (compact) 4.dp else 6.dp
                        val selectorMinWidth = when {
                            ultraCompact -> 68.dp
                            compact -> 76.dp
                            else -> 96.dp
                        }
                        val selectorMaxWidth = when {
                            ultraCompact -> 92.dp
                            compact -> 104.dp
                            else -> 140.dp
                        }
                        val externalMinWidth = when {
                            ultraCompact -> 46.dp
                            compact -> 50.dp
                            else -> 56.dp
                        }
                        val externalMaxWidth = when {
                            ultraCompact -> 58.dp
                            compact -> 64.dp
                            else -> 76.dp
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(actionGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .height(36.dp)
                                    .weight(1f)
                            ) {
                                val radius = 10.dp
                                val hasSecondaryPrimaryAction = canSaveOnline || showDlsitePlayLossless
                                val leftShape = if (hasSecondaryPrimaryAction) {
                                    RoundedCornerShape(topStart = radius, bottomStart = radius, topEnd = 0.dp, bottomEnd = 0.dp)
                                } else {
                                    RoundedCornerShape(radius)
                                }
                                val rightShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = radius, bottomEnd = radius)

                                Button(
                                    onClick = onDownloadClick,
                                    enabled = downloadEnabled,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    shape = leftShape,
                                    contentPadding = PaddingValues(horizontal = primaryButtonPadding, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(primaryIconSize))
                                    Spacer(modifier = Modifier.width(primaryIconGap))
                                    Text("下载", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }

                                if (showDlsitePlayLossless) {
                                    Button(
                                        onClick = onLosslessDownloadClick,
                                        enabled = losslessDownloadEnabled,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(1f),
                                        shape = rightShape,
                                        contentPadding = PaddingValues(horizontal = primaryButtonPadding, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colorScheme.primary.copy(alpha = 0.14f),
                                            contentColor = colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(primaryIconSize))
                                        Spacer(modifier = Modifier.width(primaryIconGap))
                                        Text("无损下载", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                    }
                                } else if (canSaveOnline) {
                                    Button(
                                        onClick = onSaveClick,
                                        enabled = saveEnabled,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(1f),
                                        shape = rightShape,
                                        contentPadding = PaddingValues(horizontal = primaryButtonPadding, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colorScheme.primary.copy(alpha = 0.14f),
                                            contentColor = colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Rounded.Bookmark, contentDescription = null, modifier = Modifier.size(primaryIconSize))
                                        Spacer(modifier = Modifier.width(primaryIconGap))
                                        Text("保存", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                    }
                                }
                            }

                            if (showGroupButton) {
                                OutlinedButton(
                                    onClick = {
                                        val id = album.id
                                        if (id > 0L) onOpenGroupPicker(id)
                                    },
                                    enabled = album.id > 0L,
                                    modifier = Modifier
                                        .height(36.dp)
                                        .widthIn(min = selectorMinWidth, max = selectorMaxWidth),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = smallButtonPadding, vertical = 0.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Icon(
                                        Icons.Rounded.CreateNewFolder,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(primaryIconSize)
                                    )
                                    Spacer(modifier = Modifier.width(primaryIconGap))
                                    Text("分组", style = MaterialTheme.typography.labelMedium, color = colorScheme.primary, maxLines = 1)
                                }
                            }

                            if (langCandidates.size > 1) {
                                Box {
                                    OutlinedButton(
                                        onClick = { languageMenuExpanded = true },
                                        modifier = Modifier
                                            .height(36.dp)
                                            .widthIn(min = selectorMinWidth, max = selectorMaxWidth),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = smallButtonPadding, vertical = 0.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = selectedLangLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colorScheme.primary,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(if (compact) 2.dp else 4.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDropDown,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(primaryIconSize)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = languageMenuExpanded,
                                        onDismissRequest = { languageMenuExpanded = false }
                                    ) {
                                        langCandidates.forEach { edition ->
                                            DropdownMenuItem(
                                                text = { Text(dlsiteLanguageButtonLabel(edition.lang)) },
                                                onClick = {
                                                    languageMenuExpanded = false
                                                    onDlsiteLangSelected(edition.lang)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            listOf(
                                "DLsite" to dlsiteUrl,
                                "ONE" to asmrOneUrl
                            ).forEach { (label, url) ->
                                OutlinedButton(
                                    onClick = {
                                        if (url.isNotBlank()) {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    },
                                    enabled = url.isNotBlank(),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .widthIn(min = externalMinWidth, max = externalMaxWidth),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = smallButtonPadding, vertical = 0.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelMedium, color = colorScheme.primary, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
private fun dlsiteLanguageButtonLabel(lang: String): String {
    return when (lang.trim().uppercase()) {
        "CHI_HANS" -> "简中"
        "CHI_HANT" -> "繁中"
        "JPN" -> "日语"
        else -> lang.trim().ifBlank { "日语" }
    }
}

@Composable
private fun AlbumHeaderInfoReveal(
    revealKey: String,
    delayMillis: Int = 0,
    enabled: Boolean = true,
    ready: Boolean = true,
    content: @Composable () -> Unit
) {
    var hasPlayed by rememberSaveable(revealKey) { mutableStateOf(false) }
    LaunchedEffect(revealKey, enabled, ready) {
        if (!enabled && !hasPlayed) {
            hasPlayed = true
        }
    }
    if (!enabled || hasPlayed) {
        content()
        return
    }
    if (!ready) {
        content()
        return
    }
    var visible by remember(revealKey) { mutableStateOf(false) }
    LaunchedEffect(revealKey, ready, enabled) {
        visible = false
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        withFrameNanos { }
        visible = true
        delay(420)
        hasPlayed = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "albumHeaderInfoAlpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 10.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "albumHeaderInfoOffsetY"
    )
    val density = LocalDensity.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 220)) + expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            expandFrom = Alignment.Top
        ),
        exit = fadeOut(animationSpec = tween(durationMillis = 120)) + shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            shrinkTowards = Alignment.Top
        )
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha
                translationY = with(density) { offsetY.toPx() }
            }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
internal fun isVideoPreviewUrl(url: String): Boolean {
    val u = url.substringBefore('#').substringBefore('?').lowercase()
    return u.endsWith(".mp4") || u.endsWith(".mkv") || u.endsWith(".webm") || u.endsWith(".m3u8")
}

internal data class PlaylistAddTarget(
    val mediaId: String,
    val uri: String,
    val title: String,
    val artist: String,
    val artworkUri: String,
    val albumTitle: String = "",
    val albumId: Long = 0L,
    val trackId: Long = 0L,
    val rjCode: String = "",
    val albumWorkId: String = "",
    val trackGroup: String = "",
    val lyricsRelativePathNoExt: String = "",
    val remoteSubtitleSources: List<RemoteSubtitleSource> = emptyList(),
    val mimeType: String? = null,
    val isVideo: Boolean = false
) {
    fun toMediaItem(): MediaItem {
        return MediaItemFactory.fromDetails(
            mediaId = mediaId,
            uri = uri,
            title = title,
            artist = artist,
            albumTitle = albumTitle,
            artworkUri = artworkUri,
            albumId = albumId,
            trackId = trackId,
            rjCode = rjCode,
            albumWorkId = albumWorkId,
            trackGroup = trackGroup,
            lyricsRelativePathNoExt = lyricsRelativePathNoExt,
            remoteSubtitleSources = remoteSubtitleSources,
            mimeType = mimeType,
            isVideo = isVideo
        )
    }

    companion object {
        fun fromTrack(album: Album, track: Track): PlaylistAddTarget {
            val rj = album.rjCode.ifBlank { album.workId }
            val artist = albumArtistLabel(album).ifBlank { rj }
            val artwork = albumArtworkLabel(album)
            val title = track.title.ifBlank { track.path.substringAfterLast('/').substringAfterLast('\\') }
            return PlaylistAddTarget(
                mediaId = track.path,
                uri = track.path,
                title = title,
                artist = artist.orEmpty(),
                artworkUri = artwork,
                albumTitle = album.title,
                albumId = album.id,
                trackId = track.id,
                rjCode = rj,
                albumWorkId = album.workId,
                trackGroup = track.group,
                lyricsRelativePathNoExt = deriveLyricsRelativePathNoExt(track.path, album.getAllLocalPaths())
            )
        }

        fun fromVideo(
            album: Album,
            title: String,
            uriOrPath: String
        ): PlaylistAddTarget? {
            val trimmed = uriOrPath.trim()
            if (trimmed.isBlank()) return null
            return PlaylistAddTarget(
                mediaId = trimmed,
                uri = trimmed,
                title = title.ifBlank { trimmed.substringAfterLast('/').substringAfterLast('\\') },
                artist = albumArtistLabel(album),
                artworkUri = albumArtworkLabel(album),
                albumTitle = album.title,
                albumId = album.id,
                rjCode = album.rjCode.ifBlank { album.workId },
                albumWorkId = album.workId,
                mimeType = MediaItemFactory.guessMimeType(trimmed),
                isVideo = true
            )
        }

        fun fromAsmrOne(album: Album, tree: List<AsmrOneTrackNodeResponse>, relativePath: String): PlaylistAddTarget? {
            val leaf = flattenAsmrOneTracksForUi(tree).firstOrNull { it.relativePath == relativePath } ?: return null
            return fromTrack(album, leaf.toTrack())
        }
    }
}



