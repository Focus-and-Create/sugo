package com.seogoapp.ui.drawer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seogoapp.data.model.Folder
import com.seogoapp.data.model.SceneWithTags
import com.seogoapp.data.model.Tag
import com.seogoapp.data.repository.FolderRepository
import com.seogoapp.data.repository.SceneRepository
import com.seogoapp.domain.importer.ImportResult
import com.seogoapp.domain.importer.SceneImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DrawerUiState(
    val folder: Folder? = null,
    val scenes: List<SceneWithTags> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val availableCharacters: List<String> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val selectedCharacter: String? = null,
    val selectedTag: String? = null,
    val isImporting: Boolean = false,
    val importError: String? = null,
    val importSuccessTitle: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class DrawerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val folderRepository: FolderRepository,
    private val sceneRepository: SceneRepository,
    private val sceneImporter: SceneImporter
) : ViewModel() {

    private val folderId: Long = checkNotNull(savedStateHandle["folderId"])

    private val _uiState = MutableStateFlow(DrawerUiState())
    val uiState: StateFlow<DrawerUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val selectedCharacter = MutableStateFlow<String?>(null)
    private val selectedTag = MutableStateFlow<String?>(null)

    init {
        // 서랍 정보
        viewModelScope.launch {
            val folder = folderRepository.getFolderById(folderId)
            _uiState.update { it.copy(folder = folder) }
        }

        // 씬 목록 — 검색/필터에 따라 동적으로 변경
        combine(searchQuery.debounce(300), selectedCharacter, selectedTag) { q, char, tag ->
            Triple(q, char, tag)
        }.flatMapLatest { (query, char, tag) ->
            when {
                query.isNotBlank() -> sceneRepository.searchInFolder(folderId, query)
                char != null -> sceneRepository.filterByCharacter(folderId, char)
                tag != null -> sceneRepository.filterByTag(folderId, tag)
                else -> sceneRepository.getScenesInFolder(folderId)
            }
        }.onEach { scenes ->
            _uiState.update { it.copy(scenes = scenes) }
        }.launchIn(viewModelScope)

        // 서랍 내 캐릭터 목록 (초기 1회 로드, 씬 변경 시 갱신)
        sceneRepository.getScenesInFolder(folderId)
            .onEach { _ -> refreshMeta() }
            .launchIn(viewModelScope)
    }

    private suspend fun refreshMeta() {
        val chars = sceneRepository.getCharactersInFolder(folderId)
        _uiState.update { it.copy(availableCharacters = chars) }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _uiState.update {
            it.copy(
                searchQuery = query,
                isSearching = query.isNotBlank(),
                selectedCharacter = null,
                selectedTag = null
            )
        }
        selectedCharacter.value = null
        selectedTag.value = null
    }

    fun selectCharacterFilter(character: String?) {
        selectedCharacter.value = character
        selectedTag.value = null
        searchQuery.value = ""
        _uiState.update {
            it.copy(
                selectedCharacter = character,
                selectedTag = null,
                searchQuery = "",
                isSearching = false
            )
        }
    }

    fun selectTagFilter(tagName: String?) {
        selectedTag.value = tagName
        selectedCharacter.value = null
        searchQuery.value = ""
        _uiState.update {
            it.copy(
                selectedTag = tagName,
                selectedCharacter = null,
                searchQuery = "",
                isSearching = false
            )
        }
    }

    fun clearFilter() {
        selectedCharacter.value = null
        selectedTag.value = null
        searchQuery.value = ""
        _uiState.update {
            it.copy(
                selectedCharacter = null,
                selectedTag = null,
                searchQuery = "",
                isSearching = false
            )
        }
    }

    fun importScene(uri: Uri) {
        _uiState.update { it.copy(isImporting = true, importError = null) }
        viewModelScope.launch {
            when (val result = sceneImporter.importFromUri(folderId, uri)) {
                is ImportResult.Success -> {
                    _uiState.update {
                        it.copy(isImporting = false, importSuccessTitle = result.title)
                    }
                    refreshMeta()
                }
                is ImportResult.NeedManualTitle -> {
                    // Phase 3에서 수동 제목 입력 화면 구현
                    _uiState.update {
                        it.copy(isImporting = false, importSuccessTitle = "씬 추가됨 (제목 미확인)")
                    }
                }
                is ImportResult.Error -> {
                    _uiState.update { it.copy(isImporting = false, importError = result.message) }
                }
            }
        }
    }

    fun importImage(uri: Uri) {
        _uiState.update { it.copy(isImporting = true, importError = null) }
        viewModelScope.launch {
            when (val result = sceneImporter.importImageFromUri(folderId, uri)) {
                is ImportResult.Success -> {
                    _uiState.update {
                        it.copy(isImporting = false, importSuccessTitle = result.title)
                    }
                    refreshMeta()
                }
                is ImportResult.NeedManualTitle -> {
                    _uiState.update {
                        it.copy(isImporting = false, importSuccessTitle = "이미지 추가됨")
                    }
                }
                is ImportResult.Error -> {
                    _uiState.update { it.copy(isImporting = false, importError = result.message) }
                }
            }
        }
    }

    fun importMultipleImages(uris: List<Uri>) {
        _uiState.update { it.copy(isImporting = true, importError = null) }
        viewModelScope.launch {
            when (val result = sceneImporter.importMultipleImagesFromUris(folderId, uris)) {
                is ImportResult.Success -> {
                    _uiState.update {
                        it.copy(isImporting = false, importSuccessTitle = result.title)
                    }
                    refreshMeta()
                }
                is ImportResult.NeedManualTitle -> {
                    _uiState.update {
                        it.copy(isImporting = false, importSuccessTitle = "이미지 추가됨")
                    }
                }
                is ImportResult.Error -> {
                    _uiState.update { it.copy(isImporting = false, importError = result.message) }
                }
            }
        }
    }

    fun clearImportMessage() {
        _uiState.update { it.copy(importError = null, importSuccessTitle = null) }
    }
}
