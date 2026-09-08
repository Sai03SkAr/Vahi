package com.vahi.app.domain.repository

/**
 * The local anonymous profile every notebook belongs to — see
 * docs/17-data-spec.md Part 7. Trimmed to exactly what `VAHI-309` needs: nothing
 * yet reads `remoteUserId` or `displayName`, since sign-in doesn't exist
 * (`VAHI-702`, v2.0). When it does, the adoption path this interface exists to
 * make possible is: look up the existing local profile by [getOrCreateLocalProfileId]
 * and set its `remoteUserId`, rather than creating a second profile or migrating
 * every notebook's `ownerProfileId`.
 */
interface ProfileRepository {

    /** Returns the single local profile's ID, creating the row on first call. */
    suspend fun getOrCreateLocalProfileId(): String
}
