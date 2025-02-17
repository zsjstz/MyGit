package com.lind.lib_base.uibase.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.blankj.utilcode.util.LogUtils
import com.google.gson.JsonSyntaxException
import com.lind.lib_base.net.CommonResponse
import com.lind.lib_base.net.HttpNetCode
import com.lind.lib_base.net.process
import com.lind.lib_base.uibase.inner.IBaseViewModel
import com.lind.lib_base.uibase.inner.INetView
import com.lind.lib_base.utils.UICoreConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.EOFException
import java.net.SocketTimeoutException
import java.text.ParseException
import javax.net.ssl.SSLException

abstract class BaseViewModel : ViewModel(), IBaseViewModel, INetView {

    val showToastStrEvent = MutableLiveData<String?>()
    val showLongToastStrEvent = MutableLiveData<String?>()
    val showToastResEvent = MutableLiveData<Int>()
    val showLongToastResEvent = MutableLiveData<Int>()
    val showCenterToastStrEvent = MutableLiveData<String?>()
    val showCenterLongToastStrEvent = MutableLiveData<String?>()
    val showCenterToastResEvent = MutableLiveData<Int>()
    val showCenterLongToastResEvent = MutableLiveData<Int>()
    val finishAcEvent = MutableLiveData<String>()
    var isViewDestroyed = false

    override fun onAny(owner: LifecycleOwner, event: Lifecycle.Event) {
    }

    override fun onCreate() {
    }

    override fun onStart() {
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onStop() {
    }

    override fun onDestroy() {
        isViewDestroyed = true
    }



    override fun showToast(msg: String?) {
        showToastStrEvent.value = msg
    }

    override fun showLongToast(msg: String?) {
        showLongToastStrEvent.value = msg
    }

    override fun showToast(@StringRes resId: Int) {
        showToastResEvent.value = resId
    }

    override fun showLongToast(resId: Int) {
        showLongToastResEvent.value = resId
    }

    override fun showCenterToast(msg: String?) {
        showCenterToastStrEvent.value = msg
    }

    override fun showCenterLongToast(msg: String?) {
        showCenterLongToastStrEvent.value = msg
    }

    override fun showCenterToast(resId: Int) {
        showCenterToastResEvent.value = resId
    }

    override fun showCenterLongToast(resId: Int) {
        showCenterLongToastResEvent.value = resId
    }

    override fun finishAc() {
        finishAcEvent.value = ""
    }

    /**
     * 主线程回调
     */
    override fun launch(block: suspend () -> Unit, failed: suspend (Int, String?) -> Unit): Job {
        val job = viewModelScope.launch(Dispatchers.Main) {
            try {
                block()
            } catch (t: Throwable) {
                onFailSuspend(t, failed)
            }
        }
        return job
    }

    /**
     * 主线程回调
     */
    override fun <T> netLaunch(
        block: suspend () -> CommonResponse<T>,
        success: (msg: String?, d: T?) -> Unit,
        failed: (Int, String?, d: T?) -> Unit
    ): Job {
        val job = viewModelScope.launch(Dispatchers.Main) {
            try {
                val response = block()
                response.process(success, failed)
            } catch (t: Throwable) {
                onFailException(t, failed)
            }
        }
        return job
    }

    /**
     * 子线程回调
     */
    override fun ioLaunch(block: suspend () -> Unit, failed: suspend (Int, String?) -> Unit): Job {
        val job = viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (t: Throwable) {
                onFailSuspend(t, failed)
            }
        }
        return job
    }

    /**
     * 子线程回调
     */
    override fun <T> ioNetLaunch(
        block: suspend () -> CommonResponse<T>,
        success: (msg: String?, d: T?) -> Unit,
        failed: (Int, String?, d: T?) -> Unit
    ): Job {
        val job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = block()
                response.process(success, failed)
            } catch (t: Throwable) {
                onFailException(t, failed)
            }
        }
        return job
    }

    private suspend fun onFailSuspend(t: Throwable, failed: suspend (Int, String?) -> Unit) {
        val loginExpired = t.message?.contains("HTTP 401") ?: false
        if (!loginExpired) {
            LogUtils.e(t)
            when (t) {
                is EOFException -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "网络读取异常：${t.message}")
                    } else {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "网络读取异常")
                    }
                }
                is SocketTimeoutException -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.NET_TIMEOUT, "网络超时：${t.message}")
                    } else {
                        failed(HttpNetCode.NET_TIMEOUT, "网络超时")
                    }
                }
                is SSLException -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "SSL校验未通过：${t.message}")
                    } else {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "SSL校验未通过")
                    }
                }
                is ParseException -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.PARSE_ERROR, "Parse解析异常：${t.message}")
                    } else {
                        failed(HttpNetCode.PARSE_ERROR, "Parse解析异常")
                    }
                }
                is JsonSyntaxException -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.JSON_ERROR, "Json解析异常：${t.message}")
                    } else {
                        failed(HttpNetCode.JSON_ERROR, "Json解析异常")
                    }
                }
                else -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "网络繁忙：${t.message}")
                    } else {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "网络繁忙")
                    }
                }
            }
        }
    }

    private fun <T> onFailException(t: Throwable, failed: (Int, String?, d: T?) -> Unit) {
        val loginExpired = t.message?.contains("HTTP 401") ?: false
        if (!loginExpired) {
            LogUtils.e(t)
            when (t) {
                is EOFException -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "网络读取异常：${t.message}", null)
                    } else {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "网络读取异常", null)
                    }
                }
                is SocketTimeoutException -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.NET_TIMEOUT, "网络超时：${t.message}", null)
                    } else {
                        failed(HttpNetCode.NET_TIMEOUT, "网络超时", null)
                    }
                }
                is SSLException -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "SSL校验未通过：${t.message}", null)
                    } else {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "SSL校验未通过", null)
                    }
                }
                is ParseException -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.PARSE_ERROR, "Parse解析异常：${t.message}", null)
                    } else {
                        failed(HttpNetCode.PARSE_ERROR, "Parse解析异常", null)
                    }
                }
                is JsonSyntaxException -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.JSON_ERROR, "Json解析异常：${t.message}", null)
                    } else {
                        failed(HttpNetCode.JSON_ERROR, "Json解析异常", null)
                    }
                }
                else -> {
                    if (UICoreConfig.mode) {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "网络繁忙：${t.message}", null)
                    } else {
                        failed(HttpNetCode.NET_CONNECT_ERROR, "网络繁忙", null)
                    }
                }
            }
        }
    }
}