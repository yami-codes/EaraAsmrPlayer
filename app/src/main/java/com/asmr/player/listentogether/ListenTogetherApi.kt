package com.asmr.player.listentogether

import retrofit2.http.Body
import retrofit2.http.POST

interface ListenTogetherApi {
    @POST("presence/upsert")
    suspend fun upsertPresence(@Body payload: ListenTogetherPresencePayload): ListenTogetherPresenceResponse

    @POST("presence/leave")
    suspend fun leave(@Body payload: ListenTogetherLeavePayload)
}
