package com.bieyitech.tapon.ui.person

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.bieyitech.tapon.PutTreasureBoxActivity
import com.bieyitech.tapon.R
import com.bieyitech.tapon.bmob.StoreObject
import com.bieyitech.tapon.data.CommonAdapterWithDataBinding
import com.bieyitech.tapon.databinding.ItemStoreObjectBinding
import com.bieyitech.tapon.databinding.PersonTabStoreObjectBinding
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * “我的”页面中的“商铺奖品”标签页内容
 */
class PersonStoreObjectPage(private val personFragment: PersonFragment,
                            private val context: Context): BasePersonTabPage() {

    private val mViewBinding = PersonTabStoreObjectBinding.inflate(LayoutInflater.from(context))

    init {
        fetchStoreObjects()
        mViewBinding.personStoreObjectSrl.setOnRefreshListener {
            fetchStoreObjects()
        }
    }

    override fun getView(): View = mViewBinding.root

    override fun refreshPage() {
        fetchStoreObjects()
    }

    /**
     * 查询商铺拥有的奖品信息并填充
     */
    private fun fetchStoreObjects() {
        mViewBinding.personStoreObjectSrl.isRefreshing = true
        BmobQuery<StoreObject>().apply {
            addWhereEqualTo("user", taponUser)
            include("store")
        }.findObjects(object : FindListener<StoreObject>() {
            override fun done(p0: MutableList<StoreObject>?, p1: BmobException?) {
                mViewBinding.personStoreObjectSrl.isRefreshing = false
                if(p1 == null && p0 != null){
                    fillStoreObjectsContent(p0)
                }else{
                    context.apply {
                        showToast(if(p1?.errorCode == 9016) "网络不可用" else "未找到奖品")
                        printLog("未找到奖品：$p1")
                    }
                }
            }
        })
    }

    /**
     * 填充奖品列表
     */
    private fun fillStoreObjectsContent(objectList: MutableList<StoreObject>) {
        mViewBinding.personStoreObjectsRv.adapter = object : CommonAdapterWithDataBinding<StoreObject>(
            context, objectList, R.layout.item_store_object
        ){
            override fun bindData(
                holder: CommonViewHolderWithDataBinding,
                data: StoreObject,
                position: Int
            ) {
                with(holder.viewBinding as ItemStoreObjectBinding) {
                    storeObject = data
                    storeObjectFindBtn.visibility = View.GONE

                    root.setOnClickListener {
                        ItemStoreObjectBinding.inflate(LayoutInflater.from(context)).apply {
                            storeObject = data
                            storeObjectIntroTv.isSingleLine = false
                            storeObjectFindBtn.visibility = View.GONE
                        }.let {
                            MaterialAlertDialogBuilder(context)
                                .setView(it.root)
                                .show()
                        }
                    }
                    root.setOnLongClickListener {
                        // 长按删除
                        MaterialAlertDialogBuilder(context, R.style.CustomDialog)
                            .setTitle("确认删除奖品[${data.name}]吗？")
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok){ _, _ ->
                                data.deleteStoreObject(context){
                                    removeItem(data)
                                }
                            }.show()
                        true
                    }
                }
            }
        }
    } 
}