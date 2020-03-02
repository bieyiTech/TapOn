package com.bieyitech.tapon.data

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class CommonAdapterWithDataBinding<T>(private val context: Context,
                                      private val dataList: MutableList<T>,
                                      @LayoutRes private val holderLayout: Int):
    RecyclerView.Adapter<CommonAdapterWithDataBinding.CommonViewHolderWithDataBinding>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = CommonViewHolderWithDataBinding(DataBindingUtil.inflate(LayoutInflater.from(context), holderLayout, parent, false))

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: CommonViewHolderWithDataBinding, position: Int) {
        bindData(holder, dataList[position], position)
    }

    abstract fun bindData(holder: CommonViewHolderWithDataBinding, data: T, position: Int)

    class CommonViewHolderWithDataBinding(val viewBinding: ViewDataBinding)
        : RecyclerView.ViewHolder(viewBinding.root)
}