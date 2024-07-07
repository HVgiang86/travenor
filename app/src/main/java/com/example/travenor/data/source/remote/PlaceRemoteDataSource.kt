package com.example.travenor.data.source.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.travenor.BuildConfig.TRIP_ADVISOR_KEY
import com.example.travenor.constant.PlaceCategory
import com.example.travenor.constant.TRIP_ADVISOR_API_KEY
import com.example.travenor.constant.TRIP_ADVISOR_URL
import com.example.travenor.core.ResultListener
import com.example.travenor.core.network.Callback
import com.example.travenor.core.network.NetWorker
import com.example.travenor.core.network.Response
import com.example.travenor.core.network.exception.NetworkException
import com.example.travenor.data.model.PlacePhotoResponse
import com.example.travenor.data.model.PlaceSearchResponse
import com.example.travenor.data.model.location.LatLng
import com.example.travenor.data.model.photo.PlacePhoto
import com.example.travenor.data.model.place.Place
import com.example.travenor.data.source.PlaceSource
import com.example.travenor.data.source.remote.api.PlaceApi
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class PlaceRemoteDataSource private constructor(
    private val placeApi: PlaceApi
) : PlaceSource.Remote {
    private val executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE)

    override fun getNearbyPlace(
        latLng: LatLng,
        category: PlaceCategory,
        radius: Double,
        listener: ResultListener<List<Place>>
    ) {
        val latLongString = "${latLng.lat},${latLng.lng}"
        val categoryName = category.name.lowercase()

        placeApi.getNearbyPlace(
            TRIP_ADVISOR_API_KEY,
            latLong = latLongString,
            radius = radius,
            category = categoryName
        ).enqueue(
            PlaceSearchResponse::class.java,
            object : Callback<PlaceSearchResponse> {
                override fun onResponse(
                    rawResponse: String,
                    response: Response<PlaceSearchResponse>
                ) {
                    val placeList = response.data.placeList

                    // Here we have some result with (location id, address, ...)
                    // but not enough detail => we need to get detail by location id
                    val idList = placeList.map { it.locationId }
                    getDetailOfPlaceList(idList, listener)
                }

                override fun onFailure(t: Throwable) {
                    t.printStackTrace()
                    Log.d(LOG_TAG, t.message.toString())
                    listener.onError(NetworkException(t.message.toString()))
                }
            }
        )
    }

    override fun searchPlace(
        query: String,
        category: PlaceCategory?,
        listener: ResultListener<List<Place>>
    ) {
        val categoryName: String = category?.name?.lowercase().toString()
        placeApi.searchPlaceTripadvisor(
            TRIP_ADVISOR_API_KEY,
            query,
            categoryName
        ).enqueue(
            PlaceSearchResponse::class.java,
            object : Callback<PlaceSearchResponse> {
                override fun onResponse(
                    rawResponse: String,
                    response: Response<PlaceSearchResponse>
                ) {
                    val placeList = response.data.placeList
                    for (place in placeList) place.locationType =
                        PlaceCategory.RESTAURANTS.name.lowercase()

                    // Here we have some result with (location id, address, ...)
                    // but not enough detail => we need to get detail by location id
                    val idList = placeList.map { it.locationId }
                    getDetailOfPlaceList(idList, listener)
                }

                override fun onFailure(t: Throwable) {
                    t.printStackTrace()
                    Log.d(LOG_TAG, t.message.toString())
                    listener.onError(NetworkException(t.message.toString()))
                }
            }
        )
    }

    override fun searchExplorePlace(
        keyword: String,
        latLng: LatLng,
        category: PlaceCategory,
        listener: ResultListener<List<Place>>
    ) {
        val categoryName = category.name.lowercase()
        val latLongString = "${latLng.lat},${latLng.lng}"

        placeApi.searchPlaceTripadvisor(
            TRIP_ADVISOR_API_KEY,
            keyword,
            categoryName,
            latLong = latLongString
        ).enqueue(
            PlaceSearchResponse::class.java,
            object : Callback<PlaceSearchResponse> {
                override fun onResponse(
                    rawResponse: String,
                    response: Response<PlaceSearchResponse>
                ) {
                    val placeList = response.data.placeList

                    for (place in placeList)
                        place.locationType = category.name.lowercase()

                    // Here we have some result with (location id, address, ...)
                    // but not enough detail => we need to get detail by location id
                    val idList = placeList.map { it.locationId }
                    getDetailOfPlaceList(idList, listener)
                }

                override fun onFailure(t: Throwable) {
                    t.printStackTrace()
                    Log.d(LOG_TAG, t.message.toString())
                    listener.onError(NetworkException(t.message.toString()))
                }
            }
        )
    }

    override fun getPlaceDetail(placeId: String, listener: ResultListener<Place>) {
        placeApi.getPlaceDetail(apiKey = TRIP_ADVISOR_API_KEY, locationId = placeId)
            .enqueue(
                Place::class.java,
                object : Callback<Place> {
                    override fun onResponse(rawResponse: String, response: Response<Place>) {
                        listener.onSuccess(response.data)
                    }

                    override fun onFailure(t: Throwable) {
                        t.printStackTrace()
                        listener.onError(NetworkException("Fail: ${t.message}"))
                    }
                }
            )
    }

    override fun getPlacePhoto(placeId: String, listener: ResultListener<List<PlacePhoto>>) {
        placeApi.getPlacePhoto(placeId, TRIP_ADVISOR_KEY, limit = 10)
            .enqueue(
                PlacePhotoResponse::class.java,
                object : Callback<PlacePhotoResponse> {
                    override fun onResponse(
                        rawResponse: String,
                        response: Response<PlacePhotoResponse>
                    ) {
                        Log.d(LOG_TAG, rawResponse)
                        val photoList = response.data.photoList
                        listener.onSuccess(photoList)
                    }

                    override fun onFailure(t: Throwable) {
                        t.printStackTrace()
                        Log.d(LOG_TAG, t.message.toString())
                        listener.onError(NetworkException(t.message.toString()))
                    }
                }
            )
    }

    private fun getDetailOfPlaceList(
        locationIdList: List<String>,
        listener: ResultListener<List<Place>>
    ) {
        val taskList = mutableListOf<Callable<Place>>()

        for (id in locationIdList) {
            val callable = Callable {
                val completableFuture = CompletableFuture<Place>()
                getPlaceDetail(
                    id,
                    object : ResultListener<Place> {
                        override fun onSuccess(data: Place?) {
                            completableFuture.complete(data)
                        }

                        override fun onError(exception: Exception?) {
                            completableFuture.complete(null)
                        }
                    }
                )
                return@Callable completableFuture.get()
            }

            taskList.add(callable)
        }

        executor.execute {
            val result = mutableListOf<Place>()
            executor.invokeAll(taskList).forEach {
                it.get()?.let { place -> result.add(place) }
            }
            Log.d(LOG_TAG, result.toString())

            Handler(Looper.getMainLooper()).post {
                // return result to UI thread
                if (result.isNotEmpty()) {

                    listener.onSuccess(result)
                } else {
                    listener.onError(Exception("No result"))
                }
            }
        }
    }

    companion object {
        private const val THREAD_POOL_SIZE = 10
        private var instance: PlaceRemoteDataSource? = null
        private const val CONNECT_TIME_OUT = 5000
        private const val READ_TIME_OUT = 5000
        const val LOG_TAG = "PLACE_RSOURCE"

        fun getInstance() = synchronized(this) {
            val placeApi = NetWorker.getInstance(
                TRIP_ADVISOR_URL,
                CONNECT_TIME_OUT,
                READ_TIME_OUT
            ).create(PlaceApi::class.java)
            instance ?: PlaceRemoteDataSource(placeApi).also { instance = it }
        }
    }
}
