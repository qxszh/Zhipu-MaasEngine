package com.zhipu.realtime.ui

interface OnPopupSaveListener {
    fun onPopupSave(systemPrompt: String)
    fun debugViewPrintln(content: String, color: Int = 0xFFFFFFFF.toInt())
}