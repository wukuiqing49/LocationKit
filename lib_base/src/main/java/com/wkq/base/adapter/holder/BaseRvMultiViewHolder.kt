package com.wkq.base.adapter.holder

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/23 9:59
 *
 *@Desc:
 */
class BaseRvMultiViewHolder<VB : ViewBinding>(var binding: VB) : RecyclerView.ViewHolder(binding.root)