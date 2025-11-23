package ua.com.programmer.qrscanner

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.mlkit.vision.barcode.common.Barcode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ua.com.programmer.qrscanner.data.BarcodeHistoryItem
import ua.com.programmer.qrscanner.error.ErrorDisplay
import ua.com.programmer.qrscanner.viewmodel.HistoryViewModel
import java.io.File

@AndroidEntryPoint
class HistoryFragment : Fragment() {
    private val viewModel: HistoryViewModel by viewModels()
    
    private lateinit var mFragmentView: View
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mItemAdapter: HistoryItemAdapter
    private val utils = Utils()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        mFragmentView = inflater.inflate(R.layout.fragment_history, container, false)

        mRecyclerView = mFragmentView.findViewById(R.id.history_recycler)
        mRecyclerView.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(requireContext())
        mRecyclerView.setLayoutManager(linearLayoutManager)

        mItemAdapter = HistoryItemAdapter()
        mRecyclerView.adapter = mItemAdapter

        val simpleCallback: ItemTouchHelper.SimpleCallback =
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT, ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    mItemAdapter.onItemDismiss(viewHolder.bindingAdapterPosition)
                }

                override fun isItemViewSwipeEnabled(): Boolean {
                    return true
                }

                override fun isLongPressDragEnabled(): Boolean {
                    return false
                }
            }
        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(mRecyclerView)

        // Observe ViewModel state
        observeViewModelState()

        return mFragmentView
    }

    private fun observeViewModelState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                mItemAdapter.updateItems(state.historyItems)
                val emptyView = mFragmentView.findViewById<LinearLayout>(R.id.title_no_data)
                emptyView.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
                
                state.error?.let { error ->
                    ErrorDisplay.showError(requireContext(), error)
                    // Clear error after displaying
                    viewModel.clearError()
                }
            }
        }
    }

    fun refreshList() {
        viewModel.refresh()
    }

    private inner class HistoryItemAdapter : RecyclerView.Adapter<ItemViewHolder>() {
        private var items: List<BarcodeHistoryItem> = emptyList()

        fun updateItems(newItems: List<BarcodeHistoryItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ItemViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.history_item, viewGroup, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = items.getOrNull(position) ?: return

            holder.date.text = item.date
            holder.type.text = utils.nameOfBarcodeFormat(item.codeType)
            holder.value.text = item.codeValue

            when (item.codeType) {
                Barcode.FORMAT_QR_CODE -> holder.icon.setImageResource(R.drawable.qr_code_48)
                Barcode.FORMAT_EAN_13 -> holder.icon.setImageResource(R.drawable.barcode_48)
                else -> holder.icon.setImageResource(R.drawable.product_48)
            }

            // Load barcode preview image if available
            if (!item.imagePath.isNullOrEmpty() && ImageStorageHelper.imageExists(item.imagePath)) {
                holder.preview.visibility = View.VISIBLE
                holder.preview.load(File(item.imagePath)) {
                    crossfade(true)
                    placeholder(R.drawable.product_48)
                    error(R.drawable.product_48)
                }
                // Set click listener to show full-size preview
                holder.preview.setOnClickListener {
                    val dialog = ImagePreviewDialogFragment.newInstance(item.imagePath)
                    dialog.show(parentFragmentManager, "ImagePreviewDialog")
                }
            } else {
                holder.preview.visibility = View.GONE
                holder.preview.setOnClickListener(null)
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TEXT, item.codeValue)
                intent.setType("text/plain")
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun getItemId(position: Int): Long {
            return items.getOrNull(position)?.id ?: 0L
        }

        fun onItemDismiss(position: Int) {
            val item = items.getOrNull(position) ?: return
            viewModel.deleteItem(item.id)
        }
    }

    internal class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var date: TextView = view.findViewById(R.id.item_date)
        var type: TextView = view.findViewById(R.id.item_type)
        var value: TextView = view.findViewById(R.id.item_value)
        var icon: ImageView = view.findViewById(R.id.item_icon)
        var preview: ImageView = view.findViewById(R.id.item_preview)
    }
}
