package com.interactiveword.data.repository

import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.Mission

class MissionRepository {
    private val api = RetrofitClient.api

    suspend fun getDailyMissions(): List<Mission> = api.getDailyMissions()

    suspend fun getAllMissions(): List<Mission> = api.getAllMissions()

    suspend fun completeMission(missionId: Int): Mission = api.completeMission(missionId)
}
