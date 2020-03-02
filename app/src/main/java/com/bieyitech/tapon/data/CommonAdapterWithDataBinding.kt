package com.bieyitech.tapon.data

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

/**
 * 使用Data Binding的通用适配器
 * @param context 上下文
 * @param dataList 数据列表
 * @param holderLayout ViewHolder的视图资源
 */
abstract class CommonAdapterWithDataBinding<T>(private val context: Context,
                                               private val dataList: MutableList<T>,
                                               @LayoutRes private val holderLayout: Int):
    RecyclerView.Adapter<CommonAdapterWithDataBinding.CommonViewHolderWithDataBinding>() {

    // 创建ViewHolder
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = CommonViewHolderWithDataBinding(DataBindingUtil.inflate(LayoutInflater.from(context), holderLayout, parent, false))

    override fun getItemCount(): Int = dataList.size

    // 绑定ViewHolder
    override fun onBindViewHolder(holder: CommonViewHolderWithDataBinding, position: Int) {
        bindData(holder, dataList[position], position)
        // 立即执行绑定，防止视图闪烁
        holder.viewBinding.executePendingBindings()
    }

    /**
     * 绑定数据的抽象方法。
     * 如对应的布局资源文件为example_layout.xml，其中有一个变量(variable)名为exampleData
     * 则生成的视图绑定类为：ExampleLayoutBinding
     * 绑定数据方法：
     * (holder.viewBinding as ExampleLayoutBinding).exampleData = data
     * @param holder ViewHolder
     * @param data 数据
     * @param position 数据的排位
     */
    abstract fun bindData(holder: CommonViewHolderWithDataBinding, data: T, position: Int)

    fun removeItem(item: T){
        dataList.remove(item)
        notifyDataSetChanged()
    }

    /**
     * 使用Data Binding的通用视图持有类
     * @param viewBinding 视图数据绑定类，需要转换为自定义布局文件对应的绑定类
     */
    class CommonViewHolderWithDataBinding(val viewBinding: ViewDataBinding)
        : RecyclerView.ViewHolder(viewBinding.root)
}