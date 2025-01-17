package keyur.diwan.project.waterReminder

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import keyur.diwan.project.waterReminder.databinding.ActivityMainBinding
import keyur.diwan.project.waterReminder.fragments.BottomSheetFragment
import keyur.diwan.project.waterReminder.helpers.AlarmHelper
import keyur.diwan.project.waterReminder.helpers.SqliteHelper
import keyur.diwan.project.waterReminder.utils.AppUtils
//import kotlinx.android.synthetic.main.activity_main.*


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var totalIntake: Int = 0
    private var inTook: Int = 0
    private lateinit var sharedPref: SharedPreferences
    private lateinit var sqliteHelper: SqliteHelper
    private lateinit var dateNow: String
    private var notificStatus: Boolean = false
    private var selectedOption: Int? = null
    private var snackbar: Snackbar? = null
    private var doubleBackToExitPressedOnce = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = getSharedPreferences(AppUtils.USERS_SHARED_PREF, AppUtils.PRIVATE_MODE)
        sqliteHelper = SqliteHelper(this)

        totalIntake = sharedPref.getInt(AppUtils.TOTAL_INTAKE, 0)

        if (sharedPref.getBoolean(AppUtils.FIRST_RUN_KEY, true)) {
            startActivity(Intent(this, WalkThroughActivity::class.java))
            finish()
        } else if (totalIntake <= 0) {
            startActivity(Intent(this, InitUserInfoActivity::class.java))
            finish()
        }

        dateNow = AppUtils.getCurrentDate()!!

    }

    fun updateValues() {
        totalIntake = sharedPref.getInt(AppUtils.TOTAL_INTAKE, 0)

        inTook = sqliteHelper.getIntook(dateNow)

        setWaterLevel(inTook, totalIntake)
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()

        val outValue = TypedValue()
        applicationContext.theme.resolveAttribute(
            android.R.attr.selectableItemBackground,
            outValue,
            true
        )

        notificStatus = sharedPref.getBoolean(AppUtils.NOTIFICATION_STATUS_KEY, true)
        val alarm = AlarmHelper()
        if (!alarm.checkAlarm(this) && notificStatus) {
            binding.btnNotific.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_bell))
            alarm.setAlarm(
                this,
                sharedPref.getInt(AppUtils.NOTIFICATION_FREQUENCY_KEY, 30).toLong()
            )
        }

        if (notificStatus) {
            binding.btnNotific.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_bell))
        } else {
            binding.btnNotific.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_bell_disabled))
        }

        sqliteHelper.addAll(dateNow, 0, totalIntake)

        updateValues()

        binding.btnMenu.setOnClickListener {
            val bottomSheetFragment = BottomSheetFragment(this)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

        binding.fabAdd.setOnClickListener {
            if (selectedOption != null) {
                if ((inTook * 100 / totalIntake) <= 140) {
                    if (sqliteHelper.addIntook(dateNow, selectedOption!!) > 0) {
                        inTook += selectedOption!!
                        setWaterLevel(inTook, totalIntake)

                        Snackbar.make(it, "Your water intake was saved...!!", Snackbar.LENGTH_SHORT)
                            .show()

                    }
                } else {
                    Snackbar.make(it, "You already achieved the goal", Snackbar.LENGTH_SHORT).show()
                }
                selectedOption = null
                binding.tvCustom.text = "Custom"
                binding.op50ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
                binding.op100ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
                binding.op150ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
                binding.op200ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
                binding.op250ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
                binding.opCustom.background = AppCompatResources.getDrawable(this, outValue.resourceId)

                // remove pending notifications
                val mNotificationManager : NotificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.cancelAll()
            } else {
                YoYo.with(Techniques.Shake)
                    .duration(700)
                    .playOn(binding.cardView)
                Snackbar.make(it, "Please select an option", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnNotific.setOnClickListener {
            notificStatus = !notificStatus
            sharedPref.edit().putBoolean(AppUtils.NOTIFICATION_STATUS_KEY, notificStatus).apply()
            if (notificStatus) {
                binding.btnNotific.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_bell))
                Snackbar.make(it, "Notification Enabled..", Snackbar.LENGTH_SHORT).show()
                alarm.setAlarm(
                    this,
                    sharedPref.getInt(AppUtils.NOTIFICATION_FREQUENCY_KEY, 30).toLong()
                )
            } else {
                binding.btnNotific.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_bell_disabled))
                Snackbar.make(it, "Notification Disabled..", Snackbar.LENGTH_SHORT).show()
                alarm.cancelAlarm(this)
            }
        }

        binding.btnStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }


        binding.op50ml.setOnClickListener {
            if (snackbar != null) {
                snackbar?.dismiss()
            }
            selectedOption = 50
            binding.op50ml.background = AppCompatResources.getDrawable(this, R.drawable.option_select_bg)
            binding.op100ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op150ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op200ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op250ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.opCustom.background = AppCompatResources.getDrawable(this, outValue.resourceId)

        }

        binding.op100ml.setOnClickListener {
            if (snackbar != null) {
                snackbar?.dismiss()
            }
            selectedOption = 100
            binding.op50ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op100ml.background = AppCompatResources.getDrawable(this, R.drawable.option_select_bg)
            binding.op150ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op200ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op250ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.opCustom.background = AppCompatResources.getDrawable(this, outValue.resourceId)

        }

        binding.op150ml.setOnClickListener {
            if (snackbar != null) {
                snackbar?.dismiss()
            }
            selectedOption = 150
            binding.op50ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op100ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op150ml.background = AppCompatResources.getDrawable(this, R.drawable.option_select_bg)
            binding.op200ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op250ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.opCustom.background = AppCompatResources.getDrawable(this, outValue.resourceId)

        }

        binding.op200ml.setOnClickListener {
            if (snackbar != null) {
                snackbar?.dismiss()
            }
            selectedOption = 200
            binding.op50ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op100ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op150ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op200ml.background = AppCompatResources.getDrawable(this, R.drawable.option_select_bg)
            binding.op250ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.opCustom.background = AppCompatResources.getDrawable(this, outValue.resourceId)

        }

        binding.op250ml.setOnClickListener {
            if (snackbar != null) {
                snackbar?.dismiss()
            }
            selectedOption = 250
            binding.op50ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op100ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op150ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op200ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op250ml.background = AppCompatResources.getDrawable(this, R.drawable.option_select_bg)
            binding.opCustom.background = AppCompatResources.getDrawable(this, outValue.resourceId)

        }

        binding.opCustom.setOnClickListener {
            if (snackbar != null) {
                snackbar?.dismiss()
            }

            val li = LayoutInflater.from(this)
            val promptsView = li.inflate(R.layout.custom_input_dialog, null)

            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setView(promptsView)

            val userInput = promptsView
                .findViewById(R.id.etCustomInput) as TextInputLayout

            alertDialogBuilder.setPositiveButton("OK") { _, _ ->
                val inputText = userInput.editText!!.text.toString()
                if (!TextUtils.isEmpty(inputText)) {
                    binding.tvCustom.text = "$inputText ml"
                    selectedOption = inputText.toInt()
                }
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()

            binding.op50ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op100ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op150ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op200ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.op250ml.background = AppCompatResources.getDrawable(this, outValue.resourceId)
            binding.opCustom.background = AppCompatResources.getDrawable(this, R.drawable.option_select_bg)

        }

    }


    @SuppressLint("SetTextI18n")
    private fun setWaterLevel(inTook: Int, totalIntake: Int) {

        YoYo.with(Techniques.SlideInDown)
            .duration(500)
            .playOn(binding.tvIntook)
        binding.tvIntook.text = "$inTook"
        binding.tvTotalIntake.text = "/$totalIntake ml"
        val progress = ((inTook / totalIntake.toFloat()) * 100).toInt()
        YoYo.with(Techniques.Pulse)
            .duration(500)
            .playOn(binding.intakeProgress)
        binding.intakeProgress.currentProgress = progress
        if ((inTook * 100 / totalIntake) > 140) {
            Snackbar.make(binding.mainActivityParent
                , "You achieved the goal", Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Snackbar.make(
            this.window.decorView.findViewById(android.R.id.content),
            "Please click BACK again to exit",
            Snackbar.LENGTH_SHORT
        ).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 1000)
    }

}
