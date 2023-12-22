package com.example.voicerecoder

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(AudioRecord::class), version = 1)
abstract class AudioRecordDB : RoomDatabase() {

    abstract fun audioRecordDao() : AudioRecordDAO
}