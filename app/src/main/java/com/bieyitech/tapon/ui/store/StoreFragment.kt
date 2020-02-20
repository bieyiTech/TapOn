package com.bieyitech.tapon.ui.store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import cn.bmob.v3.listener.QueryListener
import com.bieyitech.tapon.FindTreasureBoxActivity
import com.bieyitech.tapon.MainActivity
import com.bieyitech.tapon.R
import com.bieyitech.tapon.bmob.Store
import com.bieyitech.tapon.bmob.StoreObject
import com.bieyitech.tapon.data.CommonAdapter
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.widgets.ShadeTextView
import com.bieyitech.tapon.widgets.WaitProgressDialog

/**
 * 商铺界面
 */
class StoreFragment : Fragment() {

    companion object {
        private const val ARG_STORE_ID = "StoreID"

        fun newInstance(storeId: String) = StoreFragment().apply {
            arguments = createStoreCodeBundle(storeId)
        }
        // 传入一个商铺标识代码
        private fun createStoreCodeBundle(storeId: String) = Bundle().apply {
            putString(ARG_STORE_ID, storeId)
        }
    }

    // UI控件
    private lateinit var storeRewardsTitle: ShadeTextView
    private lateinit var storeNameTv: TextView
    private lateinit var storeObjectsRv: RecyclerView
    private lateinit var waitProgressDialog: WaitProgressDialog

    private var storeId: String = ""
    private lateinit var mStore: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storeId = arguments?.getString(ARG_STORE_ID) ?: ""
        context?.printLog("商铺代码：$storeId")

        waitProgressDialog = WaitProgressDialog(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_store, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        storeRewardsTitle = view.findViewById(R.id.store_rewards_title)
        storeNameTv = view.findViewById(R.id.store_shop_name)
        storeObjectsRv = view.findViewById(R.id.store_store_rewards_rv)

        // 查询商铺信息
        waitProgressDialog.show()
        BmobQuery<Store>().getObject(storeId, object : QueryListener<Store>() {
            override fun done(p0: Store?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null && p0 != null){
                    mStore = p0
                    fillStoreContent() // 填充商铺信息
                }else{
                    context?.showToast("无效的二维码信息")
                    context?.printLog("查询商铺错误：$p1")
                    exitStoreFragment()
                }
            }
        })
    }

    /**
     * 填充商铺内容
     */
    private fun fillStoreContent() {
        storeNameTv.text = HtmlCompat.fromHtml(resources.getString(R.string.store_shop_name_title, mStore.name),
            HtmlCompat.FROM_HTML_MODE_COMPACT)

        // 查询商铺奖品并填充
        fetchStoreObjects()
    }

    /**
     * 查询商铺奖品并填充
     */
    private fun fetchStoreObjects() {
        BmobQuery<StoreObject>().apply {
            addWhereEqualTo("store", mStore)
            include("store")
        }.findObjects(object : FindListener<StoreObject>() {
            override fun done(p0: MutableList<StoreObject>?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null && p0 != null){
                    fillStoreObjectsContent(p0)
                }else{
                    context?.apply {
                        showToast("查询商铺奖品失败")
                        printLog("查询商铺奖品失败：$p1")
                    }
                }
            }
        })
    }

    /**
     * 填充商铺奖品列表
     */
    private fun fillStoreObjectsContent(objectList: MutableList<StoreObject>) {
        // 数量
        storeRewardsTitle.text = resources.getString(R.string.store_store_rewards_title, objectList.size)
        // 列表
        storeObjectsRv.adapter = object : CommonAdapter<StoreObject>(requireContext(), objectList, R.layout.item_store_object) {
            override fun bindData(holder: CommonViewHolder, data: StoreObject, position: Int) {
                with(holder) {
                    setText(R.id.store_object_name_tv, data.name)
                    setText(R.id.store_object_intro_tv, data.intro)
                }
                with(holder.itemView) {
                    findViewById<ShadeTextView>(R.id.store_object_find_btn).enableOnPressScaleTouchListener {
                        startActivity(FindTreasureBoxActivity.newIntent(requireContext(), data))
                    }
                }
            }
        }
    }

    /**
     * 退出商铺界面
     */
    private fun exitStoreFragment() {
        if(activity is MainActivity) {
            (activity as MainActivity).exitStoreState()
        }else{
            context?.printLog("关联活动不是MainActivity")
            onDestroy()
        }
    }
}