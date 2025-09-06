/*
 * ProjectPromote - An integrated dependency on my projects promotion for my own use.
 * Copyright (C) 2017-2024 Fankes Studio(qzmmcn@163.com)
 * https://github.com/fankes/TSBattery
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 *
 * This file is created by fankes on 2023/9/13.
 */
package com.fankes.projectpromote

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale

/**
 * 项目推广工具类
 */
object ProjectPromote {

    /** 推广链接的主要地址 (EN) */
    private const val PROJECT_PROMOTE_MAIN_EN_URL = "https://raw.githubusercontent.com/fankes/fankes/main/project-promote/README.md"

    /** 推广链接的主要地址 (CN) */
    private const val PROJECT_PROMOTE_MAIN_CN_URL = "https://raw.githubusercontent.com/fankes/fankes/main/project-promote/README-zh-CN.md"

    /** 推广链接的代理地址 (EN) */
    private const val PROJECT_PROMOTE_PROXY_EN_URL = "https://hub.gitmirror.com/$PROJECT_PROMOTE_MAIN_EN_URL"

    /** 推广链接的代理地址 (CN) */
    private const val PROJECT_PROMOTE_PROXY_CN_URL = "https://hub.gitmirror.com/$PROJECT_PROMOTE_MAIN_CN_URL"

    /** 记录的 Sp 名称 */
    private const val PROJECT_PROMOTE_SP_NAME = "project_promote_readed"

    /** 记录的 Sp 已读键值名称 */
    private const val PROJECT_PROMOTE_SP_IS_READ = "is_read"

    /**
     * 展示推广对话框
     * @param activity 当前实例
     * @param tag 当前标签 - 用于非重复展示对话框
     * @param dismissSeconds “不再提示” 按钮可点击的秒数 - 默认 10 秒
     */
    fun show(activity: ComponentActivity, tag: String, dismissSeconds: Int = 10) {
        val sp = createSp(activity, tag)
        if (sp.getBoolean(PROJECT_PROMOTE_SP_IS_READ, false)) return
        var isEnableButton1 = false
        var isEnableButton2 = false
        val dialog = createDialog(activity)
        dialog.show()
        val messageView = dialog.findViewById<TextView>(android.R.id.message) ?: error("Missing message text")
        val button1 = dialog.findViewById<Button>(android.R.id.button1) ?: error("Missing button 1")
        val button2 = dialog.findViewById<Button>(android.R.id.button2) ?: error("Missing button 2")
        button1.isEnabled = false
        button2.isEnabled = false
        button1.setOnClickListener { if (isEnableButton1) dialog.dismiss() }
        button2.setOnClickListener {
            if (isEnableButton2.not()) return@setOnClickListener
            sp.edit { putBoolean(PROJECT_PROMOTE_SP_IS_READ, true) }
            dialog.dismiss()
        }
        val markwon = createMarkdown(activity)
        activity.lifecycleScope.launch(Dispatchers.IO) {
            /**
             * 执行解析逻辑
             * @param isDone 是否成功
             * @param content 内容
             */
            suspend fun doParse(isDone: Boolean, content: String) {
                withContext(Dispatchers.Main) {
                    if (isDone) {
                        markwon.setMarkdown(messageView, content)
                        messageView.invalidate()
                        messageView.requestLayout()
                    } else messageView.text = activity.getString(R.string.promote_dialog_message_fail)
                    button1.text = activity.getText(R.string.promote_dialog_button_1)
                    button2.text = ""
                    button1.isEnabled = true
                    isEnableButton1 = true
                }
                if (isDone) for (seconds in dismissSeconds downTo 0) {
                    withContext(Dispatchers.Main) {
                        val button2Text = activity.getString(R.string.promote_dialog_button_2)
                        button2.text = if (seconds > 0) "$button2Text ($seconds)" else button2Text
                        button2.isEnabled = seconds == 0
                        isEnableButton2 = seconds == 0
                    }; delay(1000)
                }
            }

            val mainUrl = if (isSystemLanguageSimplifiedChinese) PROJECT_PROMOTE_MAIN_CN_URL else PROJECT_PROMOTE_MAIN_EN_URL
            val proxyUrl = if (isSystemLanguageSimplifiedChinese) PROJECT_PROMOTE_PROXY_CN_URL else PROJECT_PROMOTE_PROXY_EN_URL
            loadContentFromUrl(mainUrl) { isDone, content ->
                if (isDone) doParse(isDone = true, content)
                else loadContentFromUrl(proxyUrl) { isDone2, content2 -> doParse(isDone2, content2) }
            }
        }
    }

    /**
     * 创建 Sp 存储
     * @param activity 当前实例
     * @param tag 当前标签
     * @return [SharedPreferences]
     */
    private fun createSp(activity: Activity, tag: String) = activity.getSharedPreferences("${PROJECT_PROMOTE_SP_NAME}_$tag", Context.MODE_PRIVATE)

    /**
     * 创建对话框
     * @param activity 当前实例
     * @return [AlertDialog]
     */
    private fun createDialog(activity: Activity) =
        MaterialAlertDialogBuilder(activity)
            .setCancelable(false)
            .setTitle(activity.getString(R.string.promote_dialog_title))
            .setMessage(activity.getString(R.string.promote_dialog_message_loading))
            .setPositiveButton("...", null)
            .setNegativeButton("...", null)
            .create()

    /**
     * 创建 [Markwon]
     * @param activity 当前实例
     * @return [Markwon]
     */
    private fun createMarkdown(activity: Activity) =
        Markwon.builder(activity)
            .usePlugin(ImagesPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .build()

    /**
     * 从 URL 读取内容
     * @param url 当前 URL
     * @param onResponse 回调结果 - ([Boolean] 是否成功,[String] 内容)
     */
    private suspend fun loadContentFromUrl(url: String, onResponse: suspend (isDone: Boolean, content: String) -> Unit) {
        runCatching {
            val response = OkHttpClient().newCall(Request.Builder().url(url).build()).execute()
            val content = response.body?.string() ?: ""
            onResponse(response.isSuccessful && content.isNotBlank(), content)
        }.onFailure { onResponse(false, "") }
    }

    /**
     * 当前系统环境是否为简体中文
     * @return [Boolean]
     */
    private val isSystemLanguageSimplifiedChinese
        get(): Boolean {
            val locale = Locale.getDefault()
            return locale.language == "zh" && locale.country == "CN"
        }
}