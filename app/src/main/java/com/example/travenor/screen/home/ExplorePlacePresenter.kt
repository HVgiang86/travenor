package com.example.travenor.screen.home

import com.example.travenor.constant.Food
import com.example.travenor.constant.PlaceCategory
import com.example.travenor.core.ResultListener
import com.example.travenor.data.model.photo.PlacePhoto
import com.example.travenor.data.model.place.Place
import com.example.travenor.data.place.repository.PlaceRepository
import com.example.travenor.data.user.UserInterestData
import com.example.travenor.data.user.repository.UserRepository
import java.util.Random

class ExplorePlacePresenter internal constructor(
    private val placeRepository: PlaceRepository,
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

        // Search explore for attraction with keyword
        placeRepository.searchExploreAttraction(
            searchKeyword,
            object : ResultListener<List<Place>> {
                override fun onSuccess(data: List<Place>?) {
                    if (data.isNullOrEmpty()) {
                        mView?.onGetExplorePlaceFail(
                            Exception(GET_NULL_EXCEPTION_MSG),
                            PlaceCategory.ATTRACTION
                        )
                    }

                    val locationIdList = mutableListOf<String>()
                    data!!.forEach {
                        locationIdList.add(it.locationId)
                    }

                    mView?.onGetExplorePlaceSuccess(data, PlaceCategory.ATTRACTION)

                    // Get thumbnail for each place with locationId
                    getThumbnail(locationIdList, PlaceCategory.ATTRACTION)
                }

                override fun onError(exception: Exception?) {
                    mView?.onGetExplorePlaceFail(exception, PlaceCategory.ATTRACTION)
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

        placeRepository.searchExploreRestaurant(
            searchKeyword,
            object : ResultListener<List<Place>> {

                override fun onSuccess(data: List<Place>?) {
                    if (data.isNullOrEmpty()) {
                        mView?.onGetExplorePlaceFail(
                            Exception(GET_NULL_EXCEPTION_MSG),
                            PlaceCategory.RESTAURANT
                        )
                    }
                    val locationIdList = mutableListOf<String>()
                    data!!.forEach {
                        locationIdList.add(it.locationId)
                    }
                    mView?.onGetExplorePlaceSuccess(data, PlaceCategory.RESTAURANT)

                    // Get thumbnail for each place with locationId
                    getThumbnail(locationIdList, PlaceCategory.RESTAURANT)
                }

                override fun onError(exception: Exception?) {
                    mView?.onGetExplorePlaceFail(exception, PlaceCategory.RESTAURANT)
                }
            }
        )
    }

    override fun getExploreHotel() {
        // TODO get random hotel around user's location & remove hardcode string
        placeRepository.searchExploreHotel(
            "hotel",
            object : ResultListener<List<Place>> {
                override fun onSuccess(data: List<Place>?) {
                    if (data.isNullOrEmpty()) {
                        mView?.onGetExplorePlaceFail(
                            Exception(GET_NULL_EXCEPTION_MSG),
                            PlaceCategory.HOTEL
                        )
                    }
                    val locationIdList = mutableListOf<String>()
                    data!!.forEach {
                        locationIdList.add(it.locationId)
                    }
                    mView?.onGetExplorePlaceSuccess(data, PlaceCategory.HOTEL)

                    // Get thumbnail for each place with locationId
                    getThumbnail(locationIdList, PlaceCategory.HOTEL)
                }

                override fun onError(exception: Exception?) {
                    mView?.onGetExplorePlaceFail(exception, PlaceCategory.HOTEL)
                }
            }
        )
    }

    private fun getThumbnail(placeIdList: MutableList<String>, category: PlaceCategory) {
        placeIdList.forEach {
            placeRepository.getPlacePhoto(
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

    override fun onStart() {
//        TODO("Not yet implemented")
    }

    override fun onStop() {
//        TODO("Not yet implemented")
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