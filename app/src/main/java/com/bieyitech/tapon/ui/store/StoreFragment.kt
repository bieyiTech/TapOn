package com.bieyitech.tapon.ui.store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.bieyitech.tapon.FindTreasureBoxActivity
import com.bieyitech.tapon.MainActivity
import com.bieyitech.tapon.R
import com.bieyitech.tapon.bmob.Store
import com.bieyitech.tapon.bmob.StoreObject
import com.bieyitech.tapon.data.CommonAdapterWithDataBinding
import com.bieyitech.tapon.databinding.FragmentStoreBinding
import com.bieyitech.tapon.databinding.ItemStoreObjectBinding
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.model.RewardModelFactory
import com.bieyitech.tapon.widgets.WaitProgressDialog

/**
 * 商铺界面
 */
class StoreFragment private constructor(): Fragment() {

    companion object {
        private const val ARG_STORE_ID = "StoreID"

        fun newInstance(store: Store) = StoreFragment().apply {
            arguments = createStoreCodeBundle(store)
        }
        // 传入一个商铺标识代码
        private fun createStoreCodeBundle(store: Store) = Bundle().apply {
            putSerializable(ARG_STORE_ID, store)
        }
    }

    // UI控件
    private var _binding: FragmentStoreBinding? = null
    private val viewBinding get() = _binding!!

    private lateinit var mStore: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = arguments?.getSerializable(ARG_STORE_ID) as Store?
        if(store == null){
            context?.showToast("未获取到商铺信息")
            exitStoreFragment()
        }else{
            mStore = store
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStoreBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 退出该商铺
        viewBinding.storeExitBtn.setOnClickListener {
            exitStoreFragment()
        }
        // 下拉刷新
        viewBinding.storeStoreRewardsSrl.setOnRefreshListener {
            fetchStoreObjects()
        }

        fillStoreContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 填充商铺内容
     */
    private fun fillStoreContent() {
        viewBinding.storeShopName.text = resources.getString(R.string.store_shop_name_title, mStore.name)

        // 查询商铺奖品并填充
        fetchStoreObjects()
    }

    /**
     * 查询商铺奖品并填充
     */
    private fun fetchStoreObjects() {
        viewBinding.storeStoreRewardsSrl.isRefreshing = true
        BmobQuery<StoreObject>().apply {
            addWhereEqualTo("store", mStore)
            include("store")
        }.findObjects(object : FindListener<StoreObject>() {
            override fun done(p0: MutableList<StoreObject>?, p1: BmobException?) {
                viewBinding.storeStoreRewardsSrl.isRefreshing = false
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
        viewBinding.storeRewardsTitle.text = resources.getString(R.string.store_store_rewards_title, objectList.size)
        // 列表
        viewBinding.storeStoreRewardsRv.adapter = object : CommonAdapterWithDataBinding<StoreObject>(
            requireContext(), objectList, R.layout.item_store_object
        ){
            override fun bindData(
                holder: CommonViewHolderWithDataBinding,
                data: StoreObject,
                position: Int
            ) {
                with(holder.viewBinding as ItemStoreObjectBinding){
                    storeObject = data
                    storeObjectImg.setImageResource(RewardModelFactory.getRewardImg(data.imgCode))

                    storeObjectFindBtn.enableOnPressScaleTouchListener {
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