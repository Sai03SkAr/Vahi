package com.vahi.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt's entry point (VAHI-202). Deliberately trivial — everything Hilt needs is
 * declared in `di/`; this class exists only to trigger Hilt's codegen.
 */
@HiltAndroidApp
class VahiApplication : Application()
