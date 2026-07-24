package com.chronie.homemoneylite.ui.expense

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.chronie.homemoneylite.databinding.ItemAiImageBinding

class AIImageAdapter(
    private val onRemove: (Uri) -> Unit,
    private val onCrop: (Uri) -> Unit
) : ListAdapter<Uri, AIImageAdapter.ImageViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Uri>() {
            override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean =
                oldItem == newItem
        }
    }

    class ImageViewHolder(
        private val binding: ItemAiImageBinding,
        private val onRemove: (Uri) -> Unit,
        private val onCrop: (Uri) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var current: Uri? = null

        init {
            binding.removeButton.setOnClickListener { current?.let(onRemove) }
            binding.imageCard.setOnClickListener { current?.let(onCrop) }
        }

        fun bind(uri: Uri) {
            current = uri
            binding.imageView.load(uri)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemAiImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImageViewHolder(binding, onRemove, onCrop)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
