package com.rizwan.kidslearn

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

sealed interface Screen {
    data object Splash : Screen
    data object Profiles : Screen
    data object Home : Screen
    data object Buddies : Screen
    data class Quiz(val test: Boolean) : Screen
    data object Fun : Screen
    data object Progress : Screen
    data object Parent : Screen
    data object Chat : Screen
    data object Models : Screen
    data object Speak : Screen
}

class AppState(val ctx: Context) {
    val store = Store(ctx)
    val speaker = Speaker(ctx)
    val models = ModelManager(ctx)
    val engine = LlmEngine(ctx, models)
    val brain = Brain(engine)
    val voice = VoiceInput(ctx)

    init { KnowledgeBase.load(ctx) }
    var profile by mutableStateOf<Profile?>(null)
    var buddy by mutableStateOf(BUDDIES[0])
    var progressVersion by mutableIntStateOf(0)

    fun selectProfile(p: Profile) { profile = p; buddy = buddyById(store.buddyId(p)); Bank.load(ctx, p.level) }
    fun chooseBuddy(b: Buddy) { buddy = b; profile?.let { store.setBuddy(it.id, b.id) } }
    fun say(text: String) = speaker.speakEn(text, buddy.pitch)
    suspend fun ensureModelLoaded() {
        val f = models.activeFile() ?: return
        if (engine.loadedName == f.name) return
        engine.load(f)
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var state: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        state = AppState(applicationContext)
        setContent { AppTheme { App(state) } }
    }

    override fun onDestroy() {
        state.speaker.shutdown()
        state.engine.unload()
        state.voice.destroy()
        super.onDestroy()
    }
}

@Composable
fun App(s: AppState) {
    var screen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var quizKey by remember { mutableIntStateOf(0) }

    BackHandler(enabled = screen != Screen.Splash && screen != Screen.Profiles) {
        screen = when (screen) { Screen.Home -> Screen.Profiles; Screen.Models -> Screen.Parent; else -> Screen.Home }
    }

    AnimatedBackground {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            AnimatedContent(screen, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) }, label = "nav") { sc ->
                when (sc) {
                    Screen.Splash -> SplashScreen { screen = Screen.Profiles }
                    Screen.Profiles -> ProfileScreen(s) { screen = Screen.Home }
                    Screen.Home -> HomeScreen(s, go = { screen = it }, onSwitch = { screen = Screen.Profiles })
                    Screen.Buddies -> BuddyPickerScreen(s) { screen = Screen.Home }
                    is Screen.Quiz -> key(quizKey) {
                        QuizScreen(s, sc.test, onExit = { screen = Screen.Home }, onRestart = { quizKey++ })
                    }
                    Screen.Fun -> FunScreen(s) { screen = Screen.Home }
                    Screen.Progress -> ProgressScreen(s) { screen = Screen.Home }
                    Screen.Parent -> ParentScreen(s, onBack = { screen = Screen.Home }, onModels = { screen = Screen.Models })
                    Screen.Chat -> ChatScreen(s) { screen = Screen.Home }
                    Screen.Models -> ModelsScreen(s) { screen = Screen.Parent }
                    Screen.Speak -> SpeakScreen(s) { screen = Screen.Home }
                }
            }
        }
    }
}
