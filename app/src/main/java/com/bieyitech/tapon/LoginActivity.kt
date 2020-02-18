package com.bieyitech.tapon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import cn.bmob.v3.Bmob
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import com.bieyitech.tapon.bmob.APPLICATION_ID
import com.bieyitech.tapon.bmob.TaponUser
import com.bieyitech.tapon.helpers.clearText
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.widgets.WaitProgressDialog
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        fun newIntent(context: Context) = Intent(context, LoginActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Bmob.initialize(this, APPLICATION_ID)

        setupListeners()
    }

    private fun setupListeners() {
        login_login_btn.enableOnPressScaleTouchListener(onClickListener = this)
        login_register_btn.enableOnPressScaleTouchListener(onClickListener = this)
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.login_login_btn -> {
                // 登录
                login()
            }
            R.id.login_register_btn -> {
                login_username_et.clearText()
                login_password_et.clearText()
                val transition = ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                    Pair<View, String>(login_username_et, resources.getString(R.string.share_username_et)),
                    Pair<View, String>(login_password_et, resources.getString(R.string.share_password_et)),
                    Pair<View, String>(login_register_btn, resources.getString(R.string.share_register_btn)),
                    Pair<View, String>(login_app_logo, resources.getString(R.string.share_app_logo))
                )
                startActivity(RegisterActivity.newIntent(this), transition.toBundle())
            }
        }
    }

    private fun login() {
        val waitDialog = WaitProgressDialog(this).apply { show() }
        val username = login_username_et.text.toString()
        val password = login_password_et.text.toString()
        if(username.isEmpty() && password.isEmpty()){
            waitDialog.dismiss()
            showToast(getString(R.string.login_wrong_string_hint))
            return
        }

        val user = TaponUser().apply {
            this.username = username
            this.setPassword(password)
        }
        // printLog("用户：${user.username}, $password")
        user.login(object : SaveListener<TaponUser>() {
            override fun done(user: TaponUser?, e: BmobException?) {
                waitDialog.dismiss()
                if(e == null){
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }else{
                    showToast("登录失败，请检查你的用户名和密码")
                    printLog("登录失败：$e")
                }
            }
        })
    }
}