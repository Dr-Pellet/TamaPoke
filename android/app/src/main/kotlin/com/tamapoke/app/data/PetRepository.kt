package com.tamapoke.app.data

import android.content.Context
import com.tamapoke.app.data.db.PetDatabase
import com.tamapoke.app.data.db.toEntity
import com.tamapoke.app.data.db.toState
import com.tamapoke.core.PetEngine
import com.tamapoke.core.PetState
import com.tamapoke.core.dex.DexTable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single seam where offline catch-up, Room persistence and [PetEngine]
 * meet. Shared (one instance per process) so the app UI and the widget
 * always see and mutate the same state.
 */
class PetRepository private constructor(
    private val context: Context,
    val dex: DexTable,
) {
    private val dao = PetDatabase.get(context).petDao()
    private val mutex = Mutex()
    private val _state = MutableStateFlow<PetState?>(null)
    val state: StateFlow<PetState?> = _state

    private suspend fun ensureLoaded(): PetState = mutex.withLock {
        _state.value?.let { return it }
        val loaded = dao.getOnce()?.toState() ?: PetState()
        _state.value = loaded
        loaded
    }

    private suspend fun mutate(block: (PetState) -> PetState) {
        mutateWithResult { Pair(block(it), Unit) }
    }

    private suspend fun <T> mutateWithResult(block: (PetState) -> Pair<PetState, T>): T = mutex.withLock {
        val current = _state.value ?: dao.getOnce()?.toState() ?: PetState()
        val (next, result) = block(current)
        _state.value = next
        dao.upsert(next.toEntity())
        result
    }

    /** Call whenever the app/widget is touched: replays elapsed minutes since the last tick, exactly like Pet::syncClock(). */
    suspend fun catchUp(nowEpochSeconds: Long = System.currentTimeMillis() / 1000) {
        ensureLoaded()
        mutate { PetEngine.advanceOffline(it, dex, nowEpochSeconds) }
    }

    suspend fun feed() = mutate { PetEngine.feed(it) }
    suspend fun feedBerry(color: Int) = mutate { PetEngine.feedBerry(it, color) }
    suspend fun feedCandy() = mutate { PetEngine.feedCandy(it) }
    suspend fun play() = mutate { PetEngine.play(it) }
    suspend fun playResult(score: Int) = mutate { PetEngine.playResult(it, score) }
    /** Returns the strength gained this session (Pet::trainStrength()'s return value). */
    suspend fun trainStrength(hits: Int): Int = mutateWithResult { PetEngine.trainStrength(it, hits) }
    suspend fun clean() = mutate { PetEngine.clean(it) }
    suspend fun caress() = mutate { PetEngine.caress(it) }
    suspend fun toggleLight() = mutate { PetEngine.toggleLight(it) }
    suspend fun eggTap() = mutate { PetEngine.eggTap(it, dex) }
    suspend fun evolve() = mutate { PetEngine.evolve(it, dex) }
    suspend fun startFarewell() = mutate { PetEngine.startFarewell(it) }
    suspend fun startRunaway() = mutate { PetEngine.startRunaway(it) }
    suspend fun release() = mutate { PetEngine.release(it) }
    suspend fun rename(nickname: String) = mutate { PetEngine.rename(it, nickname) }
    suspend fun chooseStarter(starterDex: Int) = mutate { PetEngine.chooseStarter(it, starterDex) }
    suspend fun declineEvolve() = mutate { PetEngine.declineEvolve(it) }
    suspend fun declineFarewell() = mutate { PetEngine.declineFarewell(it) }

    /** Acknowledges a finished ceremony and starts the next life cycle (a fresh egg). */
    suspend fun resolveCeremony() = mutate { PetEngine.newEgg(it, dex) }

    /** One live game-minute tick, for the foreground "watch it happen in real time" loop. */
    suspend fun tickOnce() = mutate { PetEngine.tickOnce(it, dex).state }

    /** Serializes the current save to JSON, for exporting to a user-chosen local file. */
    suspend fun exportSave(): String = SaveFileCodec.encode(ensureLoaded())

    /** Overwrites the current save with one imported from a local file. */
    suspend fun importSave(json: String) = mutate { SaveFileCodec.decode(json) }

    companion object {
        @Volatile private var instance: PetRepository? = null

        fun get(context: Context): PetRepository = instance ?: synchronized(this) {
            instance ?: PetRepository(
                context.applicationContext,
                DexAssetLoader.load(context.applicationContext),
            ).also { instance = it }
        }
    }
}
