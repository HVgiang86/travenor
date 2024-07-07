package com.example.travenor.data.source.remote.api

import com.example.travenor.constant.DEFAULT_LAT
import com.example.travenor.constant.DEFAULT_LONG
import com.example.travenor.constant.NEARBY_DISTANCE_IN_METERS
import com.example.travenor.core.network.Call
import com.example.travenor.core.network.annotation.GET
import com.example.travenor.core.network.annotation.Path
import com.example.travenor.core.network.annotation.Query
import com.example.travenor.data.model.PlacePhotoResponse
import com.example.travenor.data.model.PlaceSearchResponse
import com.example.travenor.data.model.place.Place

interface PlaceApi {
    @GET("/location/search")
    fun searchPlaceTripadvisor(
        @Query("key") apiKey: String,
        @Query("searchQuery") searchQuery: String,
        @Query("category") category: String = "attraction",
        @Query("latLong") latLong: String = "$DEFAULT_LAT,$DEFAULT_LONG",
        @Query("language") language: String = "vi"
    ): Call<PlaceSearchResponse>

    @GET("/location/{locationId}/photos")
    fun getPlacePhoto(
        @Path("locationId") locationId: String,
        @Query("key") apiKey: String,
        @Query("limit") limit: Int = 1,
        @Query("language") language: String = "vi"
    ): Call<PlacePhotoResponse>

    @GET("/location/{locationId}/details")
    fun getPlaceDetail(
        @Path("locationId") locationId: String,
        @Query("key") apiKey: String,
        @Query("language") language: String = "vi",
        @Query("currency") currency: String = "VND"
    ): Call<Place>

    @Suppress("LongParameterList")
    @GET("/location/nearby_search")
    fun getNearbyPlace(
        @Query("key") apiKey: String,
        @Query("latLong") latLong: String = "$DEFAULT_LAT,$DEFAULT_LONG",
        @Query("radius") radius: Double = NEARBY_DISTANCE_IN_METERS,
        @Query("radiusUnit") radiusUnit: String = "m",
        @Query("category") category: String = "attraction",
        @Query("language") language: String = "vi"
    ): Call<PlaceSearchResponse>
}

