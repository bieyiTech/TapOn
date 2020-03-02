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
import com.bieyitech.tapon.bmob.StoreObject
import com.bieyitech.tapon.databinding.PersonTabStoreObjectBinding
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast

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

    /**
     * 查询商铺拥有的奖品信息并填充
     * @param rv: 被填充的RecyclerView
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
                    mViewBinding.personStoreObjectsRv.adapter = PersonStoreObjectAdapter(context, p0)
                }else{
                    context.apply {
                        showToast("未找到奖品")
                        printLog("未找到奖品：$p1")
                    }
                }
            }
        })
    }
}