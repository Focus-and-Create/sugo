package com.seogoapp.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seogoapp.data.model.SceneWithTags
import com.seogoapp.data.repository.SceneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val sceneWithTags: SceneWithTags? = null,
    val isLoading: Boolean = true,
    val isMemoEditing: Boolean = false,
    val memoInput: String = "",
    val showRawHtml: Boolean = false,
    val isContentEditing: Boolean = false,
    val contentInput: String = "",
    val isDeleteConfirming: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sceneRepository: SceneRepository
) : ViewModel() {

    private val sceneId: String = checkNotNull(savedStateHandle["sceneId"])

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    init {
        loadScene()
    }

    private fun loadScene() {
        viewModelScope.launch {
            val scene = sceneRepository.getSceneById(sceneId)
            _uiState.update {
                it.copy(
                    sceneWithTags = scene,
                    memoInput = scene?.scene?.memo ?: "",
                    isLoading = false
                )
            }
        }
    }

    fun startMemoEdit() {
        _uiState.update { it.copy(isMemoEditing = true) }
    }

    fun onMemoInput(text: String) {
        _uiState.update { it.copy(memoInput = text) }
    }

    fun saveMemo() {
        val scene = _uiState.value.sceneWithTags?.scene ?: return
        viewModelScope.launch {
            sceneRepository.updateMemo(scene, _uiState.value.memoInput.ifBlank { null })
            loadScene()
            _uiState.update { it.copy(isMemoEditing = false) }
        }
    }

    fun cancelMemoEdit() {
        _uiState.update {
            it.copy(
                isMemoEditing = false,
                memoInput = it.sceneWithTags?.scene?.memo ?: ""
            )
        }
    }

    fun toggleRawHtml() {
        _uiState.update { it.copy(showRawHtml = !it.showRawHtml) }
    }

    // ── 본문 편집 ──

    fun startContentEdit() {
        val html = _uiState.value.sceneWithTags?.scene?.contentHtml ?: ""
        _uiState.update { it.copy(isContentEditing = true, contentInput = html) }
    }

    fun onContentInput(text: String) {
        _uiState.update { it.copy(contentInput = text) }
    }

    fun saveContent() {
        val scene = _uiState.value.sceneWithTags?.scene ?: return
        viewModelScope.launch {
            sceneRepository.updateContent(scene, _uiState.value.contentInput)
            loadScene()
            _uiState.update { it.copy(isContentEditing = false) }
        }
    }

    fun cancelContentEdit() {
        _uiState.update { it.copy(isContentEditing = false) }
    }

    // ── 씬 삭제 ──

    fun showDeleteConfirm() {
        _uiState.update { it.copy(isDeleteConfirming = true) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(isDeleteConfirming = false) }
    }

    fun deleteScene() {
        val scene = _uiState.value.sceneWithTags?.scene ?: return
        viewModelScope.launch {
            sceneRepository.deleteScene(scene)
            _uiState.update { it.copy(isDeleteConfirming = false, isDeleted = true) }
        }
    }
}
