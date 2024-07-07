package com.example.travenor.screen.home

import com.example.travenor.constant.DEFAULT_LAT
import com.example.travenor.constant.DEFAULT_LONG
import com.example.travenor.constant.Food
import com.example.travenor.constant.PlaceCategory
import com.example.travenor.core.ResultListener
import com.example.travenor.data.UserInterestData
import com.example.travenor.data.model.location.LatLng
import com.example.travenor.data.model.photo.PlacePhoto
import com.example.travenor.data.model.place.Place
import com.example.travenor.data.repository.PlaceRepository
import com.example.travenor.data.repository.UserRepository
import com.example.travenor.utils.location.LocationUtils
import java.util.Random

@Suppress("TooManyFunctions")
class ExplorePlacePresenter internal constructor(
    private val placeRepositoryImpl: PlaceRepository,
    private val userRepository: UserRepository
) : ExplorePlaceContract.Presenter {
    private var mView: ExplorePlaceContract.View? = null

    override fun getExploreAttraction() {
        var searchKeyword = ""

        UserInterestData.interestedPlaceList.forEach {
            searchKeyword += " ${it.name}"
        }

        // If user not input interested place type then random it!
        if (searchKeyword.isEmpty()) {
            searchKeyword = randomPlaceKeyword()
        }

        // Get user's last location
        val lastLocation = userRepository.getUserLastLocation()

        val lastLat = if (lastLocation.first != 0.0) lastLocation.first else DEFAULT_LAT
        val lastLng = if (lastLocation.second != 0.0) lastLocation.second else DEFAULT_LONG

        val randomLocation = LocationUtils.generateRandomLocation(lastLat, lastLng)
        val randomLat = randomLocation.first
        val randomLng = randomLocation.second

        // Search explore for attraction with keyword
        placeRepositoryImpl.searchExplorePlace(
            searchKeyword,
            PlaceCategory.ATTRACTIONS,
            LatLng(randomLat, randomLng),
            object : ResultListener<List<Place>> {
                override fun onSuccess(data: List<Place>?) {
                    if (data.isNullOrEmpty()) {
                        mView?.onGetExplorePlaceFail(
                            Exception(GET_NULL_EXCEPTION_MSG),
                            PlaceCategory.ATTRACTIONS
                        )
                    }

                    val locationIdList = mutableListOf<String>()

                    if (data != null) {
                        data.forEach {
                            locationIdList.add(it.locationId)
                        }

                        mView?.onGetExplorePlaceSuccess(data, PlaceCategory.ATTRACTIONS)

                        // Get thumbnail for each place with locationId
                        getThumbnail(locationIdList, PlaceCategory.ATTRACTIONS)
                    }
                }

                override fun onError(exception: Exception?) {
                    mView?.onGetExplorePlaceFail(exception, PlaceCategory.ATTRACTIONS)
                }
            }
        )
    }

    override fun getExploreRestaurant() {
        var searchKeyword = ""

        UserInterestData.interestFoodList.forEach {
            searchKeyword += " ${it.name}"
        }

        // If user not input interested food type then random it!
        if (searchKeyword.isEmpty()) {
            searchKeyword = randomFoodKeyword()
        }

        // Get user's last location
        val lastLocation = userRepository.getUserLastLocation()

        val lastLat = if (lastLocation.first != 0.0) lastLocation.first else DEFAULT_LAT
        val lastLng = if (lastLocation.second != 0.0) lastLocation.second else DEFAULT_LONG

        val randomLocation = LocationUtils.generateRandomLocation(lastLat, lastLng)
        val randomLat = randomLocation.first
        val randomLng = randomLocation.second

        placeRepositoryImpl.searchExplorePlace(
            searchKeyword,
            PlaceCategory.RESTAURANTS,
            LatLng(randomLat, randomLng),
            object : ResultListener<List<Place>> {

                override fun onSuccess(data: List<Place>?) {
                    if (data.isNullOrEmpty()) {
                        mView?.onGetExplorePlaceFail(
                            Exception(GET_NULL_EXCEPTION_MSG),
                            PlaceCategory.RESTAURANTS
                        )
                    }
                    val locationIdList = mutableListOf<String>()
                    if (data != null) {
                        data.forEach {
                            locationIdList.add(it.locationId)
                        }

                        data.let { mView?.onGetExplorePlaceSuccess(it, PlaceCategory.RESTAURANTS) }

                        // Get thumbnail for each place with locationId
                        getThumbnail(locationIdList, PlaceCategory.RESTAURANTS)
                    }
                }

                override fun onError(exception: Exception?) {
                    mView?.onGetExplorePlaceFail(exception, PlaceCategory.RESTAURANTS)
                }
            }
        )
    }

    override fun getExploreHotel() {
        // Get user's last location
        val lastLocation = userRepository.getUserLastLocation()

        val lastLat = if (lastLocation.first != 0.0) lastLocation.first else DEFAULT_LAT
        val lastLng = if (lastLocation.second != 0.0) lastLocation.second else DEFAULT_LONG

        val randomLocation = LocationUtils.generateRandomLocation(lastLat, lastLng)
        val randomLat = randomLocation.first
        val randomLng = randomLocation.second

        placeRepositoryImpl.searchExplorePlace(
            "hotel",
            PlaceCategory.HOTELS,
            LatLng(randomLat, randomLng),
            object : ResultListener<List<Place>> {
                override fun onSuccess(data: List<Place>?) {

                    if (data.isNullOrEmpty()) {
                        mView?.onGetExplorePlaceFail(
                            Exception(GET_NULL_EXCEPTION_MSG),
                            PlaceCategory.HOTELS
                        )
                    }
                    val locationIdList = mutableListOf<String>()
                    if (data != null) {
                        data.forEach {
                            locationIdList.add(it.locationId)
                        }

                        mView?.onGetExplorePlaceSuccess(data, PlaceCategory.HOTELS)

                        // Get thumbnail for each place with locationId
                        getThumbnail(locationIdList, PlaceCategory.HOTELS)
                    }
                }

                override fun onError(exception: Exception?) {
                    mView?.onGetExplorePlaceFail(exception, PlaceCategory.HOTELS)
                }
            }
        )
    }

    private fun getThumbnail(placeIdList: MutableList<String>, category: PlaceCategory) {
        placeIdList.forEach {
            placeRepositoryImpl.getPlacePhoto(
                it,
                object : ResultListener<List<PlacePhoto>> {
                    override fun onSuccess(data: List<PlacePhoto>?) {
                        if (data.isNullOrEmpty()) return
                        val photo = data.first()
                        photo.locationId = it

                        mView?.onGetPhotoSuccess(photo, category)
                    }

                    override fun onError(exception: Exception?) {
                        //
                    }
                }
            )
        }
    }

    override fun getUserInterest() {
        if (!UserInterestData.isFirstQueryUserInterest) {
            mView?.onGetUserInterestPlaceDone()
            mView?.onGetUserInterestFoodDone()
            return
        }

        UserInterestData.isFirstQueryUserInterest = false

        // Get user interest place saved on sharedPrefs
        userRepository.getUserInterestedPlace(object :
                ResultListener<List<com.example.travenor.constant.Place>> {
                override fun onSuccess(data: List<com.example.travenor.constant.Place>?) {
                    data?.let { UserInterestData.interestedPlaceList.addAll(it) }
                    mView?.onGetUserInterestPlaceDone()
                }

                override fun onError(exception: Exception?) {
                    // Done with no data, then we'll generate random value for interest
                    mView?.onGetUserInterestPlaceDone()
                }
            })

        // Get user interest food saved on sharedPrefs
        userRepository.getUserInterestedFood(object : ResultListener<List<Food>> {
            override fun onSuccess(data: List<Food>?) {
                data?.let { UserInterestData.interestFoodList.addAll(data) }
                mView?.onGetUserInterestFoodDone()
            }

            override fun onError(exception: Exception?) {
                // Done with no data, then we'll generate random value for interest
                mView?.onGetUserInterestFoodDone()
            }
        })
    }

    override fun markFavorite(placeId: String) {
        placeRepositoryImpl.markFavorite(
            placeId,
            object : ResultListener<Boolean> {
                override fun onSuccess(data: Boolean?) { /* no-op *//* view was updated before*/
                }

                override fun onError(exception: Exception?) {
                    exception?.printStackTrace()
                }
            }
        )
    }

    override fun markNotFavorite(placeId: String) {
        placeRepositoryImpl.markNotFavorite(
            placeId,
            object : ResultListener<Boolean> {
                override fun onSuccess(data: Boolean?) { /* no-op *//* view was updated before*/
                }

                override fun onError(exception: Exception?) {
                    exception?.printStackTrace()
                }
            }
        )
    }

    override fun setView(view: ExplorePlaceContract.View?) {
        this.mView = view
    }

    private fun randomPlaceKeyword(): String {
        val stringSeeds = listOf(
            com.example.travenor.constant.Place.MOUNTAIN,
            com.example.travenor.constant.Place.BEACH,
            com.example.travenor.constant.Place.CAVE
        )

        // random index in range from 0 -> string seeds size - 1
        val randomIndex = Random().nextInt() % stringSeeds.size

        return stringSeeds[randomIndex].name
    }

    private fun randomFoodKeyword(): String {
        val stringSeeds = listOf(
            Food.ASIAN_FOOD,
            Food.EUROPEAN_FOOD,
            Food.FAST_FOOD,
            Food.SEA_FOOD
        )

        // random index in range from 0 -> string seeds size - 1
        val randomIndex = Random().nextInt() % stringSeeds.size
        return stringSeeds[randomIndex].name
    }

    companion object {
        const val GET_NULL_EXCEPTION_MSG = "get_null_list"
    }
}
