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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.method.ArrowKeyMovementMethod
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.LineBackgroundSpan
import android.text.style.MetricAffectingSpan
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.SpanFactory
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.spans.CodeBlockSpan
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.destination.ImageDestinationProcessorRelativeToAbsolute
import io.noties.markwon.image.network.OkHttpNetworkSchemeHandler
import io.noties.markwon.movement.MovementMethodPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.commonmark.node.Code
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * 项目推广工具类
 */
object ProjectPromote {

    /** 推广链接的主要地址 (EN) */
    private const val PROJECT_PROMOTE_MAIN_EN_URL = "https://raw.githubusercontent.com/fankes/fankes/main/project-promote/README.md"

    /** 推广链接的主要地址 (CN) */
    private const val PROJECT_PROMOTE_MAIN_CN_URL = "https://raw.githubusercontent.com/fankes/fankes/main/project-promote/README-zh-CN.md"

    /** GitHub 代理地址 1 - 直连失败后优先尝试 */
    private const val GITHUB_PROXY_1_URL = "https://cdn.gh-proxy.org"

    /** GitHub 代理地址 2 - 代理 1 失败后尝试 */
    private const val GITHUB_PROXY_2_URL = "https://ghfast.top"

    /** 每次网络请求的超时秒数 */
    private const val PROJECT_PROMOTE_TIMEOUT_SECONDS = 5L

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
        // 必须在 Markwon 设置文本前调用，它会重置 MovementMethod，之后由 Markwon 的插件换成支持链接点击的实现
        messageView.setTextIsSelectable(true)
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
        val job = activity.lifecycleScope.launch(Dispatchers.IO) {
            /**
             * 执行解析逻辑
             * @param isDone 是否成功
             * @param content 内容
             * @param baseUrl 成功加载内容的地址 - 用于解析相对路径的图片
             */
            suspend fun doParse(isDone: Boolean, content: String, baseUrl: String) {
                withContext(Dispatchers.Main) {
                    if (isDone) {
                        createMarkdown(activity, messageView, baseUrl).setMarkdown(messageView, content)
                        messageView.invalidate()
                        messageView.requestLayout()
                    } else messageView.text = activity.getString(R.string.promote_dialog_message_fail)
                    button1.isEnabled = true
                    isEnableButton1 = true
                }
                if (isDone) for (seconds in dismissSeconds downTo 0) {
                    withContext(Dispatchers.Main) {
                        val button2Text = activity.getString(R.string.promote_dialog_button_2)
                        button2.text = if (seconds > 0) "$button2Text ($seconds)" else button2Text
                        button2.isEnabled = seconds == 0
                        isEnableButton2 = seconds == 0
                    }; delay(1000.milliseconds)
                }
            }

            val mainUrl = if (isSystemLanguageSimplifiedChinese) PROJECT_PROMOTE_MAIN_CN_URL else PROJECT_PROMOTE_MAIN_EN_URL
            var isDone = false
            var content = ""
            var loadedUrl = mainUrl
            listOf(mainUrl, "$GITHUB_PROXY_1_URL/$mainUrl", "$GITHUB_PROXY_2_URL/$mainUrl").forEach { url ->
                if (isDone) return@forEach
                loadedUrl = url
                loadContentFromUrl(url) { isDone1, content1 -> isDone = isDone1; content = content1 }
                // 第一次失败后就允许关闭对话框，后续地址仍会继续尝试
                if (isDone.not()) withContext(Dispatchers.Main) {
                    button1.isEnabled = true
                    isEnableButton1 = true
                }
            }
            doParse(isDone, content, loadedUrl)
        }
        dialog.setOnDismissListener { job.cancel() }
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
            .setPositiveButton(activity.getString(R.string.promote_dialog_button_1), null)
            .setNegativeButton(activity.getString(R.string.promote_dialog_button_2), null)
            .create()

    /**
     * 创建 [Markwon]
     * @param activity 当前实例
     * @param textView 用于显示内容的 [TextView]
     * @param baseUrl 用于解析相对路径图片的基础地址
     * @return [Markwon]
     */
    private fun createMarkdown(activity: Activity, textView: TextView, baseUrl: String) = Markwon.builder(activity).apply {
        usePlugin(object : AbstractMarkwonPlugin() {

            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.imageDestinationProcessor(ImageDestinationProcessorRelativeToAbsolute.create(baseUrl))
            }

            override fun configureTheme(builder: MarkwonTheme.Builder) {
                builder.codeBlockMargin((4 * activity.resources.displayMetrics.density).toInt())
            }

            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                val radius = 8 * activity.resources.displayMetrics.density
                val factory = SpanFactory { configuration, _ -> RoundedCodeBlockSpan(configuration.theme(), radius) }
                builder.setFactory(FencedCodeBlock::class.java, factory)
                builder.setFactory(IndentedCodeBlock::class.java, factory)
                builder.setFactory(Code::class.java) { configuration, _ ->
                    RoundedCodeSpan(configuration.theme(), radius / 2, textView)
                }
            }
        })
        usePlugin(MovementMethodPlugin.create(SelectableLinkMovementMethod()))
        usePlugin(ImagesPlugin.create { it.addSchemeHandler(OkHttpNetworkSchemeHandler.create()) })
        usePlugin(HtmlPlugin.create())
    }.build()

    /**
     * 从 URL 读取内容
     * @param url 当前 URL
     * @param onResponse 回调结果 - ([Boolean] 是否成功,[String] 内容)
     */
    private suspend fun loadContentFromUrl(url: String, onResponse: suspend (isDone: Boolean, content: String) -> Unit) {
        runCatching {
            val client = OkHttpClient.Builder().callTimeout(PROJECT_PROMOTE_TIMEOUT_SECONDS, TimeUnit.SECONDS).build()
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val content = response.body.string()
            onResponse(response.isSuccessful && content.isNotBlank(), content)
        }.onFailure { onResponse(false, "") }
    }

    /**
     * 当前系统环境是否为简体中文
     * @return [Boolean]
     */
    private val isSystemLanguageSimplifiedChinese get(): Boolean {
        val locale = Locale.getDefault()
        return locale.language == "zh" && locale.country == "CN"
    }

    /**
     * 可选中文本并且链接可点击的 [ArrowKeyMovementMethod]
     *
     * 系统的 [LinkMovementMethod] 不支持选中文本，而 [ArrowKeyMovementMethod]
     * 不会响应链接点击，所以这里在短按抬起时自己查找点击位置的 [ClickableSpan]，长按仍交给系统用于选中文本
     */
    private class SelectableLinkMovementMethod : ArrowKeyMovementMethod() {

        override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP && event.eventTime - event.downTime < ViewConfiguration.getLongPressTimeout()) {
                val layout = widget.layout ?: return super.onTouchEvent(widget, buffer, event)
                val x = event.x - widget.totalPaddingLeft + widget.scrollX
                val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
                val line = layout.getLineForVertical(y)
                if (x >= layout.getLineLeft(line) && x <= layout.getLineRight(line)) {
                    val offset = layout.getOffsetForHorizontal(line, x)
                    buffer.getSpans(offset, offset, ClickableSpan::class.java).firstOrNull()?.let {
                        it.onClick(widget)
                        return true
                    }
                }
            }
            return super.onTouchEvent(widget, buffer, event)
        }
    }

    /**
     * 带圆角的代码块 `Span`
     *
     * [CodeBlockSpan] 会按行绘制矩形背景，这里只给代码块第一行的上边和最后一行的下边加圆角
     * @param theme 当前主题
     * @param radius 圆角半径 (px)
     */
    private class RoundedCodeBlockSpan(private val theme: MarkwonTheme, private val radius: Float) : CodeBlockSpan(theme) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val rect = RectF()
        private val path = Path()

        override fun drawLeadingMargin(
            c: Canvas, p: Paint, x: Int, dir: Int, top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int, first: Boolean, layout: Layout
        ) {
            paint.color = theme.getCodeBlockBackgroundColor(p)
            if (dir > 0) rect.set(x.toFloat(), top.toFloat(), layout.width.toFloat(), bottom.toFloat())
            else rect.set((x - layout.width).toFloat(), top.toFloat(), x.toFloat(), bottom.toFloat())
            val spanned = text as Spanned
            val topRadius = if (start <= spanned.getSpanStart(this)) radius else 0f
            val bottomRadius = if (end >= spanned.getSpanEnd(this)) radius else 0f
            path.rewind()
            path.addRoundRect(
                rect,
                floatArrayOf(topRadius, topRadius, topRadius, topRadius, bottomRadius, bottomRadius, bottomRadius, bottomRadius),
                Path.Direction.CW
            )
            c.drawPath(path, paint)
        }
    }

    /**
     * 带圆角的行内代码 `Span`
     *
     * 系统通过 [TextPaint.bgColor] 绘制的背景只能是矩形，这里改为在 [drawBackground] 中按行绘制圆角背景
     *
     * [drawBackground] 没有提供 [android.text.Layout]，所以从 [textView] 里取得它来计算文字位置
     * @param theme 当前主题
     * @param radius 圆角半径 (px)
     * @param textView 正在显示此 `Span` 的 [TextView]
     */
    private class RoundedCodeSpan(
        private val theme: MarkwonTheme,
        private val radius: Float,
        private val textView: TextView
    ) : MetricAffectingSpan(), LineBackgroundSpan {

        private val codePaint = Paint()
        private val rect = RectF()

        override fun updateMeasureState(p: TextPaint) = theme.applyCodeTextStyle(p)
        override fun updateDrawState(ds: TextPaint) = theme.applyCodeTextStyle(ds)

        override fun drawBackground(
            canvas: Canvas, paint: Paint, left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int, lineNumber: Int
        ) {
            val layout = textView.layout ?: return
            val spanned = text as Spanned
            // Markwon 会在行内代码两边各加一个不换行空格，它们在排版时宽度不一致，背景只包住中间的文字，两侧用相同的内边距
            val contentStart = spanned.getSpanStart(this) + 1
            val contentEnd = spanned.getSpanEnd(this) - 1
            val lineContentStart = maxOf(start, contentStart)
            val lineContentEnd = minOf(end, contentEnd)
            if (lineContentStart >= lineContentEnd) return
            val spanLeft = layout.getPrimaryHorizontal(lineContentStart) - if (lineContentStart == contentStart) radius else 0f
            val spanRight = (if (lineContentEnd == end) layout.getLineRight(lineNumber) else layout.getPrimaryHorizontal(lineContentEnd)) +
                if (lineContentEnd == contentEnd) radius else 0f
            codePaint.set(paint)
            theme.applyCodeTextStyle(codePaint)
            codePaint.color = theme.getCodeBackgroundColor(paint)
            rect.set(spanLeft, baseline + codePaint.fontMetrics.ascent, spanRight, baseline + codePaint.fontMetrics.descent)
            canvas.drawRoundRect(rect, radius, radius, codePaint)
        }
    }
}