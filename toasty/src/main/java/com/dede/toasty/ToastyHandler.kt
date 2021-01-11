package com.dede.toasty

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.ViewGroup
import java.util.*

internal class ToastyHandler : Handler(Looper.getMainLooper()),
    ActivityLifecycleCallback.LifecycleListener {

    companion object {
        private const val TAG = "ToastyHandler"

        private const val SHOW = 1
        private const val HIDE = 2

        // toast前的延迟, 防止toast显示后立刻关闭页面, 使toast显示两次
        internal const val DEFAULT_SHOW_DELAY = 80L

        // toast忽略重新显示的时长. 当已显示时间大于1s, 切换页面时就算没有显示完也不再重新显示
        private const val IGNORE_RESHOW_DURATION = 1000L
    }

    private val toastQueue = LinkedList<ToastyBuilder>()
    private val showingToast = LinkedList<ToastEntry>()
    private var currentAct: Activity? = null

    init {
        Toasty.activityLifecycleCallback.lifecycleCallback = this
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            SHOW -> {
                if (toastQueue.isEmpty()) {
                    return
                }
                if (isFinish(this.currentAct)) {
                    return
                }
                val toastBuilder = toastQueue.pop()
                showToastInternal(toastBuilder)
            }
            HIDE -> {
                val entry = msg.obj as ToastEntry
                hideToastInternal(entry)
            }
        }
    }

    private fun hideToastInternal(entry: ToastEntry) {
        val attachAct = entry.attachAct
        val surplus = SystemClock.uptimeMillis() - entry.showWhen
        if (surplus < IGNORE_RESHOW_DURATION && surplus < entry.builder.duration) {
            // 没有显示完，更新显示时间重新显示
            entry.builder.duration = surplus
            toastQueue.remove(entry.builder)
            toastQueue.addFirst(entry.builder)
        }

        val t = entry.toastObj
        try {
            Toasty.toastyStrategy.hide(attachAct, t)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        showingToast.remove(entry)

        showNext()
    }

    private fun showToastInternal(builder: ToastyBuilder) {
        val attachAct = this.currentAct
        if (attachAct == null) {
            builder.nativeToast().show()
            return
        }
        val view = if (builder.customView != null) {
            builder.customView!!
        } else {
            Toasty.viewFactory.createView(attachAct, builder)
        }
        val parent = view.parent
        if (parent is ViewGroup) {
            try {
                parent.removeViewInLayout(view)
            } catch (ignore: Exception) {
            }
        }

        try {
            val toastObj = Toasty.toastyStrategy.show(attachAct, view, builder)
            val toastEntry = ToastEntry(builder, toastObj, attachAct, SystemClock.uptimeMillis())
            showingToast.add(toastEntry)
            val message = Message.obtain(this, HIDE, toastEntry)
            sendMessageDelayed(message, builder.duration)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNext() {
        if (toastQueue.isEmpty()) {
            return
        }
        if (isShowing(currentAct)) {
            return
        }
        val message = Message.obtain(this, SHOW, currentAct)
        sendMessage(message)
    }

    private fun isShowing(activity: Activity?): Boolean {
        if (activity == null) {
            return false
        }
        if (hasMessages(SHOW, activity)) {
            return true
        }
        for (toastEntry in showingToast) {
            if (toastEntry.attachAct == activity) {
                return true
            }
        }
        return false
    }

    private fun isFinish(activity: Activity?): Boolean {
        if (activity == null) return true

        return activity.isFinishing || activity.isDestroyed
    }

    class ToastEntry(
        val builder: ToastyBuilder,
        val toastObj: Any,
        val attachAct: Activity,
        val showWhen: Long
    )

    fun show(builder: ToastyBuilder) {
        toastQueue.add(builder)
        if (isFinish(currentAct)) {
            return
        }
        if (isShowing(currentAct)) {
            return
        }
        val message = Message.obtain(this, SHOW, currentAct)
        sendMessageDelayed(message, builder.showDelay)
    }

    override fun onCreate(activity: Activity) {
        showWithLifecycle(activity)
    }

    override fun onStart(activity: Activity) {
        showWithLifecycle(activity)
    }

    override fun onStop(activity: Activity) {
        hideWithLifecycle(activity)
    }

    override fun onDestroy(activity: Activity) {
        hideWithLifecycle(activity)
    }

    private fun showWithLifecycle(activity: Activity) {
        currentAct = activity
        if (isShowing(activity)) {
            return
        }
        showNext()
    }

    private fun hideWithLifecycle(activity: Activity) {
        if (activity == currentAct) {
            currentAct = null
        }
        removeMessages(SHOW, activity)
        for (toastEntry in showingToast) {
            if (toastEntry.attachAct == activity) {
                removeMessages(HIDE, toastEntry)
                hideToastInternal(toastEntry)
                return
            }
        }
    }
}