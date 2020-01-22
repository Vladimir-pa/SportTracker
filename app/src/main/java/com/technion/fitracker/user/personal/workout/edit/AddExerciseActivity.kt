package com.technion.fitracker.user.personal.workout.edit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.viewpager.widget.ViewPager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.technion.fitracker.R
import com.technion.fitracker.adapters.viewPages.TabsFragmentPagerAdapter
import com.technion.fitracker.databinding.ActivityAddExerciseBinding
import com.technion.fitracker.models.exercise.ExerciseDBModel
import com.technion.fitracker.models.nutrition.jsonDBModel
import com.technion.fitracker.models.workouts.CreateNewExerciseViewModel
import kotlinx.android.synthetic.main.activity_add_exercise.*
import java.util.*

class AddExerciseActivity : AppCompatActivity() {
    lateinit var viewModel: CreateNewExerciseViewModel
    lateinit var navController: NavController
    lateinit var adapter: TabsFragmentPagerAdapter
    lateinit var viewPager: ViewPager
    private val WEIGHT_PAGE = 0
    private val AEROBIC_PAGE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this)[CreateNewExerciseViewModel::class.java]
        val binding = DataBindingUtil.setContentView<ActivityAddExerciseBinding>(this, R.layout.activity_add_exercise)
        binding.lifecycleOwner = this
        binding.myViewModel = viewModel

        initDB()
        viewPager = findViewById(R.id.exerciseChooseViewPager)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = "Add new exercise"
            setDisplayHomeAsUpEnabled(true)
        }
        adapter = TabsFragmentPagerAdapter(supportFragmentManager).apply {
            addFragment(WeightExerciseFragment(), "Weight")
            addFragment(AerobicExerciseFragment(), "Aerobic")
        }
        viewPager.adapter = adapter
        tabs.setupWithViewPager(viewPager)
    }

    override fun onSupportNavigateUp(): Boolean {
        safeBack()
        return true
    }

    override fun onBackPressed() {
        safeBack()

    }

    private fun safeBack() {
        if (!checkIfDataExist()) {
            this.setResult(CreateNewWorkoutActivity.ResultCodes.RETURN.ordinal)
            this.finish()
        } else {
            MaterialAlertDialogBuilder(this)
                    .setTitle("Warning")
                    .setMessage("Data will be lost, continue?")
                    .setPositiveButton(
                            "Yes"
                    ) { _, _ ->
                        this.setResult(CreateNewWorkoutActivity.ResultCodes.RETURN.ordinal)
                        this.finish()
                    }
                    .setNegativeButton(
                            "No"
                    ) { _, _ ->

                    }.show()
        }
    }

    private fun initDB() {
        val stream = this.assets.open("exercises_with_gifs.json")
        val s = Scanner(stream).useDelimiter("\\A")
        val json = if (s.hasNext()) {
            s.next()
        } else {
            ""
        }
        viewModel.exerciseDB = Gson().fromJson(json, ExerciseDBModel::class.java).array
    }

    private fun checkIfDataExist(): Boolean {
        return when (viewPager.currentItem) {
            WEIGHT_PAGE -> {
                !viewModel.weight_name.value.isNullOrBlank() ||
                        !viewModel.weight_weight.value.isNullOrBlank() ||
                        !viewModel.weight_sets.value.isNullOrBlank() ||
                        !viewModel.weight_repetitions.value.isNullOrBlank() ||
                        !viewModel.weight_rest.value.isNullOrBlank() ||
                        !viewModel.weight_notes.value.isNullOrBlank()
            }
            AEROBIC_PAGE -> {
                !viewModel.aerobic_name.value.isNullOrBlank() ||
                        !viewModel.aerobic_duration.value.isNullOrBlank() ||
                        !viewModel.aerobic_speed.value.isNullOrBlank() ||
                        !viewModel.aerobic_intensity.value.isNullOrBlank() ||
                        !viewModel.aerobic_notes.value.isNullOrBlank()
            }
            else -> false
        }
    }

}

