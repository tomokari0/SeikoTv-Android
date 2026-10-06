package com.example

import com.example.domain.model.Content
import com.example.domain.model.Episode
import com.example.presentation.player.PlayerViewModel
import com.example.presentation.player.SubtitleManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioSubtitleTracksTest {

    private val testContent = Content(
        id = "masacre_escolar",
        title = "Masacre Escolar: El Comienzo",
        type = "series"
    )

    private val testEpisode = Episode(
        id = "me_ep1",
        episodeNumber = 1,
        title = "MI NAVIDAD JUNTO A TI"
    )

    @Test
    fun testSubtitleManager_none_returnsNull() {
        val cue = SubtitleManager.getSubtitle(
            positionMs = 15000L,
            subtitleId = "none",
            content = testContent,
            episode = testEpisode
        )
        assertNull("Subtitles should be disabled and return null", cue)
    }

    @Test
    fun testSubtitleManager_esAuto_returnsClosedCaptionsWithDescriptions() {
        val cue = SubtitleManager.getSubtitle(
            positionMs = 14000L,
            subtitleId = "es_auto",
            content = testContent,
            episode = testEpisode
        )
        assertNotNull("Should return subtitle cue", cue)
        assertTrue("Should contain character tag or CC description", cue!!.contains("[Itsuki]") || cue.contains("Isaac"))
    }

    @Test
    fun testSubtitleManager_esOrig_returnsCleanDialogue() {
        val cue = SubtitleManager.getSubtitle(
            positionMs = 14000L,
            subtitleId = "es_orig",
            content = testContent,
            episode = testEpisode
        )
        assertNotNull("Should return subtitle cue", cue)
        assertTrue("Should contain Spanish dialogue without CC tag", cue!!.contains("Isaac") && !cue.contains("[Itsuki]"))
    }

    @Test
    fun testSubtitleManager_enTrans_returnsEnglishDialogue() {
        val cue = SubtitleManager.getSubtitle(
            positionMs = 14000L,
            subtitleId = "en_trans",
            content = testContent,
            episode = testEpisode
        )
        assertNotNull("Should return English subtitle cue", cue)
        assertTrue("Should contain English words", cue!!.contains("meeting place", ignoreCase = true) || cue.contains("Isaac"))
    }

    @Test
    fun testSubtitleManager_jaTrans_returnsJapaneseDialogue() {
        val cue = SubtitleManager.getSubtitle(
            positionMs = 14000L,
            subtitleId = "ja_trans",
            content = testContent,
            episode = testEpisode
        )
        assertNotNull("Should return Japanese subtitle cue", cue)
        assertTrue("Should contain Japanese characters", cue!!.contains("アイザック") || cue.contains("イツキ"))
    }

    @Test
    fun testSubtitleManager_nativeExoCue_takesPrecedence() {
        val nativeText = "Texto de subtítulo nativo WebVTT desde el archivo"
        val cue = SubtitleManager.getSubtitle(
            positionMs = 14000L,
            subtitleId = "es_auto",
            content = testContent,
            episode = testEpisode,
            exoCueText = nativeText
        )
        assertEquals(nativeText, cue)
    }

    @Test
    fun testPlayerViewModel_selectSubtitle_updatesUiStateAndSubtitleText() {
        val viewModel = PlayerViewModel(null)
        viewModel.initPlayback(testContent, testEpisode)
        viewModel.updatePosition(14000L, 60000L)

        // Initially subtitles are none
        assertNull(viewModel.uiState.value.currentSubtitleText)
        assertEquals("none", viewModel.uiState.value.selectedSubtitleId)

        // Select Spanish Auto CC
        viewModel.selectSubtitle("es_auto")
        assertEquals("es_auto", viewModel.uiState.value.selectedSubtitleId)
        assertNotNull(viewModel.uiState.value.currentSubtitleText)

        // Disable subtitles
        viewModel.selectSubtitle("none")
        assertEquals("none", viewModel.uiState.value.selectedSubtitleId)
        assertNull(viewModel.uiState.value.currentSubtitleText)
    }

    @Test
    fun testPlayerViewModel_selectAudioTrack_updatesUiStateAndShowsToast() {
        val viewModel = PlayerViewModel(null)
        viewModel.initPlayback(testContent, testEpisode)

        assertEquals("ja", viewModel.uiState.value.selectedAudioId)
        assertNull(viewModel.uiState.value.audioFeedbackToast)

        // Select Spanish Latino dubbing
        val latinoLabel = "🇲🇽 Español Latino (Doblaje)"
        viewModel.selectAudioTrack("es_lat", latinoLabel)

        assertEquals("es_lat", viewModel.uiState.value.selectedAudioId)
        assertEquals(latinoLabel, viewModel.uiState.value.audioFeedbackToast)

        // Clear toast
        viewModel.clearAudioToast()
        assertNull(viewModel.uiState.value.audioFeedbackToast)
    }
}
