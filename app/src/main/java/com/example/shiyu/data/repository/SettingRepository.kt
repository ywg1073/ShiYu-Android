package com.example.shiyu.data.repository

import com.example.shiyu.data.dao.SettingDao
import com.example.shiyu.data.entity.SettingEntity
import kotlinx.coroutines.flow.Flow

class SettingRepository(private val settingDao: SettingDao) {
    val allSettings: Flow<List<SettingEntity>> = settingDao.getAllSettings()

    suspend fun getSettingValue(key: String): String? = settingDao.getSettingValue(key)

    suspend fun saveSetting(key: String, value: String) {
        settingDao.insertSetting(SettingEntity(key, value))
    }

    suspend fun deleteSetting(key: String) = settingDao.deleteSetting(key)
}
