package com.zhipu.realtime.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.zhipu.realtime.R
import com.zhipu.realtime.constants.RealtimeConstants
import com.lxj.xpopup.core.BottomPopupView

@SuppressLint("ViewConstructor")
class SystemPromptEditorPopup(context: Context, private val listener: OnPopupSaveListener) : BottomPopupView(context) {
    companion object {
        const val TAG: String = RealtimeConstants.TAG + "-SystemPromptEditorPopup"
    }

    override fun getImplLayoutId(): Int {
        return R.layout.system_prompt_editor
    }

    override fun onCreate() {
        super.onCreate()
        findViewById<Button>(R.id.btn_finish).setOnClickListener {
            saveSystemPrompt()
            dismiss()
        }
    }

    private fun saveSystemPrompt() {
        val etComment = findViewById<EditText>(R.id.et_comment)
        val systemPrompt = etComment.text.toString().trim()
        if (systemPrompt.isNotEmpty()) {
            val shortenedPrompt = if (systemPrompt.length > 20) {
                systemPrompt.take(10) + "..." + systemPrompt.takeLast(7)
            } else {
                systemPrompt
            }
            Log.d(TAG, "save system_prompt is $shortenedPrompt")
            listener.debugViewPrintln("更改提示词: $shortenedPrompt")
            listener.onPopupSave(systemPrompt)
        }
    }
}