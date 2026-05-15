package com.asmr.player.data.local.db

import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationsTest {
    @Test
    fun migration20To21_addsPlaylistPlaybackContextColumns() {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "migration-test-${System.nanoTime()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(20) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `playlist_items` (
                                `playlistId` INTEGER NOT NULL,
                                `mediaId` TEXT NOT NULL,
                                `title` TEXT NOT NULL,
                                `artist` TEXT NOT NULL DEFAULT '',
                                `uri` TEXT NOT NULL,
                                `artworkUri` TEXT NOT NULL DEFAULT '',
                                `itemOrder` INTEGER NOT NULL DEFAULT 0,
                                PRIMARY KEY(`playlistId`, `mediaId`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            "INSERT INTO playlist_items(playlistId, mediaId, title, artist, uri, artworkUri, itemOrder) VALUES(1, 'a', 'Track A', 'Artist', 'file:///a.mp3', '', 0)"
                        )
                    }

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabaseMigrations.MIGRATION_20_21)
            .allowMainThreadQueries()
            .build()

        val cursor = migrated.openHelper.writableDatabase.query(
            "SELECT albumTitle, albumId, trackId, rjCode, albumWorkId, trackGroup, lyricsRelativePathNoExt, mimeType, isVideo FROM playlist_items WHERE playlistId = 1 AND mediaId = 'a'"
        )
        cursor.use {
            it.moveToFirst()
            assertEquals("", it.getString(0))
            assertEquals(0L, it.getLong(1))
            assertEquals(0L, it.getLong(2))
            assertEquals("", it.getString(3))
            assertEquals("", it.getString(4))
            assertEquals("", it.getString(5))
            assertEquals("", it.getString(6))
            assertEquals("", it.getString(7))
            assertEquals(0, it.getInt(8))
        }

        migrated.close()
        File(dbFile.absolutePath).delete()
        File(dbFile.absolutePath + "-wal").delete()
        File(dbFile.absolutePath + "-shm").delete()
    }
}
