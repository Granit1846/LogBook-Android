package com.example.logbook.data.local

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.room.withTransaction
import com.example.logbook.data.local.entity.Chast1Entity
import com.example.logbook.data.local.entity.LaunchEntity
import com.example.logbook.data.local.entity.SnkEntity
import com.example.logbook.data.local.entity.Ta14Entity
import com.example.logbook.data.local.entity.Vk250001Entity
import com.example.logbook.data.local.entity.Vk250002Entity
import com.example.logbook.data.local.entity.Vr80Entity
import com.example.logbook.data.local.recalc.Chast1Recalc
import com.example.logbook.data.local.recalc.RecalcCommon
import com.example.logbook.data.local.recalc.Ta14Recalc
import com.example.logbook.data.local.recalc.Vk250001Recalc
import com.example.logbook.data.local.recalc.Vk250002Recalc
import com.example.logbook.data.local.recalc.Vr80Recalc

/**
 * Порт C# MainViewModel.EndDay + RemoveDateFromAllFormulars.
 *
 * Маршрутизация (по схеме):
 *   vozduh/zemlya > 0 при tp == 0 → Часть 1 + ВР-80 + ВК-01 + ВК-02
 *   tp > 0      → ВР-80 (земля/воздух этого запуска игнорируются)
 *   vsu > 0     → Та-14
 *   СНК двигателя без запусков → соответствующий ВК-01 или ВК-02
 *   заправка>0  → только в Launch
 *
 * Валидация:
 *   Любой launch с tp > 0 и (vsu == 0 || zemlya > 0 || vozduh > 0) — ошибка.
 *   Возвращается в DayWriterResult.errors для показа в UI.
 *   При этом такой launch учитывается как «только ТП» (земля/воздух игнорируются).
 *
 * Транзакция: всё пишется одной транзакцией — либо всё, либо ничего.
 */
class DayWriter(private val db: AppDatabase) {

    // ============================================================
    // Результат
    // ============================================================

    data class LaunchError(
        val launchId: Long,
        val launchOrder: Int,
        val message: String
    )

    data class Result(
        val writtenFormulars: Set<FormularType>,
        val errors: List<LaunchError>
    )

    enum class FormularType { CHAST1, VR80, TA14, VK01, VK02 }

    // ============================================================
    // Публичный API: закрыть день
    // ============================================================

    /**
     * Записывает день в формуляры.
     *
     * @param profileId — id профиля
     * @param date      — дата в формате dd.MM.yyyy
     * @param launches  — список запусков дня
     * @param snk       — значения СНК дня (может быть null)
     */
    suspend fun writeDay(
        profileId: Long,
        date: String,
        launches: List<LaunchEntity>,
        snk: SnkEntity?
    ): Result = db.withTransaction {
        val localDate = RecalcCommon.parseDate(date)
            ?: return@withTransaction Result(emptySet(), emptyList())

        val errors = validateLaunches(launches)
        val written = mutableSetOf<FormularType>()
        val profile = db.profileDao().getById(profileId)
        val installed = AggregateManager.numbers(profile?.aggregateInstalledAtJson ?: "{}")
        fun active(type: String): Boolean = installed[type]?.let { !localDate.isBefore(java.time.LocalDate.parse(it)) } ?: true

        // ============================================================
        // 1. Определяем, что писать (по маршрутизации)
        // ============================================================

        // Только валидные «с точки зрения учёта» запуски — те, у которых
        // нет конфликта ТП vs земля/воздух. Конфликтные уже отфильтрованы
        // для расчёта внутри Recalc'ов (пропускают zemlya/vozduh при tp > 0).
        // Здесь мы только определяем наличие данных.

        val hasVozduh = launches.any { it.tp <= 0 && it.vozduh > 0 }
        val hasZemlya = launches.any { it.tp <= 0 && it.zemlya > 0 }
        val hasTp = launches.any { it.tp > 0 }
        val hasVsu = launches.any { it.vsu > 0 }

        // Часть 1 / ВК-01/02: земля или воздух (не ТП)
        val writeChast1 = hasVozduh || hasZemlya
        val writeVkLeft = hasVozduh || hasZemlya || snk?.let {
            listOf(it.aLeft, it.bLeft, it.eLeft, it.gLeft, it.hLeft, it.iLeft).any { value -> value != 0.0 }
        } == true
        val writeVkRight = hasVozduh || hasZemlya || snk?.let {
            listOf(it.aRight, it.bRight, it.eRight, it.gRight, it.hRight, it.iRight).any { value -> value != 0.0 }
        } == true
        // ВР-80: земля, воздух или ТП
        val writeVr80 = hasVozduh || hasZemlya || hasTp
        // Та-14 учитывает только фактическую работу ВСУ.
        val writeTa14 = hasVsu

        // ============================================================
        // 2. Записываем «сырые» строки (только дата + дневные поля)
        // ============================================================

        if (writeChast1) {
            writeChast1Row(profileId, date)
            written += FormularType.CHAST1
        } else {
            removeChast1Row(profileId, date)
        }

        // Earlier days belong to the removed aggregate, never to the current installation.
        if (active("Vr80")) {
            if (writeVr80) {
                writeVr80Row(profileId, date)
                written += FormularType.VR80
            } else {
                removeVr80Row(profileId, date)
            }
        }

        if (active("Ta14")) {
            if (writeTa14) {
                writeTa14Row(profileId, date)
                written += FormularType.TA14
            } else {
                removeTa14Row(profileId, date)
            }
        }

        if (active("Vk250001")) {
            if (writeVkLeft) {
                writeVkRow(profileId, date, isLeft = true)
                written += FormularType.VK01
            } else {
                removeVkRow(profileId, date, isLeft = true)
            }
        }
        if (active("Vk250002")) {
            if (writeVkRight) {
                writeVkRow(profileId, date, isLeft = false)
                written += FormularType.VK02
            } else {
                removeVkRow(profileId, date, isLeft = false)
            }
        }

        // ============================================================
        // 3. Полный пересчёт всех формуляров (накопительные, итоги, порядок)
        // ============================================================

        Chast1Recalc(db, profileId).recalculateAll()
        Vr80Recalc(db, profileId).recalculateAll()
        Ta14Recalc(db, profileId).recalculateAll()
        Vk250001Recalc(db, profileId).recalculateAll()
        Vk250002Recalc(db, profileId).recalculateAll()

        Result(written, errors)
    }

    // ============================================================
    // Валидация
    // ============================================================

    /**
     * Возвращает список ошибок ввода.
     *
     * Ошибка — если tp > 0 и при этом заполнено zemlya или vozduh.
     * ВСУ при ТП-запуске — НОРМАЛЬНО (ТП идёт после ВСУ), не ошибка.
     */
    private fun validateLaunches(launches: List<LaunchEntity>): List<LaunchError> {
        val errors = mutableListOf<LaunchError>()
        for (l in launches) {
            if (l.tp <= 0) continue
            if (l.zemlya == 0 && l.vozduh == 0) continue
            errors += LaunchError(
                launchId = l.id,
                launchOrder = l.order,
                message = tr(R.string.msg_ground_and_or_flight_time_is_not_allowed_for_a_0976970)
            )
        }
        return errors
    }

    // ============================================================
    // Строки формуляров: пишем только дату + дневные поля
    // (накопительные заполнит Recalc)
    // ============================================================

    /**
     * Часть 1 — строка дня. Если уже есть — обновляем.
     * Если нет — вставляем.
     * Все накопительные поля (col5, colSNE, col7, landings и т.д.) —
     * оставляем 0, их заполнит Chast1Recalc.
     */
    private suspend fun writeChast1Row(profileId: Long, date: String) {
        val dao = db.chast1Dao()
        val existing = dao.getByDate(profileId, date)
        val localDate = RecalcCommon.parseDate(date) ?: return
        val sortOrder = RecalcCommon.regularSortOrder(localDate)

        if (existing == null) {
            dao.insert(Chast1Entity(profileId = profileId, date = date, sortOrder = sortOrder))
        } else if (existing.isTotal) {
            dao.delete(existing)
            dao.insert(Chast1Entity(profileId = profileId, date = date, sortOrder = sortOrder))
        } else if (existing.sortOrder != sortOrder) {
            // На случай если sortOrder старый/битый — поправим
            dao.update(existing.copy(sortOrder = sortOrder))
        }
    }

    private suspend fun removeChast1Row(profileId: Long, date: String) {
        val dao = db.chast1Dao()
        val existing = dao.getByDate(profileId, date) ?: return
        if (existing.isTotal) return
        dao.delete(existing)
    }

    private suspend fun writeVr80Row(profileId: Long, date: String) {
        val dao = db.vr80Dao()
        val existing = dao.getByDate(profileId, date)
        val localDate = RecalcCommon.parseDate(date) ?: return
        val sortOrder = RecalcCommon.regularSortOrder(localDate)

        if (existing == null) {
            dao.insert(Vr80Entity(profileId = profileId, date = date, sortOrder = sortOrder))
        } else if (existing.isTotal) {
            dao.delete(existing)
            dao.insert(Vr80Entity(profileId = profileId, date = date, sortOrder = sortOrder))
        } else if (existing.sortOrder != sortOrder) {
            dao.update(existing.copy(sortOrder = sortOrder))
        }
    }

    private suspend fun removeVr80Row(profileId: Long, date: String) {
        val dao = db.vr80Dao()
        val existing = dao.getByDate(profileId, date) ?: return
        if (existing.isTotal) return
        dao.delete(existing)
    }

    private suspend fun writeTa14Row(profileId: Long, date: String) {
        val dao = db.ta14Dao()
        val existing = dao.getByDate(profileId, date)
        val localDate = RecalcCommon.parseDate(date) ?: return
        val sortOrder = RecalcCommon.regularSortOrder(localDate)

        if (existing == null) {
            dao.insert(Ta14Entity(profileId = profileId, date = date, sortOrder = sortOrder))
        } else if (existing.isTotal) {
            dao.delete(existing)
            dao.insert(Ta14Entity(profileId = profileId, date = date, sortOrder = sortOrder))
        } else if (existing.sortOrder != sortOrder) {
            dao.update(existing.copy(sortOrder = sortOrder))
        }
    }

    private suspend fun removeTa14Row(profileId: Long, date: String) {
        val dao = db.ta14Dao()
        val existing = dao.getByDate(profileId, date) ?: return
        if (existing.isTotal) return
        dao.delete(existing)
    }

    private suspend fun writeVkRow(profileId: Long, date: String, isLeft: Boolean) {
        val localDate = RecalcCommon.parseDate(date) ?: return
        val sortOrder = RecalcCommon.regularSortOrder(localDate)

        if (isLeft) {
            val dao = db.vk250001Dao()
            val existing = dao.getByDate(profileId, date)
            if (existing == null) {
                dao.insert(
                    Vk250001Entity(
                        profileId = profileId,
                        date = date,
                        sortOrder = sortOrder
                    )
                )
            } else if (existing.isTotal) {
                dao.delete(existing)
                dao.insert(
                    Vk250001Entity(
                        profileId = profileId,
                        date = date,
                        sortOrder = sortOrder
                    )
                )
            } else if (existing.sortOrder != sortOrder) {
                dao.update(existing.copy(sortOrder = sortOrder))
            }
        } else {
            val dao = db.vk250002Dao()
            val existing = dao.getByDate(profileId, date)
            if (existing == null) {
                dao.insert(
                    Vk250002Entity(
                        profileId = profileId,
                        date = date,
                        sortOrder = sortOrder
                    )
                )
            } else if (existing.isTotal) {
                dao.delete(existing)
                dao.insert(
                    Vk250002Entity(
                        profileId = profileId,
                        date = date,
                        sortOrder = sortOrder
                    )
                )
            } else if (existing.sortOrder != sortOrder) {
                dao.update(existing.copy(sortOrder = sortOrder))
            }
        }
    }

    private suspend fun removeVkRow(profileId: Long, date: String, isLeft: Boolean) {
        if (isLeft) {
            val dao = db.vk250001Dao()
            val existing = dao.getByDate(profileId, date) ?: return
            if (existing.isTotal) return
            dao.delete(existing)
        } else {
            val dao = db.vk250002Dao()
            val existing = dao.getByDate(profileId, date) ?: return
            if (existing.isTotal) return
            dao.delete(existing)
        }
    }

    // ============================================================
    // Удаление дня изо всех формуляров (порт RemoveDateFromAllFormulars)
    // ============================================================

    /**
     * Удаляет дату из всех 5 формуляров и DayData.
     * Используется при ручном удалении строки из любого формуляра.
     */
    suspend fun removeDateFromAllFormulars(profileId: Long, date: String) {
        db.withTransaction {
            // Формуляры
            db.chast1Dao().deleteRowsByDate(profileId, date)
            db.vr80Dao().deleteRowsByDate(profileId, date)
            db.ta14Dao().deleteRowsByDate(profileId, date)
            db.vk250001Dao().deleteRowsByDate(profileId, date)
            db.vk250002Dao().deleteRowsByDate(profileId, date)

            // DayData
            val day = db.dayDao().getByDate(profileId, date)
            if (day != null) {
                db.snkDao().deleteByDay(day.id)
                db.launchDao().deleteAllByDay(day.id)
                db.dayDao().delete(day)
            }

            // Пересчёт
            Chast1Recalc(db, profileId).recalculateAll()
            Vr80Recalc(db, profileId).recalculateAll(historicalDate = date)
            Ta14Recalc(db, profileId).recalculateAll(historicalDate = date)
            Vk250001Recalc(db, profileId).recalculateAll(historicalDate = date)
            Vk250002Recalc(db, profileId).recalculateAll(historicalDate = date)
        }
    }
}
