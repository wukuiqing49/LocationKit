package com.wkq.base.fragment


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView


import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.wkq.base.R
import com.wkq.base.databinding.FragmentBaseListBinding
import com.wkq.base.util.showToast


/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/23 9:59
 *
 *@Desc:
 */
abstract class BasicVmListFragment<VM : ViewModel> : BasicVmFragment<VM>() {

    lateinit var binding: FragmentBaseListBinding

    private fun createVB(inflater: LayoutInflater, group: ViewGroup?): FragmentBaseListBinding {
        return FragmentBaseListBinding.inflate(inflater, group, false)
    }

    override fun createView(
        inflater: LayoutInflater, group: ViewGroup?
    ): View? {
        binding = createVB(inflater, group)
        addHeaderView()?.let { binding.root.addView( addHeaderView(),0) }
        return binding.root
    }

    open fun addHeaderView():View?{
        return null
    }

    override fun initView() {
        setEmptyStyle()
        binding.mSmartRefreshLayout.setRefreshHeader(ClassicsHeader(context))
        binding.mSmartRefreshLayout.setRefreshFooter(ClassicsFooter(context))
        binding.mSmartRefreshLayout.setOnRefreshListener {
            onRefresh()
        }
        binding.mSmartRefreshLayout.setOnLoadMoreListener {
            onLoadMore()
        }
        binding.mRecyclerView.layoutManager = getLayoutManager()
        setAdapter()
    }



    fun finishRefreshAndLoadMore(noMoreData: Boolean, delayed: Int = 200) {
        if (noMoreData) {
            binding.mSmartRefreshLayout.finishLoadMore(200, noMoreData, noMoreData)
        } else {
            binding.mSmartRefreshLayout.finishRefresh(delayed)
            binding.mSmartRefreshLayout.finishLoadMore(delayed)
        }
    }

    //开启刷新
    fun setEnableRefresh() {
        binding.mSmartRefreshLayout.setEnableRefresh(true)

    }

    //开启 加载
    fun setEnableLoadMore() {
        binding.mSmartRefreshLayout.setEnableLoadMore(true)

    }

    fun closeRefresh(){
        binding.mSmartRefreshLayout.setEnableLoadMore(false)
        binding.mSmartRefreshLayout.setEnableRefresh(false)
    }

    fun showMessage(msg: String?) {
        msg?.let {
            showToast(it)
        }
    }

    fun setPadding(  left:Int=0,  top:Int=0,  right:Int=0,  bottom:Int=0){
        binding.mSmartRefreshLayout.setPadding(left,top,right,bottom)
    }

    /***
     * 空布局的展示和隐藏
     * @param isShow Boolean
     */
    fun showEmpty(isShow: Boolean) {
        if (isShow) {
            binding.mSmartRefreshLayout.visibility = View.GONE
            binding.empty.visibility = View.VISIBLE
        } else {
            binding.mSmartRefreshLayout.visibility = View.VISIBLE
            binding.empty.visibility = View.GONE

        }
    }

    /**
     *设置空布局样式
     */
    fun setEmptyStyle(
        resId: Int? = R.mipmap.iv_empty_default, emptyText: String? = "暂无数据",
        textColor: Int? = R.color.black,
        textSize: Float? = 14f
    ) {
        binding.empty.setEmpty(resId, emptyText, textColor, textSize)
    }

    abstract fun getLayoutManager(): RecyclerView.LayoutManager
    abstract fun setAdapter()

    //刷新
    abstract fun onRefresh()

    // 加载
    abstract fun onLoadMore()
}