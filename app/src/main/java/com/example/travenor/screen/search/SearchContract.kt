package com.example.travenor.screen.search

import com.example.travenor.constant.PlaceCategory
import com.example.travenor.data.model.photo.PlacePhoto
import com.example.travenor.data.model.place.Place
import com.example.travenor.utils.base.BasePresenter

interface SearchContract {
    interface View {
        fun onGetRecentSearchStringSuccess(data: List<String>)
        fun onSearchPlaceSuccess(data: List<Place>)
        fun onSearchPlaceFail(message: String)
        fun onGetPlacePhotoSuccess(placeId: String, data: List<PlacePhoto>)
    }

    interface Presenter : BasePresenter<View> {
        fun getRecentSearchString()
        fun onSearchPlace(searchString: String, category: PlaceCategory?)
        fun getPlaceThumbnailPhoto(placeId: String)
        fun saveRecentSearchString(searchString: List<String>)
    }
}
//Glide - using to load image
//Retrofit
