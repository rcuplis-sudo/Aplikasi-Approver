package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SignedDocumentEntity
import com.example.data.repository.SignedDocumentRepository
import com.example.pdf.PdfPreviewRenderer
import com.example.pdf.PdfSampleGenerator
import com.example.pdf.PdfSignerService
import com.example.pdf.SigningResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface SignUiState {
    data object Idle : SignUiState
    data class Loading(val message: String) : SignUiState
    data class Success(val result: SigningResult) : SignUiState
    data class Error(val errorMessage: String) : SignUiState
}

data class SelectedDocumentSource(
    val title: String,
    val source: Any, // Uri or File
    val isSample: Boolean,
    val sampleType: PdfSampleGenerator.SampleType? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SignedDocumentRepository
    private val signerService: PdfSignerService

    init {
        val db = AppDatabase.getDatabase(application)
        repository = SignedDocumentRepository(db.signedDocumentDao())
        signerService = PdfSignerService(application, repository)
    }

    val historyDocuments: StateFlow<List<SignedDocumentEntity>> = repository.allDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<SignUiState>(SignUiState.Idle)
    val uiState: StateFlow<SignUiState> = _uiState.asStateFlow()

    private val _selectedDocument = MutableStateFlow<SelectedDocumentSource?>(null)
    val selectedDocument: StateFlow<SelectedDocumentSource?> = _selectedDocument.asStateFlow()

    private val _documentTitle = MutableStateFlow("Surat Keputusan Kerjasama")
    val documentTitle: StateFlow<String> = _documentTitle.asStateFlow()

    private val _signerName = MutableStateFlow("Dr. Hendra Wijaya, M.T.")
    val signerName: StateFlow<String> = _signerName.asStateFlow()

    private val _targetPlaceholder = MutableStateFlow("\${ttd_pengirim1}")
    val targetPlaceholder: StateFlow<String> = _targetPlaceholder.asStateFlow()

    private val _detailItem = MutableStateFlow<SignedDocumentEntity?>(null)
    val detailItem: StateFlow<SignedDocumentEntity?> = _detailItem.asStateFlow()

    // --- PDF Preview States using PDFBox ---
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _isPreviewLoading = MutableStateFlow(false)
    val isPreviewLoading: StateFlow<Boolean> = _isPreviewLoading.asStateFlow()

    private val _previewCurrentPage = MutableStateFlow(0)
    val previewCurrentPage: StateFlow<Int> = _previewCurrentPage.asStateFlow()

    private val _previewTotalPages = MutableStateFlow(0)
    val previewTotalPages: StateFlow<Int> = _previewTotalPages.asStateFlow()

    fun updateDocumentTitle(title: String) {
        _documentTitle.value = title
    }

    fun updateSignerName(signer: String) {
        _signerName.value = signer
    }

    fun updateTargetPlaceholder(placeholder: String) {
        _targetPlaceholder.value = placeholder
    }

    fun setCustomUri(uri: Uri, fileName: String) {
        _selectedDocument.value = SelectedDocumentSource(
            title = fileName,
            source = uri,
            isSample = false
        )
        if (_documentTitle.value.isBlank()) {
            _documentTitle.value = fileName.removeSuffix(".pdf")
        }
        _uiState.value = SignUiState.Idle
        loadPreviewForDocument(uri, 0)
    }

    fun loadSample(type: PdfSampleGenerator.SampleType) {
        viewModelScope.launch {
            _uiState.value = SignUiState.Loading("Membuat dokumen sampel...")
            try {
                val file = PdfSampleGenerator.generateSample(getApplication(), type)
                val title = when (type) {
                    PdfSampleGenerator.SampleType.TEXT_PLACEHOLDER -> "Dokumen Sampel (Placeholder Teks \${ttd_pengirim1})"
                    PdfSampleGenerator.SampleType.ACROFORM_FIELD -> "Dokumen Sampel (AcroForm Field 'ttd_pengirim1')"
                    PdfSampleGenerator.SampleType.NO_PLACEHOLDER_FALLBACK -> "Dokumen Sampel (Tanpa Placeholder - Test Fallback)"
                }
                _selectedDocument.value = SelectedDocumentSource(
                    title = file.name,
                    source = file,
                    isSample = true,
                    sampleType = type
                )
                _documentTitle.value = title
                _uiState.value = SignUiState.Idle
                loadPreviewForDocument(file, 0)
            } catch (e: Exception) {
                _uiState.value = SignUiState.Error("Gagal membuat sampel: ${e.localizedMessage}")
            }
        }
    }

    private fun loadPreviewForDocument(source: Any, pageIndex: Int) {
        viewModelScope.launch {
            _isPreviewLoading.value = true
            try {
                val total = PdfPreviewRenderer.getPageCount(getApplication(), source)
                _previewTotalPages.value = total
                val targetPage = pageIndex.coerceIn(0, (total - 1).coerceAtLeast(0))
                _previewCurrentPage.value = targetPage

                val bitmap = PdfPreviewRenderer.renderPageToBitmap(
                    context = getApplication(),
                    inputSource = source,
                    pageIndex = targetPage,
                    scale = 1.3f
                )
                _previewBitmap.value = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                _previewBitmap.value = null
            } finally {
                _isPreviewLoading.value = false
            }
        }
    }

    fun changePreviewPage(pageIndex: Int) {
        val currentDoc = _selectedDocument.value ?: return
        if (pageIndex in 0 until _previewTotalPages.value) {
            loadPreviewForDocument(currentDoc.source, pageIndex)
        }
    }

    fun signDocument() {
        val currentDoc = _selectedDocument.value
        if (currentDoc == null) {
            _uiState.value = SignUiState.Error("Pilih file PDF atau gunakan dokumen sampel terlebih dahulu!")
            return
        }

        viewModelScope.launch {
            _uiState.value = SignUiState.Loading("Menganalisis placeholder & menandatangani...")
            try {
                val result = signerService.signPdf(
                    documentTitle = _documentTitle.value,
                    signerName = _signerName.value,
                    inputSource = currentDoc.source,
                    originalFileName = currentDoc.title,
                    customPlaceholder = _targetPlaceholder.value
                )
                _uiState.value = SignUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = SignUiState.Error("Gagal menandatangani: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun showDetail(item: SignedDocumentEntity) {
        _detailItem.value = item
    }

    fun dismissDetail() {
        _detailItem.value = null
    }

    fun deleteHistory(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
        }
    }

    fun resetState() {
        _uiState.value = SignUiState.Idle
    }
}
