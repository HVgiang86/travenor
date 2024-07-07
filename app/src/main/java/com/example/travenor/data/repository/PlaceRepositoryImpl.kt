package com.example.travenor.data.repository

import com.example.travenor.constant.IS_FAVORITE
import com.example.travenor.constant.IS_NOT_FAVORITE
import com.example.travenor.constant.MAX_EXPLORE_RESULTS
import com.example.travenor.constant.PlaceCategory
import com.example.travenor.core.ResultListener
import com.example.travenor.core.observable.FavoritePlaceObservable
import com.example.travenor.core.observable.FavoritePlaceObserver
import com.example.travenor.data.model.location.LatLng
import com.example.travenor.data.model.photo.PlacePhoto
import com.example.travenor.data.model.place.Place
import com.example.travenor.data.source.PlaceSource

@Suppress("TooManyFunctions")
class PlaceRepositoryImpl private constructor(
    private val remote: PlaceSource.Remote,
    private val local: PlaceSource.Local,
    private val localExplore: PlaceSource.ExplorePlaceLocal
) : PlaceRepository, FavoritePlaceObservable {
    private val favoritePlaceObserverList = mutableListOf<FavoritePlaceObserver>()

    override fun searchExplorePlace(
        keyword: String,
        category: PlaceCategory,
        latLng: LatLng,
        listener: ResultListener<List<Place>>
    ) {
        // Try get online mode
        // Get local data and check freshness
        val result = localExplore.getExplorePlaceLocal(limit = MAX_EXPLORE_RESULTS, category)

        // No data stored, so get remote
        if (result.isEmpty()) {
            getExplorePlaceFromRemote(keyword, latLng, category, listener)
            return
        }

        val samplePair = result[0]
        val timeStamp = samplePair.second

        // Data is fresh, so return local data
        if (isDataExploreFresh(timeStamp)) {
            listener.onSuccess(
                result.map {
                    it.first
                }
            )
        } else {
            // Data is too old, so get remote
            // Get remote data from api and store to local
            getExplorePlaceFromRemote(keyword, latLng, category, listener)
        }
    }

    override fun getRecentSearch(listener: ResultListener<List<String>>) {
        local.getRecentSearchPlaces(listener)
    }

    override fun saveRecentSearch(keyword: List<String>) {
        local.saveRecentSearchPlaces(keyword)
    }

    override fun searchPlace(
        keyword: String,
        category: PlaceCategory?,
        listener: ResultListener<List<Place>>
    ) {
        // try online
        remote.searchPlace(
            keyword,
            category,
            object : ResultListener<List<Place>> {
                override fun onSuccess(data: List<Place>?) {
                    // No data
                    if (data.isNullOrEmpty()) {
                        listener.onError(Exception("No data!"))
                        return
                    }
                    listener.onSuccess(data)

                    // Save to local DB
                    data.forEach {
                        it.locationType = category?.name.toString()
                        local.savePlaceDetail(it)
                        local.savePlaceAddress(it)
                    }
                }

                override fun onError(exception: Exception?) {
                    // get Offline
                    local.searchPlace(keyword, category, listener)
                }
            }
        )
    }

    /**
     * Data explore fresh if it has been created within the previous 2 days ago
     */
    private fun isDataExploreFresh(timeStamp: Long): Boolean {
        // Calculate the current time minus the time the data was last updated
        val timeSinceLastUpdate = System.currentTimeMillis() - timeStamp

        // Return true if the time since the last update is less than the freshness threshold
        val result = timeSinceLastUpdate < DATA_FRESHNESS_THRESH_HOLD
        return result
    }

    override fun getPlaceDetail(placeId: String, listener: ResultListener<Place>) {
        // Online mode
        remote.getPlaceDetail(
            placeId,
            object : ResultListener<Place> {
                override fun onSuccess(data: Place?) {
                    if (data == null) {
                        local.getPlaceDetail(placeId).apply {
                            if (this == null) {
                                listener.onError(Exception("No data!"))
                            } else {
                                listener.onSuccess(this)
                            }
                        }
                    } else {
                        listener.onSuccess(data)
                        local.savePlaceDetail(data)
                    }
                }

                override fun onError(exception: Exception?) {
                    exception?.printStackTrace()
                    local.getPlaceDetail(placeId).apply {
                        if (this == null) {
                            listener.onError(Exception("No data!"))
                        } else {
                            listener.onSuccess(this)
                        }
                    }
                }
            }
        )
    }

    override fun getPlacePhoto(placeId: String, listener: ResultListener<List<PlacePhoto>>) {
        remote.getPlacePhoto(
            placeId,
            object : ResultListener<List<PlacePhoto>> {
                override fun onSuccess(data: List<PlacePhoto>?) {
                    if (data.isNullOrEmpty()) {
                        listener.onError(Exception("No data!"))
                        return
                    }
                    data.forEach {
                        it.locationId = placeId
                    }
                    listener.onSuccess(data)

                    // Save to local DB
                    local.savePlacePhoto(data)
                }

                override fun onError(exception: Exception?) {
                    exception?.printStackTrace()
                    local.getPlacePhoto(placeId).apply {
                        if (this.isNullOrEmpty()) {
                            listener.onError(Exception("No data!"))
                        } else {
                            listener.onSuccess(this)
                        }
                    }
                }
            }
        )
    }

    override fun markFavorite(placeId: String, listener: ResultListener<Boolean>) {
        local.markFavorite(placeId, listener)
        notifyChanged(placeId, IS_FAVORITE)
    }

    override fun markNotFavorite(placeId: String, listener: ResultListener<Boolean>) {
        local.markNotFavorite(placeId, listener)
        notifyChanged(placeId, IS_NOT_FAVORITE)
    }

    override fun getFavoritePlace(listener: ResultListener<List<Place>>) {
        local.getFavoritePlace(listener)
    }

    override fun registerObserver(favoritePlaceObserver: FavoritePlaceObserver?) {
        if (favoritePlaceObserverList.contains(favoritePlaceObserver)) {
            return
        }
        if (favoritePlaceObserver == null) return

        favoritePlaceObserverList.add(favoritePlaceObserver)
    }

    override fun removeObserver(favoritePlaceObserver: FavoritePlaceObserver?) {
        favoritePlaceObserverList.remove(favoritePlaceObserver)
    }

    override fun notifyChanged(placeId: String, isFavorite: Int) {
        favoritePlaceObserverList.forEach { observer ->
            observer.onFavoritePlaceChange(placeId, isFavorite)
        }
    }

    private fun getExplorePlaceFromRemote(
        keyword: String,
        latLng: LatLng,
        category: PlaceCategory,
        listener: ResultListener<List<Place>>
    ) {
        remote.searchExplorePlace(
            keyword,
            latLng,
            category,
            object : ResultListener<List<Place>> {
                override fun onSuccess(data: List<Place>?) {
                    if (data.isNullOrEmpty()) {
                        listener.onError(Exception("No data!"))
                        return
                    }

                    // Save to local DB
                    localExplore.saveExploreAttractionLocal(data.map { it.locationId })
                    data.forEach {
                        it.locationType = category.name.lowercase()
                        local.savePlaceDetail(it)
                        local.savePlaceAddress(it)
                    }

                    listener.onSuccess(data)
                }

                override fun onError(exception: Exception?) {
                    listener.onError(exception)
                }
            }
        )
    }

    companion object {
        private const val DATA_FRESHNESS_THRESH_HOLD = 2 * 24 * 60 * 60 * 1000 // 2 days
        private var instance: PlaceRepositoryImpl? = null

        fun getInstance(
            remote: PlaceSource.Remote,
            local: PlaceSource.Local,
            localExplore: PlaceSource.ExplorePlaceLocal
        ) = synchronized(this) {
            instance ?: PlaceRepositoryImpl(
                remote,
                local,
                localExplore
            ).also { instance = it }
        }
    }
}
