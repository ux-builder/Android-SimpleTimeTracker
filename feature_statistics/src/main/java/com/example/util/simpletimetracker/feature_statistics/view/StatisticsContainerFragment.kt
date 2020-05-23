package com.example.util.simpletimetracker.feature_statistics.view

import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.core.extension.flipVisibility
import com.example.util.simpletimetracker.core.extension.setOnClick
import com.example.util.simpletimetracker.core.extension.setOnLongClick
import com.example.util.simpletimetracker.core.extension.visible
import com.example.util.simpletimetracker.feature_statistics.R
import com.example.util.simpletimetracker.feature_statistics.adapter.StatisticsContainerAdapter
import com.example.util.simpletimetracker.feature_statistics.di.StatisticsComponentProvider
import com.example.util.simpletimetracker.feature_statistics.viewData.RangeLength
import com.example.util.simpletimetracker.feature_statistics.viewModel.StatisticsContainerViewModel
import kotlinx.android.synthetic.main.statistics_container_fragment.*

class StatisticsContainerFragment : BaseFragment() {

    override val layoutId: Int = R.layout.statistics_container_fragment

    private val viewModel: StatisticsContainerViewModel by viewModels()
    private var adapter: StatisticsContainerAdapter? = null

    override fun initDi() {
        val component = (activity?.application as StatisticsComponentProvider)
            .statisticsComponent

        component?.inject(viewModel)
    }

    override fun initUx() {
        btnStatisticsContainerPrevious.setOnClick(viewModel::onPreviousClick)
        btnStatisticsContainerNext.setOnClick(viewModel::onNextClick)
        btnStatisticsContainerToday.setOnClick(layoutStatisticsContainerButtons::flipVisibility)
        btnStatisticsContainerToday.setOnLongClick(viewModel::onTodayClick)
        // TODO recycler?
        btnStatisticsContainerRange1.setOnClick { viewModel.onRangeClick(1) }
        btnStatisticsContainerRange2.setOnClick { viewModel.onRangeClick(2) }
        btnStatisticsContainerRange3.setOnClick { viewModel.onRangeClick(3) }
        btnStatisticsContainerRange4.setOnClick { viewModel.onRangeClick(4) }
    }

    override fun initViewModel() {
        viewModel.title.observe(viewLifecycleOwner, ::updateTitle)
        viewModel.rangeLength.observe(viewLifecycleOwner, ::updateRange)
        viewModel.position.observe(viewLifecycleOwner, ::updatePosition)
    }

    private fun setupPager(rangeLength: RangeLength) {
        adapter = StatisticsContainerAdapter(this, rangeLength)
        pagerStatisticsContainer.apply {
            this.adapter = this@StatisticsContainerFragment.adapter
            offscreenPageLimit = 2
            isUserInputEnabled = false
        }
    }

    private fun updateRange(rangeLength: RangeLength) {
        // TODO avoid recreation, implement update
        setupPager(rangeLength)
        if (rangeLength == RangeLength.ALL) {
            btnStatisticsContainerPrevious.visible = false
            btnStatisticsContainerNext.visible = false
        } else {
            btnStatisticsContainerPrevious.visible = true
            btnStatisticsContainerNext.visible = true
        }
    }

    private fun updateTitle(title: String) {
        layoutStatisticsContainerButtons.visible = false
        btnStatisticsContainerToday.text = title
    }

    private fun updatePosition(position: Int) {
        pagerStatisticsContainer.currentItem = position + StatisticsContainerAdapter.FIRST
    }

    companion object {
        fun newInstance() = StatisticsContainerFragment()
    }
}