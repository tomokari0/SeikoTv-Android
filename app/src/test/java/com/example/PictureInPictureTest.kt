package com.example

import android.app.Activity
import com.example.presentation.player.enterPipMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PictureInPictureTest {

    @Test
    fun testEnterPipMode_nullActivity_returnsFalse() {
        assertFalse(enterPipMode(null))
    }

    @Test
    fun testEnterPipMode_withActivity_doesNotCrash() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        assertNotNull(activity)
        // Robolectric activity should invoke enterPipMode gracefully without uncaught exceptions
        enterPipMode(activity)
    }
}
