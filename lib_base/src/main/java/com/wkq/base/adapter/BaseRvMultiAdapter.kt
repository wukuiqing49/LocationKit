package com.wkq.base.adapter

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.wkq.base.adapter.holder.BaseRvMultiViewHolder
import com.wkq.base.adapter.bean.MultiItemEntity


/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/23 9:59
 *
 *@Desc:
 */
@SuppressLint("NotifyDataSetChanged")
abstract class BaseRvMultiAdapter<T: MultiItemEntity>() : RecyclerView.Adapter<BaseRvMultiViewHolder<ViewBinding>>() {

    private var listData = mutableListOf<T>()

    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseRvMultiViewHolder<ViewBinding>

    abstract override fun onBindViewHolder(holder: BaseRvMultiViewHolder<ViewBinding>, position: Int)


    override fun getItemCount(): Int = listData.size

    override fun getItemViewType(position: Int): Int {
        return listData[position].getItemType()
    }

    fun getData(): MutableList<T> {
        return listData
    }

    fun getItem(position:Int):T?{
        if (listData==null||position>=listData.size)return null
        return listData[position]
    }

    fun setNewData(temp: MutableList<T>) {
        listData = temp
        notifyDataSetChanged()
    }

    fun addData(temp: MutableList<T>) {
        listData.addAll(temp)
        notifyItemRangeChanged(listData.size - temp.size, temp.size)
    }
    fun addData( temp: T) {
        listData.add( temp)
        notifyItemInserted(listData.size-1)
    }
    fun addData(pos: Int, temp: T) {
        listData.add(pos, temp)
        notifyItemInserted(pos)
    }

    fun addData(pos: Int, temp: MutableList<T>) {
        listData.addAll(pos, temp)
        notifyItemRangeInserted(pos, temp.size)
    }

    fun removeData(pos: Int? = null) {
        if(pos == null){
            listData.clear()
            notifyDataSetChanged()
        }else{
            listData.removeAt(pos)
            notifyItemRemoved(pos)
        }

    }

    var  mListener: OnItemClickListener?=null
    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }


}