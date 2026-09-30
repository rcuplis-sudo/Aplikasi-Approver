package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SignedDocumentEntity
import com.example.data.repository.SignedDocumentRepository
import com.example.model.BatchFileItem
import com.example.model.BatchItemStatus
import com.example.model.BatchProgressState
import com.example.model.CustomQrPlacement
import com.example.model.QrCenterOverlayType
import com.example.model.QrOverlayConfig
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
import java.io.FileOutputStream
import java.util.UUID

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

    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    // Default to empty string on first launch; otherwise load previously saved signer name!
    private val _signerName = MutableStateFlow(
        prefs.getString(KEY_SIGNER_NAME, "") ?: ""
    )
    val signerName: StateFlow<String> = _signerName.asStateFlow()

    private val _targetPlaceholder = MutableStateFlow("\${ttd_pengirim1}")
    val targetPlaceholder: StateFlow<String> = _targetPlaceholder.asStateFlow()

    // --- App Theme & Appearance Preferences ---
    private val _themeMode = MutableStateFlow(
        try {
            val savedMode = prefs.getString(KEY_THEME_MODE, null)
            if (savedMode != null) com.example.model.AppThemeMode.valueOf(savedMode) else com.example.model.AppThemeMode.LIGHT
        } catch (_: Exception) {
            com.example.model.AppThemeMode.LIGHT
        }
    )
    val themeMode: StateFlow<com.example.model.AppThemeMode> = _themeMode.asStateFlow()

    private val _paletteStyle = MutableStateFlow(
        try {
            val savedStyle = prefs.getString(KEY_PALETTE_STYLE, null)
            if (savedStyle != null) com.example.model.ColorPaletteStyle.valueOf(savedStyle) else com.example.model.ColorPaletteStyle.OCEAN_BLUE
        } catch (_: Exception) {
            com.example.model.ColorPaletteStyle.OCEAN_BLUE
        }
    )
    val paletteStyle: StateFlow<com.example.model.ColorPaletteStyle> = _paletteStyle.asStateFlow()

    fun updateThemeMode(mode: com.example.model.AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun updatePaletteStyle(style: com.example.model.ColorPaletteStyle) {
        _paletteStyle.value = style
        prefs.edit().putString(KEY_PALETTE_STYLE, style.name).apply()
    }

    // --- QR Center Logo / Initials Embedding Config (Persisted in SharedPreferences & Storage) ---
    private val _qrOverlayConfig: MutableStateFlow<QrOverlayConfig> = run {
        val savedTypeName = prefs.getString(KEY_OVERLAY_TYPE, null)
        val initialType = try {
            if (savedTypeName != null) QrCenterOverlayType.valueOf(savedTypeName) else QrCenterOverlayType.NONE
        } catch (_: Exception) {
            QrCenterOverlayType.NONE
        }

        val savedInitials = prefs.getString(KEY_OVERLAY_INITIALS, null)
            ?: computeInitials(prefs.getString(KEY_SIGNER_NAME, "") ?: "")

        val savedLogoPath = prefs.getString(KEY_OVERLAY_LOGO_FILE, null)
        var loadedBitmap: Bitmap? = null
        var loadedUri: Uri? = null

        if (savedLogoPath != null) {
            try {
                val file = File(savedLogoPath)
                if (file.exists()) {
                    loadedBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    loadedUri = Uri.fromFile(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val effectiveType = if (initialType == QrCenterOverlayType.CUSTOM_LOGO && loadedBitmap == null) {
            QrCenterOverlayType.INITIALS
        } else {
            initialType
        }

        MutableStateFlow(
            QrOverlayConfig(
                type = effectiveType,
                initials = savedInitials,
                logoUri = loadedUri,
                logoBitmap = loadedBitmap
            )
        )
    }
    val qrOverlayConfig: StateFlow<QrOverlayConfig> = _qrOverlayConfig.asStateFlow()

    fun updateOverlayType(type: QrCenterOverlayType) {
        _qrOverlayConfig.value = _qrOverlayConfig.value.copy(type = type)
        prefs.edit().putString(KEY_OVERLAY_TYPE, type.name).apply()
    }

    fun updateOverlayInitials(initials: String) {
        _qrOverlayConfig.value = _qrOverlayConfig.value.copy(initials = initials)
        prefs.edit().putString(KEY_OVERLAY_INITIALS, initials).apply()
    }

    fun updateOverlayLogo(uri: Uri?, bitmap: Bitmap?) {
        val targetType = if (bitmap != null) QrCenterOverlayType.CUSTOM_LOGO else QrCenterOverlayType.INITIALS
        val logoFile = File(getApplication<Application>().filesDir, "qr_custom_logo.png")
        if (bitmap != null) {
            try {
                FileOutputStream(logoFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                prefs.edit()
                    .putString(KEY_OVERLAY_TYPE, targetType.name)
                    .putString(KEY_OVERLAY_LOGO_FILE, logoFile.absolutePath)
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                if (logoFile.exists()) logoFile.delete()
            } catch (_: Exception) {}
            prefs.edit()
                .putString(KEY_OVERLAY_TYPE, targetType.name)
                .remove(KEY_OVERLAY_LOGO_FILE)
                .apply()
        }

        _qrOverlayConfig.value = _qrOverlayConfig.value.copy(
            type = targetType,
            logoUri = if (bitmap != null) Uri.fromFile(logoFile) else null,
            logoBitmap = bitmap
        )
    }

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

    // Touch-and-drag custom placement state: Map of pageIndex to CustomQrPlacement
    private val _customPlacements = MutableStateFlow<Map<Int, CustomQrPlacement>>(emptyMap())
    val customPlacements: StateFlow<Map<Int, CustomQrPlacement>> = _customPlacements.asStateFlow()

    private val _isManualPlacementMode = MutableStateFlow(false)
    val isManualPlacementMode: StateFlow<Boolean> = _isManualPlacementMode.asStateFlow()

    fun toggleManualPlacementMode(enable: Boolean? = null) {
        val next = enable ?: !_isManualPlacementMode.value
        _isManualPlacementMode.value = next
        // If enabling and no placement exists for current page, set a sensible default center-bottom
        if (next && !_customPlacements.value.containsKey(_previewCurrentPage.value)) {
            setQrPlacement(_previewCurrentPage.value, 0.65f, 0.75f)
        }
    }

    fun setQrPlacement(pageIndex: Int, normX: Float, normY: Float, sizeDp: Float? = null) {
        val current = _customPlacements.value.toMutableMap()
        val existing = current[pageIndex]
        val finalSize = sizeDp ?: existing?.qrSizeDp ?: 75f
        current[pageIndex] = CustomQrPlacement(
            pageIndex = pageIndex,
            normalizedX = normX.coerceIn(0.01f, 0.95f),
            normalizedY = normY.coerceIn(0.01f, 0.95f),
            qrSizeDp = finalSize.coerceIn(45f, 130f)
        )
        _customPlacements.value = current
    }

    fun updateQrPlacementSize(pageIndex: Int, sizeDp: Float) {
        val current = _customPlacements.value.toMutableMap()
        val existing = current[pageIndex]
        if (existing != null) {
            current[pageIndex] = existing.copy(qrSizeDp = sizeDp.coerceIn(45f, 130f))
            _customPlacements.value = current
        } else {
            setQrPlacement(pageIndex, 0.65f, 0.75f, sizeDp)
        }
    }

    fun clearCustomPlacement(pageIndex: Int? = null) {
        if (pageIndex != null) {
            val current = _customPlacements.value.toMutableMap()
            current.remove(pageIndex)
            _customPlacements.value = current
        } else {
            _customPlacements.value = emptyMap()
            _isManualPlacementMode.value = false
        }
    }

    fun updateDocumentTitle(title: String) {
        _documentTitle.value = title
    }

    fun updateSignerName(signer: String) {
        _signerName.value = signer
        prefs.edit().putString(KEY_SIGNER_NAME, signer).apply()
        val newInitials = computeInitials(signer)
        val hasCustomInitials = prefs.contains(KEY_OVERLAY_INITIALS)
        if (!hasCustomInitials && _qrOverlayConfig.value.type == QrCenterOverlayType.INITIALS && newInitials.isNotBlank()) {
            _qrOverlayConfig.value = _qrOverlayConfig.value.copy(initials = newInitials)
        }
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
                    PdfSampleGenerator.SampleType.MULTI_PLACEHOLDER -> "Dokumen Kontrak Multi-Pihak (2 Placeholder TTD)"
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

        if (_signerName.value.trim().isEmpty()) {
            _uiState.value = SignUiState.Error("Silakan isi nama penandatangan terlebih dahulu.")
            return
        }

        // Persist signer name
        prefs.edit().putString(KEY_SIGNER_NAME, _signerName.value.trim()).apply()

        viewModelScope.launch {
            _uiState.value = SignUiState.Loading("Menganalisis placeholder & menandatangani...")
            try {
                // If user customized QR placement via touch-and-drag, pass it to signerService
                val placement = if (_isManualPlacementMode.value) {
                    _customPlacements.value[_previewCurrentPage.value]
                } else {
                    null
                }

                val result = signerService.signPdf(
                    documentTitle = _documentTitle.value,
                    signerName = _signerName.value,
                    inputSource = currentDoc.source,
                    originalFileName = currentDoc.title,
                    customPlaceholder = _targetPlaceholder.value,
                    qrOverlayConfig = _qrOverlayConfig.value,
                    customPlacement = placement
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

    // --- Backup & Restore Operations ---
    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage.asStateFlow()

    private val _isBackupProcessing = MutableStateFlow(false)
    val isBackupProcessing: StateFlow<Boolean> = _isBackupProcessing.asStateFlow()

    private val _lastBackupResult = MutableStateFlow<com.example.data.backup.BackupResult?>(null)
    val lastBackupResult: StateFlow<com.example.data.backup.BackupResult?> = _lastBackupResult.asStateFlow()

    fun performBackup() {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            _backupStatusMessage.value = null
            try {
                val result = com.example.data.backup.BackupRestoreManager.createBackup(
                    context = getApplication(),
                    repository = repository
                )
                _lastBackupResult.value = result
                _backupStatusMessage.value = "Backup berhasil dibuat (${result.fileSizeFormatted}, ${result.totalDocuments} dokumen, ${result.totalPdfFiles} file PDF)."
            } catch (e: Exception) {
                _backupStatusMessage.value = "Gagal membuat backup: ${e.localizedMessage ?: "Terjadi kesalahan"}"
            } finally {
                _isBackupProcessing.value = false
            }
        }
    }

    fun performRestore(zipUri: Uri, replaceExisting: Boolean) {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            _backupStatusMessage.value = null
            try {
                val result = com.example.data.backup.BackupRestoreManager.restoreBackup(
                    context = getApplication(),
                    zipUri = zipUri,
                    repository = repository,
                    replaceExisting = replaceExisting
                )
                _backupStatusMessage.value = result.message
            } catch (e: Exception) {
                _backupStatusMessage.value = "Gagal memulihkan backup: ${e.localizedMessage ?: "File ZIP tidak valid"}"
            } finally {
                _isBackupProcessing.value = false
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
            _backupStatusMessage.value = "Semua riwayat data Room berhasil dikosongkan."
        }
    }

    fun clearBackupStatus() {
        _backupStatusMessage.value = null
    }

    // --- Batch Auto-Signing Progress & State ---
    private val _batchState = MutableStateFlow(BatchProgressState())
    val batchState: StateFlow<BatchProgressState> = _batchState.asStateFlow()

    fun addBatchFiles(uris: List<Pair<Uri, String>>) {
        val currentItems = _batchState.value.items.toMutableList()
        val newItems = uris.map { (uri, name) ->
            BatchFileItem(
                id = UUID.randomUUID().toString(),
                fileName = name,
                source = uri,
                status = BatchItemStatus.PENDING,
                details = "Siap untuk diproses"
            )
        }
        currentItems.addAll(newItems)
        _batchState.value = BatchProgressState(
            items = currentItems,
            totalCount = currentItems.size,
            currentIndex = 0,
            isCompleted = false
        )
    }

    fun loadSampleBatchSuite() {
        viewModelScope.launch {
            _batchState.value = _batchState.value.copy(isRunning = true)
            try {
                val f1 = PdfSampleGenerator.generateSample(getApplication(), PdfSampleGenerator.SampleType.MULTI_PLACEHOLDER)
                val f2 = PdfSampleGenerator.generateSample(getApplication(), PdfSampleGenerator.SampleType.TEXT_PLACEHOLDER)

                val sampleItems = listOf(
                    BatchFileItem(UUID.randomUUID().toString(), "Kontrak_Multi_TTD_2x.pdf", f1, details = "1 Teks ${'$'}{ttd_pengirim1} muncul 2 kali (Multi-TTD)"),
                    BatchFileItem(UUID.randomUUID().toString(), "Surat_Keputusan_1_Teks.pdf", f2, details = "1 Placeholder teks (${'$'}{ttd_pengirim1})")
                )

                _batchState.value = BatchProgressState(
                    isRunning = false,
                    items = sampleItems,
                    totalCount = sampleItems.size,
                    currentIndex = 0,
                    isCompleted = false
                )
            } catch (e: Exception) {
                _batchState.value = _batchState.value.copy(isRunning = false)
            }
        }
    }

    fun startBatchSigning() {
        val currentState = _batchState.value
        val itemsToProcess = currentState.items
        if (itemsToProcess.isEmpty() || currentState.isRunning) return

        viewModelScope.launch {
            val total = itemsToProcess.size
            var success = 0
            var failure = 0
            var totalSigs = 0

            _batchState.value = currentState.copy(
                isRunning = true,
                currentIndex = 0,
                totalCount = total,
                successCount = 0,
                failureCount = 0,
                totalSignaturesPlaced = 0,
                isCompleted = false
            )

            val updatedItems = itemsToProcess.toMutableList()

            for (index in itemsToProcess.indices) {
                val item = updatedItems[index]
                _batchState.value = _batchState.value.copy(
                    currentIndex = index,
                    currentFileName = item.fileName
                )

                // Set current item to processing
                updatedItems[index] = item.copy(
                    status = BatchItemStatus.PROCESSING,
                    details = "Mencari placeholder & menempelkan tanda tangan..."
                )
                _batchState.value = _batchState.value.copy(items = updatedItems.toList())

                val startTime = System.currentTimeMillis()
                try {
                    val result = signerService.signPdf(
                        documentTitle = item.fileName.removeSuffix(".pdf"),
                        signerName = _signerName.value.ifBlank { "Penandatangan Resmi" },
                        inputSource = item.source,
                        originalFileName = item.fileName,
                        customPlaceholder = _targetPlaceholder.value,
                        qrOverlayConfig = _qrOverlayConfig.value
                    )
                    val duration = System.currentTimeMillis() - startTime
                    val sigCount = result.signaturesCount
                    totalSigs += sigCount
                    success++

                    val statusDetail = if (sigCount > 1) {
                        "Berhasil! $sigCount placeholder terdeteksi & ditandatangani otomatis (${duration}ms)"
                    } else {
                        "Berhasil! 1 placeholder ditandatangani (${result.match.source.name}, ${duration}ms)"
                    }

                    updatedItems[index] = item.copy(
                        status = BatchItemStatus.SUCCESS,
                        signaturesCount = sigCount,
                        details = statusDetail,
                        durationMs = duration,
                        signedFilePath = result.signedFile.absolutePath
                    )
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    failure++
                    updatedItems[index] = item.copy(
                        status = BatchItemStatus.FAILED,
                        details = "Gagal: ${e.localizedMessage ?: "Kesalahan pemrosesan berkas"}",
                        durationMs = duration,
                        errorMessage = e.localizedMessage
                    )
                }

                _batchState.value = _batchState.value.copy(
                    currentIndex = index + 1,
                    successCount = success,
                    failureCount = failure,
                    totalSignaturesPlaced = totalSigs,
                    items = updatedItems.toList()
                )
            }

            _batchState.value = _batchState.value.copy(
                isRunning = false,
                isCompleted = true,
                currentFileName = null
            )
        }
    }

    fun clearBatchQueue() {
        if (!_batchState.value.isRunning) {
            _batchState.value = BatchProgressState()
        }
    }

    companion object {
        private const val PREFS_NAME = "pdf_signer_preferences"
        private const val KEY_SIGNER_NAME = "saved_signer_name"
        private const val KEY_THEME_MODE = "saved_theme_mode"
        private const val KEY_PALETTE_STYLE = "saved_palette_style"
        private const val KEY_OVERLAY_TYPE = "saved_overlay_type"
        private const val KEY_OVERLAY_INITIALS = "saved_overlay_initials"
        private const val KEY_OVERLAY_LOGO_FILE = "saved_overlay_logo_file"

        fun computeInitials(name: String): String {
            val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> ""
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            }
        }
    }
}
