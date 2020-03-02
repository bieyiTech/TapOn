/**
 * @author GizFei
 */
package com.bieyitech.tapon.ui.person

import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        viewBinding.personViewPager.adapter = PersonViewPagerAdapter()
        viewBinding.personTabLayout.setupWithViewPager(viewBinding.personViewPager)

        setupButtonClickListeners()
        fillUserInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
    fun fillUserInfo() {
        taponUser.apply {
            viewBinding.personNicknameTv.text = nickname
            viewBinding.personUserTypeTv.text = if(isCustomer()) "顾客" else "商家"
        }
    }

    private inner class PersonViewPagerAdapter : PagerAdapter() {

        private val titles = arrayOf("我的奖品", "商铺", "商铺奖品")
        private val cacheViews = SparseArray<View>(titles.size) // 视图缓存

        override fun getCount(): Int = titles.size

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val cacheView = cacheViews[position]
            if(cacheView != null){
                container.addView(cacheView)
                return cacheView
            }
            val view =  when(position) {
                0 -> {
                    PersonRewardObjectPage(this@PersonFragment, requireContext()).getView()
                }
                1 -> {
                    PersonStorePage(this@PersonFragment, requireContext()).getView()
                }
                2 -> {
                    PersonStoreObjectPage(this@PersonFragment, requireContext()).getView()
                }
                else -> View(context)
            }
            cacheViews.put(position, view)
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean = (view == `object` as View)

        override fun getPageTitle(position: Int): CharSequence? = titles[position]
    }
}