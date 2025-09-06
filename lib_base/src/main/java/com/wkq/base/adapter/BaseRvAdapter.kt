package com.wkq.base.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.wkq.base.adapter.holder.BaseRvViewHolder


/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/23 9:59
 *
 *@Desc:
 */
@SuppressLint("NotifyDataSetChanged")
abstract class BaseRvAdapter<D, VB : ViewBinding> : RecyclerView.Adapter<BaseRvViewHolder<VB>>() {

    private var listData = mutableListOf<D>()

    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseRvViewHolder<VB>

    abstract override fun onBindViewHolder(holder: BaseRvViewHolder<VB>, position: Int)



    override fun getItemCount(): Int {
        return listData.size
    }

    fun getData(): MutableList<D> {
        return listData
    }

    fun setNewData(temp: MutableList<D>) {
        listData.clear()         // 保留原列表引用，避免 notify 不生效
        listData.addAll(temp)
        notifyDataSetChanged()
    }

    fun addData(temp: MutableList<D>) {
        listData.addAll(temp)
        notifyItemRangeChanged(listData.size - temp.size, temp.size)
    }

    fun addData( temp: D) {
        listData.add( temp)
        notifyItemInserted(listData.size-1)
    }
    fun addData(pos: Int, temp: D) {
        listData.add(pos, temp)
        notifyItemInserted(pos)
    }


    fun addData(pos: Int, temp: MutableList<D>) {
        listData.addAll(pos, temp)
        notifyItemRangeInserted(pos, temp.size)
    }

    fun removeData(pos: Int? = null) {
        if (pos == null) {
            listData.clear()
            notifyDataSetChanged()
        } else if (pos in listData.indices) {
            listData.removeAt(pos)
            notifyItemRemoved(pos)
            // 仅当后续还有 item 时再刷新
            if (pos < listData.size) {
                notifyItemRangeChanged(pos, listData.size - pos)
            }
        } else {
            Log.e("BaseRvAdapter", "Invalid remove pos: $pos, size: ${listData.size}")
        }
    }


    fun getItem(position: Int): D {
        return listData[position]
    }
    fun getItemOrNull(position: Int): D? {
        return if (position in listData.indices) listData[position] else null
    }

    var  mListener: OnItemClickListener?=null
    var  mLongListener: OnItemLongClickListener?=null
    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener) {
        mLongListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(view: View, position: Int)
    }
}