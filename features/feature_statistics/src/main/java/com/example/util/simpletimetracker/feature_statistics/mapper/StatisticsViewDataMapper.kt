package com.example.util.simpletimetracker.feature_statistics.mapper

import com.example.util.simpletimetracker.core.mapper.ColorMapper
import com.example.util.simpletimetracker.core.mapper.IconMapper
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.core.utils.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.core.viewData.StatisticsDataHolder
import com.example.util.simpletimetracker.domain.mapper.StatisticsMapper
import com.example.util.simpletimetracker.domain.model.RangeLength
import com.example.util.simpletimetracker.domain.model.Statistics
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.empty.EmptyViewData
import com.example.util.simpletimetracker.feature_base_adapter.hint.HintViewData
import com.example.util.simpletimetracker.feature_base_adapter.statistics.StatisticsViewData
import com.example.util.simpletimetracker.feature_base_adapter.statisticsGoal.StatisticsGoalViewData
import com.example.util.simpletimetracker.feature_statistics.R
import com.example.util.simpletimetracker.feature_statistics.viewData.StatisticsInfoViewData
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon
import javax.inject.Inject
import kotlin.math.roundToLong

class StatisticsViewDataMapper @Inject constructor(
    private val iconMapper: IconMapper,
    private val colorMapper: ColorMapper,
    private val resourceRepo: ResourceRepo,
    private val timeMapper: TimeMapper,
    private val statisticsMapper: StatisticsMapper,
) {

    fun mapItemsList(
        statistics: List<Statistics>,
        data: Map<Long, StatisticsDataHolder>,
        filteredIds: List<Long>,
        showDuration: Boolean,
        isDarkTheme: Boolean,
        useProportionalMinutes: Boolean,
        showSeconds: Boolean,
    ): List<StatisticsViewData> {
        val statisticsFiltered = statistics.filterNot { it.id in filteredIds }
        val sumDuration = statisticsFiltered.map(Statistics::duration).sum()
        val statisticsSize = statisticsFiltered.size

        return statisticsFiltered
            .mapNotNull { statistic ->
                val item = mapItem(
                    statistics = statistic,
                    sumDuration = sumDuration,
                    dataHolder = data[statistic.id],
                    showDuration = showDuration,
                    isDarkTheme = isDarkTheme,
                    statisticsSize = statisticsSize,
                    useProportionalMinutes = useProportionalMinutes,
                    showSeconds = showSeconds,
                ) ?: return@mapNotNull null

                item to statistic.duration
            }
            .sortedByDescending { (_, duration) -> duration }
            .map { (statistics, _) -> statistics }
    }

    fun mapGoalItemsList(
        rangeLength: RangeLength,
        statistics: List<Statistics>,
        runningStatistics: List<Statistics>,
        data: Map<Long, StatisticsDataHolder>,
        filteredIds: List<Long>,
        isDarkTheme: Boolean,
        useProportionalMinutes: Boolean,
        showSeconds: Boolean,
    ): List<StatisticsGoalViewData> {
        val statisticsFiltered = statistics.filterNot { it.id in filteredIds }

        return statisticsFiltered
            .mapNotNull { statistic ->
                val currentRunningDuration = runningStatistics
                    .filter { it.id == statistic.id }
                    .sumOf { it.duration }
                mapGoalItem(
                    rangeLength = rangeLength,
                    currentRunningDuration = currentRunningDuration,
                    statistics = statistic,
                    dataHolder = data[statistic.id] ?: return@mapNotNull null,
                    isDarkTheme = isDarkTheme,
                    useProportionalMinutes = useProportionalMinutes,
                    showSeconds = showSeconds,
                )
            }
            .sortedBy { it.goal.percent }
    }

    fun mapStatisticsTotalTracked(totalTracked: String): ViewHolderType {
        return StatisticsInfoViewData(
            name = resourceRepo.getString(R.string.statistics_total_tracked),
            text = totalTracked
        )
    }

    fun mapToEmpty(): ViewHolderType {
        return EmptyViewData(
            message = R.string.statistics_empty.let(resourceRepo::getString)
        )
    }

    fun mapToHint(): ViewHolderType {
        return HintViewData(
            text = R.string.statistics_hint.let(resourceRepo::getString)
        )
    }

    fun mapToGoalHint(): ViewHolderType {
        return HintViewData(
            text = R.string.change_record_type_goal_time_hint.let(resourceRepo::getString)
        )
    }

    private fun mapItem(
        statistics: Statistics,
        sumDuration: Long,
        dataHolder: StatisticsDataHolder?,
        showDuration: Boolean,
        isDarkTheme: Boolean,
        statisticsSize: Int,
        useProportionalMinutes: Boolean,
        showSeconds: Boolean,
    ): StatisticsViewData? {
        val durationPercent = statisticsMapper.getDurationPercentString(
            sumDuration = sumDuration,
            duration = statistics.duration,
            statisticsSize = statisticsSize
        )

        when {
            statistics.id == UNTRACKED_ITEM_ID -> {
                return StatisticsViewData(
                    id = statistics.id,
                    name = R.string.untracked_time_name
                        .let(resourceRepo::getString),
                    duration = statistics.duration
                        .let {
                            timeMapper.formatInterval(
                                interval = it,
                                forceSeconds = showSeconds,
                                useProportionalMinutes = useProportionalMinutes,
                            )
                        },
                    percent = durationPercent,
                    icon = RecordTypeIcon.Image(R.drawable.unknown),
                    color = colorMapper.toUntrackedColor(isDarkTheme)
                )
            }
            dataHolder != null -> {
                return StatisticsViewData(
                    id = statistics.id,
                    name = dataHolder.name,
                    duration = if (showDuration) {
                        timeMapper.formatInterval(
                            interval = statistics.duration,
                            forceSeconds = showSeconds,
                            useProportionalMinutes = useProportionalMinutes,
                        )
                    } else {
                        ""
                    },
                    percent = durationPercent,
                    icon = dataHolder.icon
                        ?.let(iconMapper::mapIcon),
                    color = dataHolder.color
                        .let { colorMapper.mapToColorInt(it, isDarkTheme) },
                )
            }
            else -> {
                return null
            }
        }
    }

    private fun mapGoalItem(
        rangeLength: RangeLength,
        currentRunningDuration: Long,
        statistics: Statistics,
        dataHolder: StatisticsDataHolder,
        isDarkTheme: Boolean,
        useProportionalMinutes: Boolean,
        showSeconds: Boolean,
    ): StatisticsGoalViewData {
        return StatisticsGoalViewData(
            id = statistics.id,
            name = dataHolder.name,
            icon = dataHolder.icon.orEmpty()
                .let(iconMapper::mapIcon),
            color = dataHolder.color
                .let { colorMapper.mapToColorInt(it, isDarkTheme) },
            goal = mapGoal(
                currentRunningDuration = currentRunningDuration,
                statistics = statistics,
                dataHolder = dataHolder,
                rangeLength = rangeLength,
                useProportionalMinutes = useProportionalMinutes,
                showSeconds = showSeconds,
            ),
        )
    }

    private fun mapGoal(
        currentRunningDuration: Long,
        statistics: Statistics,
        dataHolder: StatisticsDataHolder,
        rangeLength: RangeLength,
        useProportionalMinutes: Boolean,
        showSeconds: Boolean,
    ): StatisticsGoalViewData.Goal {
        val goal = when (rangeLength) {
            is RangeLength.Day -> dataHolder.dailyGoalTime
            is RangeLength.Week -> dataHolder.weeklyGoalTime
            is RangeLength.Month -> dataHolder.monthlyGoalTime
            else -> return StatisticsGoalViewData.Goal.empty()
        } * 1000

        if (goal == 0L) return StatisticsGoalViewData.Goal.empty()

        val current = statistics.duration + currentRunningDuration
        val goalComplete = goal - current <= 0L
        val currentString = timeMapper.formatInterval(
            interval = current,
            forceSeconds = showSeconds,
            useProportionalMinutes = useProportionalMinutes,
        )
        val goalString = timeMapper.formatInterval(
            interval = goal,
            forceSeconds = showSeconds,
            useProportionalMinutes = useProportionalMinutes,
        )
        val goalTimeString = "$currentString\n($goalString)"
        val goalTimePercent = (current * 100f / goal).roundToLong()
            .coerceAtMost(100)

        return StatisticsGoalViewData.Goal(
            goalTime = goalTimeString,
            goalPercent = goalTimePercent.let { "$it%" },
            goalComplete = goalComplete,
            percent = goalTimePercent,
        )
    }
}