package com.interactiveword.ui.screens.home

import android.app.Application
import android.media.MediaPlayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.R
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.User
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.repository.UserRepository
import com.interactiveword.data.repository.WordRepository
import com.interactiveword.ui.components.AppNotification
import com.interactiveword.ui.components.NotiType
import com.interactiveword.ui.components.XpManager
import com.interactiveword.util.RankManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val user: User? = null,
    val recentWords: List<WordCard> = emptyList(),
    val wordCount: Int = 0,
    val bestPronunciationWord: String? = null,
    val bestPronunciationScore: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val userRepo: UserRepository = UserRepository(),
    private val wordRepo: WordRepository = WordRepository(),
) : AndroidViewModel(application) {

    private val context = getApplication<Application>()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val oldUser = _uiState.value.user
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val user = userRepo.getMe()
                val words = wordRepo.getMyWords()

                // 변경 감지 및 알림
                if (oldUser != null) {
                    val localizedContext = com.interactiveword.data.local.LanguageManager.applyLocale(context)
                    // 1. 랭크 업 감지
                    val oldRank = RankManager.getCurrentBand(oldUser.xp).rank
                    val newRank = RankManager.getCurrentBand(user.xp).rank
                    
                    if (newRank != oldRank) {
                        XpManager.emitNotification(
                            AppNotification(
                                type = NotiType.RANK_UP,
                                messageRes = R.string.noti_rank_up,
                                messageArgs = listOf(RankManager.getRankLabel(localizedContext, newRank)),
                                color = Color(0xFFFFA000), // Gold
                                icon = Icons.Default.EmojiEvents
                            )
                        )
                    }

                    // 2. 단어 슬롯 확장 감지
                    if (user.maxWordSlots > oldUser.maxWordSlots) {
                        XpManager.emitNotification(
                            AppNotification(
                                type = NotiType.SLOT_INCREASE,
                                messageRes = R.string.noti_word_slot_increase,
                                messageArgs = listOf(user.maxWordSlots),
                                color = Color(0xFF2196F3), // Blue
                                icon = Icons.Default.Bolt
                            )
                        )
                    }

                    // 3. 티켓 획득 감지 (500 XP 마다)
                    val oldTickets = oldUser.xp / 500
                    val newTickets = user.xp / 500
                    if (newTickets > oldTickets) {
                        XpManager.emitNotification(
                            AppNotification(
                                type = NotiType.TICKET,
                                messageRes = R.string.noti_ticket_acquired,
                                color = Color(0xFFE91E63), // Pink
                                icon = Icons.Default.ConfirmationNumber
                            )
                        )
                    }
                }

                val bestCard = words.filter { it.bestScore > 0 }.maxByOrNull { it.bestScore }

                _uiState.value = _uiState.value.copy(
                    user = user,
                    recentWords = words.takeLast(4).reversed(),
                    wordCount = words.size,
                    bestPronunciationWord = bestCard?.koreanWord,
                    bestPronunciationScore = bestCard?.bestScore?.toInt() ?: 0,
                    isLoading = false,
                    error = null,
                )
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    fun changeLanguage(newLanguage: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                userRepo.updateLanguage(newLanguage)
                loadData()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                userRepo.deleteAccount()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun playTts(path: String?) {
        val url = RetrofitClient.resolveStaticUrl(path) ?: return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }
}
