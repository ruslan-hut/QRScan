package ua.com.programmer.barcodetest

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.barcode.common.Barcode

class HistoryFragment : Fragment() {
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

        refreshList()
        return mFragmentView
    }

    fun refreshList() {
        mRecyclerView.swapAdapter(mItemAdapter, true)
        val emptyView = mFragmentView.findViewById<LinearLayout>(R.id.title_no_data)
        if (mItemAdapter.itemCount == 0) {
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Properly close database cursor to prevent memory leaks
        if (::mItemAdapter.isInitialized) {
            mItemAdapter.closeCursor()
        }
    }

    private inner class HistoryItemAdapter : RecyclerView.Adapter<ItemViewHolder>() {
        var cursor: Cursor? = null

        init {
            resetCursor()
        }

        fun resetCursor() {
            // Close old cursor before creating a new one to prevent memory leaks
            cursor?.close()
            val dbHelper = DBHelper(requireContext())
            val db = dbHelper.writableDatabase
            cursor = db.query("history", null, null, null, null, null, "time DESC")
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ItemViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.history_item, viewGroup, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val currentCursor = cursor ?: return
            if (!currentCursor.moveToPosition(position)) {
                return
            }

            val helper = CursorHelper(currentCursor)

            //long itemID = cursor.getLong(cursor.getColumnIndex("_id"));
            val codeType = helper.getInt("codeType")
            val codeValue = helper.getString("codeValue")

            holder.date.text = helper.getString("date")
            holder.type.text = utils.nameOfBarcodeFormat(codeType)
            holder.value.text = codeValue

            when (codeType) {
                Barcode.FORMAT_QR_CODE -> holder.icon.setImageResource(R.drawable.qr_code_48)
                Barcode.FORMAT_EAN_13 -> holder.icon.setImageResource(R.drawable.barcode_48)
                else -> holder.icon.setImageResource(R.drawable.product_48)
            }

            holder.itemView.setOnClickListener { view: View? ->
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TEXT, codeValue)
                intent.setType("text/plain")
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return cursor?.count ?: 0
        }

        override fun getItemId(position: Int): Long {
            val currentCursor = cursor ?: return 0L
            if (!currentCursor.moveToPosition(position)) {
                return 0L
            }
            val helper = CursorHelper(currentCursor)
            return helper.getLong("raw_id")
        }

        fun onItemDismiss(position: Int) {
            val itemID = getItemId(position)
            val dbHelper = DBHelper(requireContext())
            val db = dbHelper.writableDatabase
            db.delete("history", "_id=$itemID", null)
            resetCursor()
            mItemAdapter.notifyItemRemoved(position)
        }
        
        fun closeCursor() {
            cursor?.close()
            cursor = null
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
