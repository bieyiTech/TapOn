package com.bieyitech.tapon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.bmob.v3.Bmob
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import com.bieyitech.tapon.bmob.APPLICATION_ID
import com.bieyitech.tapon.bmob.TaponUser
import com.bieyitech.tapon.helpers.isEmail
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.widgets.WaitProgressDialog
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        fun newIntent(context: Context) = Intent(context, RegisterActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        Bmob.initialize(this, APPLICATION_ID)

        setupListeners()
    }

    private fun setupListeners() {
        register_back_btn.setOnClickListener(this)
        register_register_btn.setOnClickListener(this)
        register_register_btn.enableOnPressScaleTouchListener(onClickListener = this)

        register_username_et.addEditTextChangedListener {
            onAfterTextChanged {
                val username = it.toString()
                // 判断用户名格式
                if(username.contains(Regex("[@/]"))){
                    register_username_layout.error = resources.getString(R.string.wrong_username_hint)
                }else{
                    if(register_username_layout.error != ""){
                        register_username_layout.error = ""
                    }
                }
            }
        }
        register_email_et.addEditTextChangedListener {
            onAfterTextChanged {
                if(register_email_layout.error != "") {
                    register_email_layout.error = ""
                }
            }
        }
        register_password_et.addEditTextChangedListener {
            onAfterTextChanged {
                if(register_confirm_psd_layout.error != "") {
                    register_confirm_psd_layout.error = ""
                }
            }
        }
        register_confirm_psd_et.addEditTextChangedListener  {
            onAfterTextChanged {
                if(register_confirm_psd_layout.error != "") {
                    register_confirm_psd_layout.error = ""
                }
            }
        }
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.register_back_btn -> { onBackPressed() }
            R.id.register_register_btn -> {
                // 注册
                register()
            }
        }
    }

    private fun register() {
        val username = register_username_et.text.toString()
        val email = register_email_et.text.toString()
        val psd = register_password_et.text.toString()
        val confirmPsd = register_confirm_psd_et.text.toString()
        // 判断用户名格式
        if(username.isEmpty() || username.isBlank() || username.contains(Regex("[@/]"))){
            register_username_layout.error = resources.getString(R.string.wrong_username_hint)
            return
        }
        // 判断邮箱格式
        if(!email.isEmail()) {
            register_email_layout.error = resources.getString(R.string.wrong_email_hint)
            return
        }
        // 判断两次密码是否相同，长度大于8，不能有空格
        if(psd.length < 8) {
            register_confirm_psd_layout.error = resources.getString(R.string.wrong_password_length_hint)
            return
        }
        if(psd.contains(" ")){
            register_confirm_psd_layout.error = resources.getString(R.string.wrong_password_blank_hint)
            return
        }
        if(psd != confirmPsd){
            register_confirm_psd_layout.error = resources.getString(R.string.wrong_password_same_hint)
            return
        }
        // 注册
        val waitDialog = WaitProgressDialog(this).apply { show() }
        Log.d("RegisterActivity", "[${username}], [${email}], [$psd]")
        // 新建用户，默认为顾客
        val user = TaponUser().apply {
            this.username = username
            nickname = username // 昵称默认为用户名
            setPassword(psd)
            this.email = email
        }
        user.signUp(object : SaveListener<TaponUser>() {
            override fun done(user: TaponUser?, e: BmobException?) {
                waitDialog.dismiss()
                if(e == null) {
                    showToast("注册成功")
                    onBackPressed()
                }else{
                    e.printStackTrace()
                    Log.e("RegisterActivity", e.toString())
                    if(e.errorCode == 202){
                        showToast("用户名已存在")
                    }else if(e.errorCode == 203){
                        showToast("邮箱已被注册")
                    }else{
                        showToast("注册失败")
                    }
                }
            }
        })
    }

    // EditText文本变化监听器
    private fun EditText.addEditTextChangedListener(listener: TextWatcherBuilder.() -> Unit) {
        this.addTextChangedListener(TextWatcherBuilder().apply(listener).build())
    }
    private class TextWatcherBuilder {

        private var afterTextChanged: (Editable?) -> Unit = {}
        private var beforeTextChanged: (CharSequence?, Int, Int, Int) -> Unit = {_, _, _, _ -> }
        private var _onTextChanged: (CharSequence?, Int, Int, Int) -> Unit = {_, _, _, _ -> }

        internal fun build() = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = this@TextWatcherBuilder.afterTextChanged(s)

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                this@TextWatcherBuilder.beforeTextChanged(s, start, count, after)

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                this@TextWatcherBuilder._onTextChanged(s, start, before, count)
        }

        fun onAfterTextChanged(action: (s: Editable?) -> Unit) {
            afterTextChanged = action
        }

        fun onBeforeTextChanged(action: (s: CharSequence?, start: Int, count: Int, after: Int) -> Unit) {
            beforeTextChanged = action
        }

        fun onTextChanged(action: (s: CharSequence?, start: Int, count: Int, after: Int) -> Unit) {
            _onTextChanged = action
        }

    }
}




/*
// EditText文本变化监听器
    private open class CustomTextWatcher : TextWatcher {

        private lateinit var mWatcherBuilder: WatcherBuilder

        fun registerWatcherEvent(listener: WatcherBuilder.() -> Unit) {
            mWatcherBuilder = WatcherBuilder().apply(listener)
        }

        inner class WatcherBuilder {
            internal var afterTextChanged: (Editable?) -> Unit = {}
            internal var beforeTextChanged: (CharSequence?, Int, Int, Int) -> Unit = {_, _, _, _ -> }
            internal var _onTextChanged: (CharSequence?, Int, Int, Int) -> Unit = {_, _, _, _ -> }

            fun onAfterTextChanged(action: (s: Editable?) -> Unit) {
                afterTextChanged = action
            }

            fun onBeforeTextChanged(action: (s: CharSequence?, start: Int, count: Int, after: Int) -> Unit) {
                beforeTextChanged = action
            }

            fun onTextChanged(action: (s: CharSequence?, start: Int, count: Int, after: Int) -> Unit) {
                _onTextChanged = action
            }
        }

        override fun afterTextChanged(s: Editable?) {
            if(this::mWatcherBuilder.isInitialized){
                mWatcherBuilder.afterTextChanged(s)
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            if(this::mWatcherBuilder.isInitialized){
                mWatcherBuilder.beforeTextChanged(s, start, count, after)
            }
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if(this::mWatcherBuilder.isInitialized){
                mWatcherBuilder._onTextChanged(s, start, before, count)
            }
        }
    }
 */