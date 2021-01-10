package com.dede.toasty

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.View
import kotlin.math.roundToInt

object Toasty {

    const val TOAST_SHORT = 2000L
    const val TOAST_LONG = 3000L

    internal val activityLifecycleCallback = ActivityLifecycleCallback()
    internal val toastyHandler = ToastyHandler()

    internal lateinit var applicationContext: Context
        private set

    internal lateinit var viewFactory: ViewFactory
    internal lateinit var toastyStrategy: ToastyStrategy<Any>

    @JvmOverloads
    fun init(
        application: Application,
        viewFactory: ViewFactory = ToastyViewFactory(),
        toastyStrategy: ToastyStrategy<*> = DialogToastyStrategy()
    ) {
        applicationContext = application.applicationContext
        Toasty.viewFactory = viewFactory
        Toasty.toastyStrategy = toastyStrategy as ToastyStrategy<Any>
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallback)
        application.registerActivityLifecycleCallbacks(activityLifecycleCallback)
    }

    fun with(): ToastBuilder {
        return ToastBuilder()
    }

    internal fun dip(dp: Number): Int {
        val dpFloat = dp.toFloat()
        return (applicationContext.resources.displayMetrics.density * dpFloat).roundToInt()
    }

    interface ViewFactory {
        fun createView(context: Context, builder: ToastBuilder): View
    }

    interface ToastyStrategy<T> {
        fun show(activity: Activity, view: View, builder: ToastBuilder): T
        fun hide(activity: Activity, t: T)
    }
}