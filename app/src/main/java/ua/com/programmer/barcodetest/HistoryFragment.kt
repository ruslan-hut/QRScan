package ua.com.programmer.barcodetest

import android.content.Intent
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
import com.google.mlkit.vision.barcode.common.Barcode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ua.com.programmer.barcodetest.data.BarcodeHistoryItem
import ua.com.programmer.barcodetest.viewmodel.HistoryViewModel

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
                    // Handle error if needed
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
        var icon: ImageView =
            view.findViewById(R.id.item_icon)
    }
}
