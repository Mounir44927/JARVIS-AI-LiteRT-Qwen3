package com.jarvis.ai

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.rive.runtime.kotlin.core.Rive
import com.jarvis.ai.brain.ConfiguredAiGateway
import com.jarvis.ai.brain.KoogAgentGateway
import com.jarvis.ai.brain.LiteRtLmGateway
import com.jarvis.ai.brain.ModelRuntimeCoordinator
import com.jarvis.ai.brain.LocalFirstRoutingGateway
import com.jarvis.ai.data.GeminiApiKeyStore
import com.jarvis.ai.data.GeminiEmbeddingService
import com.jarvis.ai.data.MemoryRepository
import com.jarvis.ai.data.SearchMemoryRepository
import com.jarvis.ai.data.SettingsStore
import com.jarvis.ai.data.db.AppDatabase

class JarvisApp : Application() {
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Fact ADD COLUMN embeddingJson TEXT")
                db.execSQL("ALTER TABLE SearchMemory ADD COLUMN embeddingJson TEXT")
            }
        }
    }

    lateinit var db: AppDatabase
        private set
    lateinit var memory: MemoryRepository
        private set
    lateinit var searchMemory: SearchMemoryRepository
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var geminiKeyStore: GeminiApiKeyStore
        private set
    lateinit var modelRuntimeCoordinator: ModelRuntimeCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        Rive.init(this)
        geminiKeyStore = GeminiApiKeyStore(this)
        db = Room.databaseBuilder(this, AppDatabase::class.java, "jarvis.db")
            .addMigrations(MIGRATION_1_2)
            .build()
        val embeddings = GeminiEmbeddingService(geminiKeyStore)
        memory = MemoryRepository(db, embeddings)
        searchMemory = SearchMemoryRepository(db.dao(), embeddings)
        settings = SettingsStore(this)
        modelRuntimeCoordinator = ModelRuntimeCoordinator(this)
    }

    fun createGateway(): LocalFirstRoutingGateway {
        val cloudFallback = ConfiguredAiGateway(geminiKeyStore)
        val koog = KoogAgentGateway(
            keyStore = geminiKeyStore,
            memory = memory,
            searchMemory = searchMemory,
            fallback = cloudFallback
        )
        val local = LiteRtLmGateway(this, runtimeCoordinator = modelRuntimeCoordinator)
        return LocalFirstRoutingGateway(local = local, cloud = koog)
    }
}
