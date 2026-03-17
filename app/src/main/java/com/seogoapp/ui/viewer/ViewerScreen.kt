package com.seogoapp.ui.viewer

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seogoapp.domain.parser.HtmlParser
import com.seogoapp.ui.components.CharacterChip
import com.seogoapp.ui.theme.NotionSurface
import com.seogoapp.ui.theme.NotionText
import com.seogoapp.ui.theme.NotionTextSub

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    sceneId: String,
    onBack: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scene = uiState.sceneWithTags?.scene
    val characters = remember(scene?.characters) {
        scene?.let { HtmlParser.fromJson(it.characters) } ?: emptyList()
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (scene == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("씬을 불러올 수 없습니다.", color = NotionTextSub)
        }
        return
    }

    // 메모 편집 다이얼로그
    if (uiState.isMemoEditing) {
        MemoEditDialog(
            initialMemo = uiState.memoInput,
            onMemoChange = viewModel::onMemoInput,
            onSave = viewModel::saveMemo,
            onDismiss = viewModel::cancelMemoEdit
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        scene.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            ViewerBottomBar(
                onMemoClick = viewModel::startMemoEdit,
                onToggleRaw = viewModel::toggleRawHtml,
                isRawMode = uiState.showRawHtml
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── 캐릭터 칩 행 ──
            if (characters.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    characters.forEach { char -> CharacterChip(name = char) }
                }
            }

            // ── 본문: WebView or Raw HTML 텍스트 ──
            if (uiState.showRawHtml) {
                // 원본 HTML 텍스트 보기
                Text(
                    text = scene.contentHtml,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = NotionText,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                // WebView 렌더링
                SceneWebView(
                    html = scene.contentHtml,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ── 메모 카드 ──
            scene.memo?.let { memo ->
                if (memo.isNotBlank()) {
                    MemoCard(
                        memo = memo,
                        onEdit = viewModel::startMemoEdit,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

// ── WebView 컴포저블 ──
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SceneWebView(html: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = false  // 백업 HTML은 보통 JS 불필요
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            webViewClient = WebViewClient()
        }
    }

    // 모바일 친화적 CSS 주입
    val styledHtml = remember(html) { injectMobileStyle(html) }

    AndroidView(
        factory = { webView },
        update = {
            it.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}

/** 원본 HTML에 모바일 뷰포트 + 기본 스타일 주입 */
private fun injectMobileStyle(html: String): String {
    val style = """
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          body { font-family: -apple-system, sans-serif; font-size: 15px;
                 line-height: 1.6; color: #37352f; padding: 16px; margin: 0; }
          img  { max-width: 100%; height: auto; border-radius: 8px; }
          a    { color: #4f6ef7; }
          .toot        { margin-bottom: 24px; padding-bottom: 16px;
                         border-bottom: 1px solid #e9e9e7; }
          .username    { font-weight: 600; font-size: 14px; color: #787774;
                         margin-bottom: 6px; }
          .content     { font-size: 15px; }
          .content p   { margin: 0 0 8px; }
          .media       { margin-top: 8px; }
          span.h-card  { color: #4f6ef7; }
        </style>
    """.trimIndent()

    return if (html.contains("<head>", ignoreCase = true)) {
        html.replaceFirst(Regex("<head>", RegexOption.IGNORE_CASE), "<head>$style")
    } else {
        "<html><head>$style</head><body>$html</body></html>"
    }
}

// ── 하단 툴바 ──
@Composable
private fun ViewerBottomBar(
    onMemoClick: () -> Unit,
    onToggleRaw: () -> Unit,
    isRawMode: Boolean
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        modifier = Modifier.height(56.dp)
    ) {
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onMemoClick) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("메모")
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onToggleRaw) {
            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (isRawMode) "렌더" else "원본")
        }
        Spacer(Modifier.width(8.dp))
    }
}

// ── 메모 카드 ──
@Composable
private fun MemoCard(
    memo: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = NotionSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("메모", style = MaterialTheme.typography.labelSmall, color = NotionTextSub)
            Spacer(Modifier.height(4.dp))
            Text(memo, style = MaterialTheme.typography.bodyMedium, color = NotionText)
        }
    }
}

// ── 메모 편집 다이얼로그 ──
@Composable
private fun MemoEditDialog(
    initialMemo: String,
    onMemoChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("메모") },
        text = {
            OutlinedTextField(
                value = initialMemo,
                onValueChange = onMemoChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                placeholder = { Text("씬에 대한 메모를 남겨보세요") },
                maxLines = 8
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
