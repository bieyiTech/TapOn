/**
 * @author GizFei
 */
package com.bieyitech.tapon.ui.person

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.forEach
import androidx.databinding.adapters.ViewBindingAdapter
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import cn.bmob.v3.BmobUser
import com.bieyitech.tapon.LoginActivity
import com.bieyitech.tapon.R
import com.bieyitech.tapon.bmob.Store
import com.bieyitech.tapon.bmob.TaponUser
import com.bieyitech.tapon.databinding.FragmentPersonBinding
import com.bieyitech.tapon.helpers.printLog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PersonFragment : Fragment() {

    // 当前用户
    private val taponUser by lazy { BmobUser.getCurrentUser(TaponUser::class.java) }

    // UI组件，视图绑定
    private var _binding: FragmentPersonBinding? = null
    private val viewBinding get() = _binding!!
    private lateinit var personViewPagerAdapter: PersonViewPagerAdapter

    // 网络连接管理器
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 绑定视图
        _binding = FragmentPersonBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 初始化ViewPager，并与TabLayout关联
        personViewPagerAdapter = PersonViewPagerAdapter()
        viewBinding.personViewPager.adapter = personViewPagerAdapter
        viewBinding.personTabLayout.setupWithViewPager(viewBinding.personViewPager)

        setupButtonClickListeners()
        fillUserInfo()

        connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                context?.printLog("连接上网络，刷新页面")
                activity?.runOnUiThread {
                    personViewPagerAdapter.refreshAllPages()    // 在主线程刷新所有页面
                }
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        connectivityManager.unregisterNetworkCallback(networkCallback) // 注销
        _binding = null // 解绑
    }

    /**
     * 为按钮设置点击事件
     */
    private fun setupButtonClickListeners() {
        viewBinding.personLogoutBtn.setOnClickListener {
            // 登出
            MaterialAlertDialogBuilder(requireContext(), R.style.CustomDialog)
                .setTitle("确认退出登录吗？")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok){ _, _ ->
                    BmobUser.logOut()
                    startActivity(LoginActivity.newIntent(requireContext()))
                    requireActivity().finish()
                }.show()
        }
    }

    /**
     * 填充用户信息并获取奖品信息、商铺信息
     */
    private fun fillUserInfo() {
        taponUser.apply {
            viewBinding.personNicknameTv.text = nickname
            viewBinding.personUserTypeTv.text = if(isCustomer()) "顾客" else "商家"
        }
    }

    /**
     * 顾客创建商铺后更新界面
     */
    fun convertToMerchant() {
        taponUser.type = TaponUser.UserType.MERCHANT.ordinal
        fillUserInfo()
        personViewPagerAdapter = PersonViewPagerAdapter()
        viewBinding.personViewPager.adapter = personViewPagerAdapter
    }

    // ViewPager视图适配器
    private inner class PersonViewPagerAdapter : PagerAdapter() {

        private val titles = if(taponUser.isMerchant()) arrayOf("我的奖品", "商铺", "商铺奖品")
            else arrayOf("我的奖品", "商铺")
        // 标题
        private val cachePages = SparseArray<BasePersonTabPage>(titles.size) // 页面缓存

        // 刷新所有页面
        fun refreshAllPages() {
            cachePages.forEach { _, page ->
                page.refreshPage()
            }
        }

        override fun getCount(): Int = titles.size

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val cacheView = cachePages[position]?.getView()
            if(cacheView != null){
                container.addView(cacheView)
                return cacheView
            }
            val basePage =  when(position) {
                0 -> {
                    PersonRewardObjectPage(this@PersonFragment, requireContext())
                }
                1 -> {
                    PersonStorePage(this@PersonFragment, requireContext())
                }
                2 -> {
                    PersonStoreObjectPage(this@PersonFragment, requireContext())
                }
                else -> object : BasePersonTabPage(){
                    override fun getView(): View = View(context)
                }
            }
            cachePages.put(position, basePage)
            container.addView(basePage.getView())
            return basePage.getView()
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean = (view == `object` as View)

        // 与TabLayout关联
        override fun getPageTitle(position: Int): CharSequence? = titles[position]
    }
}