package app.photidy

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.photidy.core.designsystem.theme.PhotidyTheme
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.AppSettings
import app.photidy.core.model.GestureAction
import app.photidy.core.model.PhotoItem
import app.photidy.core.model.PhotoScene
import app.photidy.core.model.PhotoState
import app.photidy.core.model.PhotoWithState
import app.photidy.core.model.SwipeDirection
import app.photidy.feature.home.HomeScreen
import app.photidy.feature.home.HomeUiState
import app.photidy.feature.organizer.OrganizerScreen
import app.photidy.feature.organizer.OrganizerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotidyUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun homeShowsPrimaryActionAndNavigationEntries() {
        composeRule.setContent {
            PhotidyTheme(settings = AppSettings(animationsEnabled = false)) {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        totalPhotos = 56,
                        organizedCount = 12,
                        favoriteCount = 3,
                        deleteCount = 4,
                        deleteBytes = 12_500_000,
                        activeSessionRemaining = 8,
                    ),
                    accessDenied = false,
                    onRequestAccess = {},
                    onKeepDemo = {},
                    onStartOrganizer = {},
                    onContinueOrganizer = {},
                    onOpenReview = {},
                    onOpenInsights = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(LocR.string.home_continue_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(LocR.string.home_review_title))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(LocR.string.cd_settings))
            .assertIsDisplayed()
    }

    @Test
    fun swipeLeftCommitsTheMappedAction() {
        var swipedDirection: SwipeDirection? = null
        composeRule.setContent {
            PhotidyTheme(settings = AppSettings(animationsEnabled = false)) {
                OrganizerScreen(
                    state = OrganizerUiState(
                        isLoading = false,
                        hasSession = true,
                        current = previewPhoto(),
                        total = 10,
                        cursor = 0,
                        remaining = 10,
                    ),
                    onBack = {},
                    onOpenReview = {},
                    onOpenPhoto = {},
                    onChoosePhotos = {},
                    onAction = {},
                    onDirection = { swipedDirection = it },
                    onUndo = {},
                    onChooseAlbum = {},
                    onDismissAlbumPicker = {},
                    snackbarHostState = remember { SnackbarHostState() },
                )
            }
        }

        composeRule.onNodeWithTag("photo_card").performTouchInput {
            swipe(
                start = Offset(width * 0.8f, height * 0.5f),
                end = Offset(width * 0.12f, height * 0.5f),
                durationMillis = 400,
            )
        }
        composeRule.waitUntil(timeoutMillis = 3_000) { swipedDirection != null }

        assertEquals(SwipeDirection.LEFT, swipedDirection)
    }

    @Test
    fun actionBarButtonDispatchesTheBoundAction() {
        var action: GestureAction? = null
        composeRule.setContent {
            PhotidyTheme(settings = AppSettings(animationsEnabled = false)) {
                OrganizerScreen(
                    state = OrganizerUiState(
                        isLoading = false,
                        hasSession = true,
                        current = previewPhoto(),
                        total = 10,
                        cursor = 0,
                        remaining = 10,
                    ),
                    onBack = {},
                    onOpenReview = {},
                    onOpenPhoto = {},
                    onChoosePhotos = {},
                    onAction = { action = it },
                    onDirection = {},
                    onUndo = {},
                    onChooseAlbum = {},
                    onDismissAlbumPicker = {},
                    snackbarHostState = remember { SnackbarHostState() },
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            context.getString(LocR.string.organizer_action_delete),
        ).performClick()

        assertTrue(action == GestureAction.ADD_TO_DELETE_LIST)
    }

    private fun previewPhoto(): PhotoWithState = PhotoWithState(
        photo = PhotoItem(
            id = "test_photo",
            name = "IMG_1001",
            albumId = "camera",
            capturedAt = System.currentTimeMillis(),
            width = 4032,
            height = 3024,
            sizeBytes = 4_200_000,
            scene = PhotoScene.MOUNTAIN_DAWN,
            artSeed = 7,
        ),
        state = PhotoState(),
    )
}
