package com.seogoapp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seogoapp.data.model.Folder
import com.seogoapp.data.model.FolderWithSceneCount
import com.seogoapp.data.model.SceneWithTags
import com.seogoapp.data.repository.FolderRepository
import com.seogoapp.data.repository.SceneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val folders: List<FolderWithSceneCount> = emptyList(),
    val searchResults: List<SceneWithTags> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val showAddFolderDialog: Boolean = false,
    val folderToRename: Folder? = null,
    val folderToDelete: Folder? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val sceneRepository: SceneRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        // 서랍 목록 실시간 관찰
        folderRepository.getAllFolders()
            .onEach { folders ->
                _uiState.update { it.copy(folders = folders) }
            }
            .launchIn(viewModelScope)

        // 검색어 디바운스 → 전체 검색
        searchQuery
            .debounce(300)
            .flatMapLatest { query ->
                if (query.isBlank()) flowOf(emptyList())
                else sceneRepository.searchAll(query)
            }
            .onEach { results ->
                _uiState.update { it.copy(searchResults = results) }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
    }

    fun clearSearch() {
        searchQuery.value = ""
        _uiState.update { it.copy(searchQuery = "", isSearching = false, searchResults = emptyList()) }
    }

    fun showAddFolderDialog() {
        _uiState.update { it.copy(showAddFolderDialog = true) }
    }

    fun dismissAddFolderDialog() {
        _uiState.update { it.copy(showAddFolderDialog = false) }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            folderRepository.createFolder(name.trim())
            _uiState.update { it.copy(showAddFolderDialog = false) }
        }
    }

    fun requestRename(folder: Folder) {
        _uiState.update { it.copy(folderToRename = folder) }
    }

    fun confirmRename(newName: String) {
        val folder = _uiState.value.folderToRename ?: return
        if (newName.isBlank()) {
            _uiState.update { it.copy(folderToRename = null) }
            return
        }
        viewModelScope.launch {
            folderRepository.renameFolder(folder, newName.trim())
            _uiState.update { it.copy(folderToRename = null) }
        }
    }

    fun dismissRename() {
        _uiState.update { it.copy(folderToRename = null) }
    }

    fun requestDelete(folder: Folder) {
        _uiState.update { it.copy(folderToDelete = folder) }
    }

    fun confirmDelete() {
        val folder = _uiState.value.folderToDelete ?: return
        viewModelScope.launch {
            folderRepository.deleteFolder(folder)
            _uiState.update { it.copy(folderToDelete = null) }
        }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(folderToDelete = null) }
    }
}
