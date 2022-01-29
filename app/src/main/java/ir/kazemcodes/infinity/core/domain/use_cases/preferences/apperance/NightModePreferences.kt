package ir.kazemcodes.infinity.core.domain.use_cases.preferences.apperance

import ir.kazemcodes.infinity.core.domain.repository.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class SaveNightModePreferences(
    private val repository: Repository,
) {
    operator fun invoke(mode: NightMode) {
        repository.preferencesHelper.nightModeKey.set(mode.mode)
    }
}

class ReadNightModePreferences(
    private val repository: Repository,
) {
    operator fun invoke(): Flow<NightMode>  = flow {
        when (repository.preferencesHelper.nightModeKey.get()) {
            -1 -> emit(NightMode.FollowSystem)
            1 -> emit(NightMode.Enable)
            0 -> emit(NightMode.Disable)
            else -> emit(NightMode.Disable)
        }
    }
}

sealed class NightMode(val mode: Int) {
    object FollowSystem : NightMode(-1)
    object Enable : NightMode(1)
    object Disable : NightMode(0)
}