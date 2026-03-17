package com.seogoapp.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.seogoapp.data.model.FolderWithSceneCount
import com.seogoapp.ui.components.SceneCard
import com.seogoapp.ui.components.SeogoSearchBar
import com.seogoapp.ui.theme.NotionTextSub
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onFolderClick: (Long) -> Unit,
    onSceneClick: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── 서랍 추가 다이얼로그 ──
    if (uiState.showAddFolderDialog) {
        FolderNameDialog(
            title = "새 서랍",
            confirmLabel = "만들기",
            onConfirm = viewModel::createFolder,
            onDismiss = viewModel::dismissAddFolderDialog
        )
    }

    // ── 이름 변경 다이얼로그 ──
    uiState.folderToRename?.let { folder ->
        FolderNameDialog(
            title = "이름 변경",
            initialValue = folder.name,
            confirmLabel = "변경",
            onConfirm = viewModel::confirmRename,
            onDismiss = viewModel::dismissRename
        )
    }

    // ── 삭제 확인 다이얼로그 ──
    uiState.folderToDelete?.let { fw ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("서랍 삭제") },
            text = { Text("'${fw.name}' 서랍과 안에 있는 모든 씬이 삭제됩니다.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "서고",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddFolderDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "서랍 추가")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── 검색창 ──
            item {
                SeogoSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "씬 전체 검색",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }

            // ── 검색 결과 ──
            if (uiState.isSearching) {
                if (uiState.searchResults.isEmpty() && uiState.searchQuery.isNotBlank()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("검색 결과 없음", color = NotionTextSub)
                        }
                    }
                } else {
                    items(uiState.searchResults, key = { it.scene.sceneId }) { sceneWithTags ->
                        SceneCard(
                            sceneWithTags = sceneWithTags,
                            onClick = { onSceneClick(sceneWithTags.scene.sceneId) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                return@LazyColumn
            }

            // ── 서랍 목록 ──
            if (uiState.folders.isEmpty()) {
                item {
                    EmptyLibraryHint(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp))
                }
            } else {
                items(uiState.folders, key = { it.folder.folderId }) { fw ->
                    FolderItem(
                        fw = fw,
                        onClick = { onFolderClick(fw.folder.folderId) },
                        onLongClick = {
                            // 길게 누르면 드롭다운
                        },
                        onRename = { viewModel.requestRename(fw.folder) },
                        onDelete = { viewModel.requestDelete(fw.folder) },
                        modifier = Modifier.combinedClickable(
                            onClick = { onFolderClick(fw.folder.folderId) },
                            onLongClick = {}
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── 서랍 행 ──
@Composable
private fun FolderItem(
    fw: FolderWithSceneCount,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fw.folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = buildString {
                        append("${fw.sceneCount}개 씬")
                        fw.lastImportedAt?.let { append(" · ${formatDate(it)}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = NotionTextSub
                )
            }
            // 컨텍스트 메뉴
            Box {
                TextButton(onClick = { showMenu = true }) {
                    Text("⋯", color = NotionTextSub)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("이름 변경") },
                        onClick = { showMenu = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("삭제", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

// ── 빈 상태 힌트 ──
@Composable
private fun EmptyLibraryHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("서랍이 없습니다", style = MaterialTheme.typography.titleMedium, color = NotionTextSub)
        Spacer(Modifier.height(4.dp))
        Text("+ 버튼으로 첫 서랍을 만들어 보세요", style = MaterialTheme.typography.bodySmall, color = NotionTextSub)
    }
}

// ── 서랍 이름 입력 다이얼로그 ──
@Composable
private fun FolderNameDialog(
    title: String,
    initialValue: String = "",
    confirmLabel: String = "확인",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("서랍 이름") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

private val dateFormat = SimpleDateFormat("yy.MM.dd", Locale.KOREA)
private fun formatDate(epochMs: Long) = dateFormat.format(Date(epochMs))
