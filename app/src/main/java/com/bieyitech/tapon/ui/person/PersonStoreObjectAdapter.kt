package com.bieyitech.tapon.ui.person

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bieyitech.tapon.R
import com.bieyitech.tapon.bmob.StoreObject
import com.bieyitech.tapon.databinding.ItemStoreObjectBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PersonStoreObjectAdapter(private val context: Context,
                               private val storeObjectList: MutableList<StoreObject>)
    : RecyclerView.Adapter<PersonStoreObjectAdapter.PersonStoreObjectHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonStoreObjectHolder =
        PersonStoreObjectHolder(ItemStoreObjectBinding.inflate(LayoutInflater.from(context), parent, false))

    override fun getItemCount(): Int = storeObjectList.size

    override fun onBindViewHolder(holder: PersonStoreObjectHolder, position: Int) {
        val _storeObject = storeObjectList[position]
        holder.viewBinding.storeObject = _storeObject
        holder.viewBinding.storeObjectFindBtn.visibility = View.GONE

        holder.viewBinding.root.setOnClickListener {
            ItemStoreObjectBinding.inflate(LayoutInflater.from(context)).apply {
                storeObject = storeObjectList[position]
                storeObjectIntroTv.isSingleLine = false
                storeObjectFindBtn.visibility = View.GONE
            }.let {
                MaterialAlertDialogBuilder(context)
                    .setView(it.root)
                    .show()
            }
        }
        holder.viewBinding.root.setOnLongClickListener {
            // 长按删除
            MaterialAlertDialogBuilder(context, R.style.CustomDialog)
                .setTitle("确认删除奖品[${_storeObject.name}]吗？")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok){ _, _ ->
                    _storeObject.deleteStoreObject(context){
                        storeObjectList.remove(_storeObject)
                        notifyDataSetChanged()
                    }
                }.show()
            true
        }
        // 防止闪烁
        holder.viewBinding.executePendingBindings()
    }

    class PersonStoreObjectHolder(val viewBinding: ItemStoreObjectBinding)
        : RecyclerView.ViewHolder(viewBinding.root)

}