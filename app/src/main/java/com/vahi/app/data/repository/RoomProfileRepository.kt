package com.vahi.app.data.repository

import com.vahi.app.data.database.dao.ProfileDao
import com.vahi.app.domain.repository.ProfileRepository
import javax.inject.Inject

class RoomProfileRepository @Inject constructor(
    private val dao: ProfileDao
) : ProfileRepository {

    override suspend fun getOrCreateLocalProfileId(): String = dao.getOrCreateLocalProfileId()
}
