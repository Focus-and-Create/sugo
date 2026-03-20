package com.seogoapp.ui.drawer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import com.seogoapp.data.model.SceneWithTags
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seogoapp.ui.components.CharacterChip
import com.seogoapp.ui.components.SceneCard
import com.seogoapp.ui.components.SeogoSearchBar
import com.seogoapp.ui.components.TagChip
import com.seogoapp.ui.theme.NotionTextSub

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DrawerScreen(
    folderId: Long,
    onBack: () -> Unit,
    onSceneClick: (String) -> Unit,
    viewModel: DrawerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // SAF 파일 선택 런처 (HTML)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importScene(it) }
    }

    // 이미지 선택 런처 (여러 장 선택 가능)
    val imageImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.size == 1) {
            viewModel.importImage(uris.first())
        } else if (uris.size > 1) {
            viewModel.importMultipleImages(uris)
        }
    }

    // 스낵바
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.importError) {
        uiState.importError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessage()
        }
    }
    LaunchedEffect(uiState.importSuccessTitle) {
        uiState.importSuccessTitle?.let {
            snackbarHostState.showSnackbar("'$it' 씬 추가됨")
            viewModel.clearImportMessage()
        }
    }

    // 삭제 확인 다이얼로그
    var sceneToDelete by remember { mutableStateOf<SceneWithTags?>(null) }
    sceneToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { sceneToDelete = null },
            title = { Text("씬 삭제") },
            text = { Text("'${target.scene.title}'을(를) 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteScene(target.scene)
                        sceneToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { sceneToDelete = null }) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.folder?.name ?: "",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            imageImportLauncher.launch(arrayOf("image/*"))
                        }
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "이미지 가져오기")
                    }
                    IconButton(
                        onClick = { importLauncher.launch(arrayOf("text/html", "text/*")) }
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = "HTML 가져오기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── 검색창 ──
            item {
                SeogoSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "씬 검색",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp)
                )
            }

            // ── 캐릭터 필터 칩 ──
            if (uiState.availableCharacters.isNotEmpty()) {
                item {
                    FilterRow(
                        characters = uiState.availableCharacters,
                        selectedCharacter = uiState.selectedCharacter,
                        onCharacterClick = { char ->
                            if (uiState.selectedCharacter == char) viewModel.clearFilter()
                            else viewModel.selectCharacterFilter(char)
                        }
                    )
                }
            }

            // ── 씬 목록 ──
            if (uiState.scenes.isEmpty()) {
                item {
                    EmptyDrawerHint(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp)
                    )
                }
            } else {
                items(uiState.scenes, key = { it.scene.sceneId }) { sceneWithTags ->
                    SceneCard(
                        sceneWithTags = sceneWithTags,
                        onClick = { onSceneClick(sceneWithTags.scene.sceneId) },
                        onLongClick = { sceneToDelete = sceneWithTags }
                    )
                }
            }

            // 로딩 인디케이터
            if (uiState.isImporting) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FilterRow(
    characters: List<String>,
    selectedCharacter: String?,
    onCharacterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        characters.forEach { char ->
            val isSelected = char == selectedCharacter
            if (isSelected) {
                FilterChip(
                    selected = true,
                    onClick = { onCharacterClick(char) },
                    label = { Text(char, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(4.dp)
                )
            } else {
                CharacterChip(
                    name = char,
                    modifier = Modifier.noRippleClickable { onCharacterClick(char) }
                )
            }
        }
    }
}

@Composable
private fun EmptyDrawerHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("씬이 없습니다", style = MaterialTheme.typography.titleMedium, color = NotionTextSub)
        Spacer(Modifier.height(4.dp))
        Text("↑ 상단 버튼으로 HTML 또는 이미지를 가져오세요", style = MaterialTheme.typography.bodySmall, color = NotionTextSub)
    }
}

// 클릭 가능한 Modifier (ripple 없이)
@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(indication = null, interactionSource = interactionSource, onClick = onClick)
}
