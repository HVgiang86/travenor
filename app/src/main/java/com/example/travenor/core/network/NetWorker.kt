package com.example.travenor.core.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.travenor.core.network.annotation.GET
import com.example.travenor.core.network.annotation.POST
import com.example.travenor.core.network.annotation.PUT
import com.example.travenor.core.network.annotation.Path
import com.example.travenor.core.network.annotation.Query
import com.example.travenor.core.network.exception.NetworkException
import com.google.gson.Gson
import org.json.JSONException
import java.io.BufferedReader
import java.io.IOException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.net.HttpRetryException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@Suppress("UNCHECKED_CAST")
class NetWorker private constructor(
    private val baseUrl: String,
    private val mConnectTimeout: Int,
    private val mReadTimeout: Int
) {
    private val executeService = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE)
    private val uiHandler = Handler(Looper.getMainLooper())

    /**
     * We create an ApiService instance with Java Reflection!
     */
    fun <T> create(service: Class<T>): T {
        return Proxy.newProxyInstance(
            service.classLoader,
            arrayOf(service)
        ) { _, method, args ->
            var needArgs = false
            if (method.parameterAnnotations.isNotEmpty()) needArgs = true

            // Check if needed args are passed into api
            if (needArgs && args == null) require(true)

            // Handle HTTP Method
            val putAnnotation = method.getAnnotation(PUT::class.java)
            val postAnnotation = method.getAnnotation(POST::class.java)
            val getAnnotation = method.getAnnotation(GET::class.java)

            when {
                getAnnotation != null -> {
                    // Handle get method
                    return@newProxyInstance performGetRequest<T>(
                        getAnnotation,
                        baseUrl,
                        method,
                        args
                    )
                }

                putAnnotation != null -> {
                    // handle put method
                }

                postAnnotation != null -> {
                    // handle post method
                }

                else -> null
            }
        } as T
    }

    private fun <T> performGetRequest(
        annotation: GET,
        baseUrl: String,
        method: Method,
        args: Array<Any?>?
    ): Call<T> {
        // initial Url
        var url = "$baseUrl${annotation.value}"

        // get Path params
        val paramAnnotations = method.parameterAnnotations

        // Handle @Path param annotation
        url = handlePathAnnotation(paramAnnotations, url, args)

        // Handle @Query param annotation
        url = handleQueryAnnotation(paramAnnotations, url, args)

        Log.d(LOG_TAG, "Finish endpoint Url METHOD: $url")
        val apiUrl = URL(url)

        // Return a Call
        return object : Call<T> {
            override fun enqueue(parseType: Type, callback: Callback<T>) {
                val callable = Callable {
                    Log.d(LOG_TAG, "Network request")
                    try {
                        val connection = (apiUrl.openConnection() as HttpURLConnection).apply {
                            useCaches = true // Enable or ensure usage of the cache
                            connectTimeout = mConnectTimeout // Optional: 15 seconds timeout
                            readTimeout = mReadTimeout // Optional
                        }

                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val inputStream = connection.inputStream
                            val responseBody =
                                inputStream.bufferedReader().use(BufferedReader::readText)

                            val data = Gson().fromJson(responseBody, parseType) as T
                            uiHandler.post {
                                callback.onResponse(responseBody, Response(data))
                            }
                        } else {
                            // Throw ApiException for non-OK HTTP response codes
                            val exception = NetworkException(
                                "HTTP error: " + "$responseCode when try get: $apiUrl\n" + connection.responseMessage
                            )
                            uiHandler.post {
                                callback.onFailure(exception)
                            }
                        }
                    } catch (e: IOException) {
                        // Catch any other exceptions and invoke onFailure callback
                        val exception = NetworkException(e.message.toString())
                        uiHandler.post {
                            callback.onFailure(exception)
                        }
                    } catch (e: JSONException) {
                        val exception = NetworkException(e.message.toString())
                        uiHandler.post {
                            callback.onFailure(exception)
                        }
                    } catch (e: HttpRetryException) {
                        val exception = NetworkException(e.message.toString())
                        uiHandler.post {
                            callback.onFailure(exception)
                        }
                    }
                }

                executeService.submit(callable)
            }
        }
    }

    private fun handlePathAnnotation(
        paramAnnotations: Array<Array<Annotation>>,
        url: String,
        args: Array<Any?>?
    ): String {
        // Handle @Path param annotation
        var modifiedUrl = url
        paramAnnotations.forEachIndexed { index, annotations ->
            if (args != null) {
                annotations.forEach { annotation ->
                    if (annotation is Path) {
                        val name = annotation.value
                        modifiedUrl = modifiedUrl.replace("{$name}", args[index].toString())
                    }
                }
            }
        }
        return modifiedUrl
    }

    private fun handleQueryAnnotation(
        paramAnnotations: Array<Array<Annotation>>,
        urlIn: String,
        args: Array<Any?>?
    ): String {
        var url = urlIn
        val queryParams = mutableListOf<String>()
        paramAnnotations.forEachIndexed { index, annotations ->
            if (args != null) {
                annotations.forEach { annotation ->
                    if (annotation is Query) {
                        val name = annotation.value
                        val value = args[index]
                        queryParams.add("$name=$value")
                    }
                }
            }
        }
        if (queryParams.isNotEmpty()) {
            val queryString = queryParams.joinToString("&", "?")
            url = "$url$queryString"
        }
        return url
    }

    companion object {
        const val GET_METHOD = "GET"
        const val LOG_TAG = "NET"
        const val MAX_THREAD_POOL_SIZE = 5

        @Volatile
        private var instance: NetWorker? = null
        fun getInstance(baseUrl: String, connectTimeout: Int, readTimeout: Int): NetWorker =
            instance ?: synchronized(this) {
                instance ?: NetWorker(baseUrl, connectTimeout, readTimeout).also { instance = it }
            }
    }
}
