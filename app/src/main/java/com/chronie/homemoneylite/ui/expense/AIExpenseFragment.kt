package com.chronie.homemoneylite.ui.expense

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.FragmentAiExpenseBinding
import com.chronie.homemoneylite.databinding.DialogAddRecordEditBinding
import com.chronie.homemoneylite.databinding.DialogOcrResultBinding
import com.chronie.homemoneylite.domain.model.AIExpenseRecord
import com.chronie.homemoneylite.domain.model.ExpenseType
import com.chronie.homemoneylite.ui.common.collectWithLifecycle
import com.chronie.homemoneylite.ui.components.showWheelDatePicker
import android.app.AlertDialog
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AIExpenseFragment : Fragment() {

    private var _binding: FragmentAiExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AIExpenseViewModel by viewModels()

    private lateinit var imageAdapter: AIImageAdapter
    private lateinit var recordAdapter: AIRecordAdapter

    private var cameraImageUri: Uri? = null
    private var lastCropOutputFile: File? = null
    private var ocrDialog: AlertDialog? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // region Activity Result 注册（在字段初始化阶段注册，符合 Fragment 要求）
    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val outputUri = UCrop.getOutput(result.data ?: return@registerForActivityResult)
            // 注意：此处不要删除 lastCropOutputFile —— outputUri 指向的正是该文件，
            // 一旦删除，预览（Coil 加载该 URI）会显示空白。裁剪产物为应用私有小图，
            // 保留即可（卸载时随应用数据一起清除），无需在此删除。
            outputUri?.let { viewModel.addImages(listOf(it)) }
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            // 相册返回的 content:// URI 可能受限，先拷贝到应用私有目录，
            // 既保证 UCrop 能读取源图，也保证裁剪后的预览（同为本应用 FileProvider URI）能被 Coil 稳定加载。
            val local = copyUriToAppFile(requireContext(), uri) ?: uri
            startCrop(local)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { startCrop(it) }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else Unit
    }
    // endregion

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = getString(R.string.ai_expense_title)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        imageAdapter = AIImageAdapter(
            onRemove = viewModel::removeImage,
            onCrop = ::handleCropExistingImage
        )
        binding.imagesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.imagesRecyclerView.adapter = imageAdapter

        recordAdapter = AIRecordAdapter(
            onEdit = ::showEditDialog,
            onDelete = { record ->
                val index = viewModel.uiState.value.recognizedRecords
                    .indexOfFirst { it.id == record.id }
                if (index >= 0) viewModel.deleteRecord(index)
            }
        )
        binding.recordsRecyclerView.adapter = recordAdapter

        binding.addImagesButton.setOnClickListener { showImageSourceDialog() }
        binding.imagePlaceholder.setOnClickListener { showImageSourceDialog() }
        binding.recognizeButton.setOnClickListener { viewModel.startRecognition() }
        binding.saveAllButton.setOnClickListener {
            viewModel.saveAllRecords { findNavController().popBackStack() }
        }

        binding.textInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateTextInput(s?.toString().orEmpty())
            }
        })

        collectWithLifecycle(viewModel.uiState) { state ->
            imageAdapter.submitList(state.selectedImages)

            val hasImages = state.selectedImages.isNotEmpty()
            binding.imagesRecyclerView.visibility = if (hasImages) View.VISIBLE else View.GONE
            binding.imagePlaceholder.visibility = if (hasImages) View.GONE else View.VISIBLE

            if (binding.textInput.text.toString() != state.textInput) {
                binding.textInput.setText(state.textInput)
                binding.textInput.setSelection(state.textInput.length)
            }

            val recognizing = state.isLoading
            binding.recognizeButton.isEnabled =
                !recognizing && (state.selectedImages.isNotEmpty() || state.textInput.isNotBlank())
            binding.recognizeButton.text = if (recognizing) {
                getString(R.string.ai_expense_recognizing)
            } else {
                getString(R.string.ai_expense_start_recognition)
            }

            recordAdapter.submitList(state.recognizedRecords)

            val hasRecords = state.recognizedRecords.isNotEmpty()
            binding.recordsSection.visibility = if (hasRecords) View.VISIBLE else View.GONE
            if (hasRecords) {
                binding.recordsCountText.text = getString(
                    R.string.ai_expense_records_count,
                    state.recognizedRecords.size
                )
                val anyValid = state.recognizedRecords.any { it.isValid }
                binding.saveAllButton.isEnabled = !state.isSaving && anyValid
                binding.saveAllButton.text = if (state.isSaving) {
                    getString(R.string.ai_expense_saving)
                } else {
                    getString(R.string.ai_expense_save_all)
                }
            }

            if (state.errorMessage != null) {
                binding.errorText.visibility = View.VISIBLE
                binding.errorText.text = state.errorMessage
            } else {
                binding.errorText.visibility = View.GONE
            }

            // OCR 结果确认弹窗：识别出的文字先给用户查看/修改，确认后才发给 AI
            if (state.showOcrDialog) {
                if (ocrDialog == null) {
                    showOcrResultDialog(state.ocrText, state.ocrError)
                }
            } else {
                ocrDialog?.dismiss()
                ocrDialog = null
            }
        }
    }

    /**
     * 展示 OCR 文字确认弹窗。
     * OCR 失败/为空时 hint 显示失败原因，用户仍可手动输入内容兜底。
     */
    private fun showOcrResultDialog(ocrText: String, ocrError: String?) {
        val dialogBinding = DialogOcrResultBinding.inflate(layoutInflater)
        dialogBinding.etOcrText.setText(ocrText)
        dialogBinding.etOcrText.setSelection(ocrText.length)
        dialogBinding.tvOcrHint.text = ocrError ?: getString(R.string.ai_expense_ocr_hint)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.ai_expense_ocr_dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.ai_expense_ocr_send, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnDismissListener {
            ocrDialog = null
            viewModel.dismissOcrDialog()
        }
        dialog.show()

        // 覆盖 positive 按钮：校验非空后才发送并关闭
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val text = dialogBinding.etOcrText.text?.toString().orEmpty().trim()
            if (text.isBlank()) {
                dialogBinding.etOcrText.error = getString(R.string.ai_expense_ocr_text_required)
                return@setOnClickListener
            }
            // 先摘除 dismiss 监听，避免 dismiss 时重复触发 dismissOcrDialog 覆盖状态
            dialog.setOnDismissListener { ocrDialog = null }
            viewModel.confirmOcrText(text)
            dialog.dismiss()
        }
        ocrDialog = dialog
    }

    // region 图片来源 / 拍摄 / 裁剪
    private fun showImageSourceDialog() {
        val items = arrayOf(
            getString(R.string.ai_expense_take_photo),
            getString(R.string.ai_expense_choose_from_gallery)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.ai_expense_add_images)
            .setItems(items) { _, which ->
                if (which == 0) handleCameraClick() else handleGalleryClick()
            }
            .show()
    }

    private fun handleCameraClick() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        cameraImageUri = createImageFile(requireContext())
        cameraImageUri?.let { cameraLauncher.launch(it) }
    }

    private fun handleGalleryClick() {
        imagePickerLauncher.launch("image/*")
    }

    private fun handleCropExistingImage(uri: Uri) {
        viewModel.removeImage(uri)
        startCrop(uri)
    }

    private fun startCrop(uri: Uri) {
        try {
            val timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(LocalDateTime.now())
            val storageDir = requireContext()
                .getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return
            storageDir.mkdirs()
            val image = File(storageDir, "CROP_${timeStamp}_.jpg")
            if (image.exists()) image.delete()
            image.createNewFile()
            lastCropOutputFile = image
            val outputUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                image
            )

            val options = UCrop.Options()
            // OCR 需要清晰文字：压缩质量提高，避免细节丢失
            options.setCompressionQuality(95)
            options.setHideBottomControls(false)
            options.setFreeStyleCropEnabled(true)
            options.setToolbarColor(Color.parseColor("#6750A4"))
            options.setActiveControlsWidgetColor(Color.WHITE)
            options.setToolbarTitle("")
            options.setToolbarWidgetColor(Color.WHITE)
            options.setDimmedLayerColor(Color.parseColor("#80000000"))
            options.setShowCropGrid(false)
            options.setShowCropFrame(true)

            // 注意：不要强制 1:1 裁剪比例，账单/小票多为竖长图，
            // 强制正方形+1080 压缩会把文字缩到 ML Kit OCR 无法识别的尺寸
            val uCrop = UCrop.of(uri, outputUri)
                .withMaxResultSize(2560, 4096)
                .withOptions(options)
            cropLauncher.launch(uCrop.getIntent(requireContext()))
        } catch (e: Exception) {
            android.util.Log.e("AIExpenseFragment", "Failed to start crop", e)
        }
    }

    private fun createImageFile(context: Context): Uri? {
        return try {
            val timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(LocalDateTime.now())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return null
            storageDir.mkdirs()
            val image = File(storageDir, "JPEG_${timeStamp}_.jpg")
            if (image.exists()) image.delete()
            if (!image.createNewFile()) {
                android.util.Log.e("AIExpenseFragment", "Failed to create file")
                return null
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                image
            )
        } catch (ex: IOException) {
            android.util.Log.e("AIExpenseFragment", "IOException in createImageFile", ex)
            null
        } catch (ex: Exception) {
            android.util.Log.e("AIExpenseFragment", "Exception in createImageFile", ex)
            null
        }
    }

    /**
     * 将相册等来源的 content:// URI 拷贝到应用私有 Pictures 目录，
     * 返回对应的 FileProvider URI。这样裁剪源与最终预览都使用本应用可稳定读写的 URI，
     * 避免某些 content:// 因权限/跨进程限制导致 Coil 加载空白。
     */
    private fun copyUriToAppFile(context: Context, uri: Uri): Uri? {
        return try {
            val timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(LocalDateTime.now())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return null
            storageDir.mkdirs()
            val file = File(storageDir, "AI_SRC_${timeStamp}_${System.currentTimeMillis()}.jpg")
            if (file.exists()) file.delete()
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            android.util.Log.e("AIExpenseFragment", "copyUriToAppFile failed", e)
            null
        }
    }
    // endregion

    // region 记录编辑对话框
    private fun showEditDialog(record: AIExpenseRecord) {
        val dialogBinding = DialogAddRecordEditBinding.inflate(layoutInflater)

        var selectedType = record.type
        var selectedDate = runCatching { LocalDate.parse(record.date) }
            .getOrDefault(LocalDate.now())

        val typeAdapter = ExpenseTypeAdapter(requireContext(), ExpenseType.values().toList())
        dialogBinding.editTypeInput.setAdapter(typeAdapter)
        dialogBinding.editTypeInput.setText(
            ExpenseTypeLocalizer.getLocalizedName(requireContext(), selectedType),
            false
        )
        dialogBinding.editTypeInput.setOnItemClickListener { _, _, position, _ ->
            selectedType = typeAdapter.getItem(position)
            val name = ExpenseTypeLocalizer.getLocalizedName(requireContext(), selectedType)
            dialogBinding.editTypeInput.post {
                dialogBinding.editTypeInput.setText(name, false)
            }
        }

        dialogBinding.editAmountInput.setText(record.amount.toString())
        dialogBinding.editRemarkInput.setText(record.remark)
        updateEditDateButton(dialogBinding, selectedDate)

        dialogBinding.editDateButton.setOnClickListener {
            showWheelDatePicker(
                requireContext(),
                initial = selectedDate,
                minDate = LocalDate.of(2000, 1, 1),
                maxDate = LocalDate.now()
            ) { date ->
                selectedDate = date
                updateEditDateButton(dialogBinding, selectedDate)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.ai_expense_edit_record)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.confirm, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()
        // 覆盖 positive 按钮以便校验后再关闭
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val amount = dialogBinding.editAmountInput.text?.toString()
                ?.toDoubleOrNull() ?: record.amount
            val updated = record.copy(
                type = selectedType,
                amount = amount,
                date = selectedDate.format(dateFormatter),
                remark = dialogBinding.editRemarkInput.text?.toString().orEmpty(),
                isEdited = true
            )
            val index = viewModel.uiState.value.recognizedRecords
                .indexOfFirst { it.id == record.id }
            if (index >= 0) viewModel.updateRecord(index, updated)
            dialog.dismiss()
        }
    }

    private fun updateEditDateButton(binding: DialogAddRecordEditBinding, date: LocalDate) {
        binding.editDateButton.text = date.format(dateFormatter)
    }
    // endregion

    override fun onDestroyView() {
        super.onDestroyView()
        ocrDialog?.setOnDismissListener(null)
        ocrDialog?.dismiss()
        ocrDialog = null
        _binding = null
    }
}
