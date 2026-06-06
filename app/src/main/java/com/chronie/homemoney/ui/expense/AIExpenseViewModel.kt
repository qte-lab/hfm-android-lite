package com.chronie.homemoney.ui.expense

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoney.R
import com.chronie.homemoney.domain.model.AIExpenseRecord
import com.chronie.homemoney.domain.model.ExpenseType
import com.chronie.homemoney.domain.repository.AIRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * AI 支出记录视图模型
 */
@HiltViewModel
class AIExpenseViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val aiRecordRepository: AIRecordRepository,
    private val syncScheduler: com.chronie.homemoney.data.sync.SyncScheduler
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AIExpenseUiState())
    val uiState: StateFlow<AIExpenseUiState> = _uiState.asStateFlow()
    
    /**
     * 添加图片
     */
    fun addImages(uris: List<Uri>) {
        _uiState.update { state ->
            state.copy(
                selectedImages = state.selectedImages + uris
            )
        }
    }
    
    /**
     * 移除图片
     */
    fun removeImage(uri: Uri) {
        _uiState.update { state ->
            state.copy(
                selectedImages = state.selectedImages - uri
            )
        }
    }
    
    /**
     * 更新文本输入
     */
    fun updateTextInput(text: String) {
        _uiState.update { it.copy(textInput = text) }
    }
    
    /**
     * 开始识别
     */
    fun startRecognition() {
        val state = _uiState.value
        
        if (state.selectedImages.isEmpty() && state.textInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.ai_expense_no_input)) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val records = mutableListOf<AIExpenseRecord>()
                
                // 处理图片
                if (state.selectedImages.isNotEmpty()) {
                    val imageResult = aiRecordRepository.parseImagesToRecords(state.selectedImages)
                    imageResult.onSuccess { records.addAll(it) }
                        .onFailure { throw it }
                }
                
                // 处理文本
                if (state.textInput.isNotBlank()) {
                    val textResult = aiRecordRepository.parseTextToRecords(state.textInput)
                    textResult.onSuccess { records.addAll(it) }
                        .onFailure { throw it }
                }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recognizedRecords = records,
                        errorMessage = if (records.isEmpty()) context.getString(R.string.ai_expense_no_records) else null
                    )
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "未知错误"
                android.util.Log.e("AIExpenseViewModel", "Recognition failed: $errorMessage", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.ai_expense_recognition_failed, errorMessage)
                    )
                }
            }
        }
    }
    
    /**
     * 更新记录
     */
    fun updateRecord(index: Int, updatedRecord: AIExpenseRecord) {
        _uiState.update { state ->
            val newRecords = state.recognizedRecords.toMutableList()
            if (index in newRecords.indices) {
                newRecords[index] = updatedRecord.copy(isEdited = true)
            }
            state.copy(recognizedRecords = newRecords)
        }
    }
    
    /**
     * 删除记录
     */
    fun deleteRecord(index: Int) {
        _uiState.update { state ->
            val newRecords = state.recognizedRecords.toMutableList()
            if (index in newRecords.indices) {
                newRecords.removeAt(index)
            }
            state.copy(recognizedRecords = newRecords)
        }
    }
    
    /**
     * 保存所有记录
     */
    fun saveAllRecords(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            
            try {
                val validRecords = _uiState.value.recognizedRecords.filter { it.isValid }
                
                if (validRecords.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = context.getString(R.string.ai_expense_no_valid_records)
                        )
                    }
                    return@launch
                }
                
                val result = aiRecordRepository.saveRecords(validRecords)
                
                result.onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            recognizedRecords = emptyList(),
                            selectedImages = emptyList(),
                            textInput = ""
                        )
                    }
                    
                    // 触发云同步尝试（允许失败）
                    try {
                        syncScheduler.triggerImmediateSync()
                    } catch (e: Exception) {
                        // 同步失败不影响保存记录的成功
                        android.util.Log.w("AIExpenseViewModel", "Failed to trigger sync after saving AI records", e)
                    }
                    
                    onSuccess()
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = context.getString(R.string.ai_expense_save_failed, error.message ?: "")
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = context.getString(R.string.ai_expense_save_failed, e.message ?: "")
                    )
                }
            }
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        _uiState.value = AIExpenseUiState()
    }
}

/**
 * AI 支出记录 UI 状态
 */
data class AIExpenseUiState(
    val selectedImages: List<Uri> = emptyList(),
    val textInput: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val recognizedRecords: List<AIExpenseRecord> = emptyList(),
    val errorMessage: String? = null
)
