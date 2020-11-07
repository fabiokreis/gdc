package com.roquebuarque.gdc.feature.task.add.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.roquebuarque.gdc.GdcApplication
import com.roquebuarque.gdc.R
import com.roquebuarque.gdc.feature.DatePickerFragment
import com.roquebuarque.gdc.feature.TimePickerFragment
import com.roquebuarque.gdc.feature.task.add.TaskAddViewModel
import com.roquebuarque.gdc.feature.task.data.entity.TaskDateDto
import com.roquebuarque.gdc.feature.task.data.entity.TaskDto
import com.roquebuarque.gdc.job.NotificationJobService
import kotlinx.android.synthetic.main.activity_task_new.*
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.ChronoUnit

class TaskAddActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener,
    TimePickerDialog.OnTimeSetListener {

    private lateinit var viewModel: TaskAddViewModel
    private var taskDate: TaskDateDto = TaskDateDto()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_new)

        //Add arrow back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(
            this,
            TaskAddViewModel.TaskViewModelFactory(
                GdcApplication.instance
            )
        ).get(TaskAddViewModel::class.java)

        btnTaskNewSave.setOnClickListener {
            val name = edtTaskNewName.text.toString()
            if (name.isNotEmpty()) {
                var taskDto = TaskDto(name = name)
                var offsetDateTime: OffsetDateTime? = null
                if (taskDate.isDateReady() && taskDate.isTimeReady()) {
                    offsetDateTime = setTaskDateTime(taskDate)

                    taskDto = taskDto.copy(date = offsetDateTime)

                    createScheduler(taskDto)
                } else {
                    Toast.makeText(this, R.string.date_not_set, Toast.LENGTH_LONG)
                        .show()
                }
                viewModel.insert(taskDto)
                finish()

            } else {
                Snackbar.make(edtTaskNewName, R.string.name_required, Snackbar.LENGTH_LONG).show()
            }
        }

        txtTaskNewDate.setOnClickListener {
            showDatePickerDialog()
        }

        txtTaskNewTime.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun createScheduler(taskDto: TaskDto) {
        //Create scheduler from system
        val scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val serviceName = ComponentName(packageName, NotificationJobService::class.java.name)

        //Calculating time
        val timeTilFuture = ChronoUnit.MILLIS.between(OffsetDateTime.now(), taskDto.date)

        //Create schedule build info
        val builder = JobInfo.Builder(0, serviceName)
            .setMinimumLatency(timeTilFuture)

        //Create extras
        val extras = PersistableBundle()
        extras.putString(SCHEDULE_EXTRA_TASK_NAME, taskDto.name)

        //Build and schedule job
        val jobInfo = builder.setExtras(extras)
            .build()

        scheduler.schedule(jobInfo)
    }


    private fun showTimePickerDialog() {
        val newFragment: DialogFragment = TimePickerFragment.newInstance(this)
        newFragment.show(supportFragmentManager, "timePicker")
    }

    private fun showDatePickerDialog() {
        val newFragment: DialogFragment = DatePickerFragment.newInstance(this)
        newFragment.show(supportFragmentManager, "datePicker")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> return super.onOptionsItemSelected(item)
        }
        return false
    }

    @SuppressLint("SetTextI18n")
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, day: Int) {
        taskDate = taskDate.copy(day = day, month = month + 1, year = year)
        txtTaskNewDate.text = "$day/${this.taskDate.month}/$year"
    }

    @SuppressLint("SetTextI18n")
    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        taskDate = taskDate.copy(minute = minute, hour = hourOfDay)
        txtTaskNewTime.text = "$hourOfDay:$minute"
    }

    private fun setTaskDateTime(taskDateDto: TaskDateDto): OffsetDateTime {
        return OffsetDateTime.of(
            taskDateDto.year,
            taskDateDto.month,
            taskDateDto.day,
            taskDateDto.hour,
            taskDateDto.minute,
            0,
            0,
            OffsetDateTime.now().offset
        )
    }

    companion object {

        const val SCHEDULE_EXTRA_TASK_NAME = "SCHEDULE_EXTRA_TASK_NAME"

        /**
         * Start [TaskAddActivity]
         * @param context previous activity
         */
        fun start(context: Context): Intent {
            return Intent(context, TaskAddActivity::class.java)
        }
    }
}