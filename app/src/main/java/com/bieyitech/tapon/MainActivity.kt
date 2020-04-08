/**
 * @author GizFei
 */
package com.bieyitech.tapon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import cn.bmob.v3.Bmob
import cn.bmob.v3.BmobUser
import com.bieyitech.tapon.bmob.APPLICATION_ID
import com.bieyitech.tapon.bmob.Store
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.ui.QRCodeFragment
import com.bieyitech.tapon.ui.person.PersonFragment
import com.bieyitech.tapon.ui.store.StoreFragment

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 0
    }

    private var qrCodeFragment: QRCodeFragment? = null
    private var storeFragment: StoreFragment? = null
    private var personFragment: PersonFragment? = null

    private var isInStoreState = false // 是否进入商铺

    private var numberOfRequestPermission: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 请求相关动态权限
        if(!requestDynamicPermissions()){
            start()
        }
    }

    // 启动
    private fun start() {
        // 初始化Bmob，并检查是否有用户登录
        Bmob.initialize(this, APPLICATION_ID)
        if(!BmobUser.isLogin()){
            startActivity(LoginActivity.newIntent(this))
            finish()
        } else {
            setContentView(R.layout.activity_main)
            val navView: BottomNavigationView = findViewById(R.id.main_nav_view)
            navView.setOnNavigationItemSelectedListener {
                // printLog("底部菜单：${it.title}")
                when(it.itemId) {
                    R.id.navigation_home -> { setFragment(1) }
                    R.id.navigation_person -> { setFragment(2) }
                }
                true
            }
            setFragment(1)
        }
    }

    /**
     * 设置当前[Fragment]
     * @param i: 1, 2
     */
    private fun setFragment(i: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        if (qrCodeFragment != null && !isInStoreState){
            transaction.hide(qrCodeFragment!!)
        }
        if (storeFragment != null && isInStoreState){
            transaction.hide(storeFragment!!)
        }
        if (personFragment != null){
            transaction.hide(personFragment!!)
        }
        when(i) {
            1 -> {
                if(!isInStoreState){
                    if(qrCodeFragment == null){
                        qrCodeFragment = QRCodeFragment().also {
                            transaction.add(R.id.main_fragment_container, it)
                        }
                    }
                    transaction.show(qrCodeFragment!!)
                }else{
                    if(storeFragment == null){
                        storeFragment = StoreFragment.newInstance(Store()).also {
                            transaction.add(R.id.main_fragment_container, it)
                        }
                    }
                    transaction.show(storeFragment!!)
                }
            }
            2 -> {
                if(personFragment == null){
                    personFragment = PersonFragment().also {
                        transaction.add(R.id.main_fragment_container, it)
                    }
                }
                transaction.show(personFragment!!)
            }
        }
        transaction.commit()
    }

    // 动态请求权限
    private fun requestDynamicPermissions(): Boolean {
        val permissions = arrayListOf<String>()
        if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if(permissions.isNotEmpty()){
            numberOfRequestPermission = permissions.size
            requestPermissions(permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            return true
        }
        return false
    }

    // 权限请求结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == PERMISSION_REQUEST_CODE) {
            if(grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // 权限通过
                start()
            }else{
                showToast(R.string.no_permission_text)
                finish()
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /**
     * 由二维码扫描界面进入商铺界面
     * @param store 商铺实例
     */
    fun enterStoreState(store: Store) {
        isInStoreState = true
        storeFragment = StoreFragment.newInstance(store).also {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                .remove(qrCodeFragment!!)
                .add(R.id.main_fragment_container, it)
                .commit()
            qrCodeFragment = null
        }
    }

    /**
     * 退出商铺界面，进入二维码扫描界面
     */
    fun exitStoreState() {
        isInStoreState = false
        qrCodeFragment = QRCodeFragment().also {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                .remove(storeFragment!!)
                .add(R.id.main_fragment_container, it)
                .commit()
            storeFragment = null
        }
    }
}


/*
val navController = findNavController(R.id.nav_host_fragment) // 导航控制器
            // 传入菜单的ID
            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_home, R.id.navigation_person
                )
            )
            // setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)
 */