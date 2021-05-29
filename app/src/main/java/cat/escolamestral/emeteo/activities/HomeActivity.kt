package cat.escolamestral.emeteo.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import cat.escolamestral.emeteo.R
import cat.escolamestral.emeteo.databinding.ActivityHomeBinding
import cat.escolamestral.emeteo.utils.ContextUtils
import cat.escolamestral.emeteo.utils.PreferencesManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.nameRes
import java.util.*

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding

    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    private var fragments: Array<Fragment?> = arrayOfNulls(FRAGMENT_COUNT)

    private var showingFragment = WEATHER_FRAGMENT

    private var liveViewDialog: AlertDialog? = null
    private var dialogShowing = false

    private val startSettingsForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    if (intent.getBooleanExtra("language_changed", false)
                        || intent.getBooleanExtra("units_changed", false)
                    ) {
                        binding.root.closeDrawer(binding.slider)
                        recreate()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        //Set theme to hide splash screen
        setTheme(R.style.Theme_EMeteo)

        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = PreferencesManager.getPreferencesInstance(this)
        AppCompatDelegate.setDefaultNightMode(prefs.getAppTheme())

        val fragmentManager: FragmentManager = supportFragmentManager
        fragments[WEATHER_FRAGMENT] = fragmentManager.findFragmentById(R.id.weather_fragment)
        fragments[CHART_FRAGMENT] = fragmentManager.findFragmentById(R.id.charts_fragment)

        val transaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragments[CHART_FRAGMENT]?.let { transaction.hide(it) }
        fragments[WEATHER_FRAGMENT]?.let { transaction.hide(it) }
        transaction.commit()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        actionBarDrawerToggle = ActionBarDrawerToggle(
            this, binding.root, binding.toolbar,
            com.mikepenz.materialdrawer.R.string.material_drawer_open,
            com.mikepenz.materialdrawer.R.string.material_drawer_close
        )

        dialogShowing = savedInstanceState?.getBoolean("dialog_showing", false) ?: false
        if (dialogShowing)
            loadLiveImage()

        createDrawer(savedInstanceState)
    }

    private fun createDrawer(savedInstanceState: Bundle?) {
        //Create ColorStateList for app title in drawer
        val states = arrayOf(
            intArrayOf(-android.R.attr.state_enabled)
        )
        val colors =
            if (ContextUtils.isDarkThemeOn(this)) intArrayOf(Color.WHITE) else intArrayOf(Color.BLACK)

        //Outline provider for appbar, used to draw shadow. To remove shadow on charts fragment,
        //we set it's provider to null, and to restore shadow we restore this state
        val oldOutlineProvider = binding.appBar.outlineProvider

        binding.slider.apply {
            itemAdapter.add(
                PrimaryDrawerItem().apply {
                    isSelectable = false; isEnabled = false
                    nameRes = R.string.app_name; textColor = ColorStateList(states, colors)
                },
                DividerDrawerItem(),
                PrimaryDrawerItem().apply {
                    nameRes = R.string.weather; identifier = 1
                    icon = ImageHolder(R.drawable.ic_cloud_sun_bold)
                    isIconTinted = true
                },
                PrimaryDrawerItem().apply {
                    nameRes = R.string.resumes; identifier = 2
                    icon = ImageHolder(R.drawable.ic_chart_line_bold)
                    isIconTinted = true
                },
                PrimaryDrawerItem().apply {
                    nameRes = R.string.finder; identifier = 3
                    isSelectable = false; icon = ImageHolder(R.drawable.ic_magnifying_glass_bold)
                    isIconTinted = true
                },
                PrimaryDrawerItem().apply {
                    nameRes = R.string.live_view; identifier = 4
                    isSelectable = false; icon = ImageHolder(R.drawable.ic_play_circle_bold)
                    isIconTinted = true
                },
                DividerDrawerItem(),
                PrimaryDrawerItem().apply {
                    nameRes = R.string.settings; identifier = 5
                    isSelectable = false; icon = ImageHolder(R.drawable.ic_gear_six_bold)
                    isIconTinted = true
                },
                PrimaryDrawerItem().apply {
                    nameRes = R.string.about; identifier = 6
                    isSelectable = false; icon = ImageHolder(R.drawable.ic_info_bold)
                    isIconTinted = true
                }
            )
            onDrawerItemClickListener = { _, drawerItem, _ ->
                when (drawerItem.identifier) {
                    1L -> {
                        supportActionBar?.title = getString(R.string.weather)
                        binding.appBar.setExpanded(true)
                        showFragment(WEATHER_FRAGMENT)
                        binding.appBar.outlineProvider = oldOutlineProvider
                    }
                    2L -> {
                        supportActionBar?.title = getString(R.string.resumes)
                        showFragment(CHART_FRAGMENT)
                        binding.appBar.outlineProvider = null
                    }
                    3L -> showDatePickerDialog()
                    4L -> loadLiveImage()
                    5L -> startSettingsForResult.launch(
                        Intent(
                            this@HomeActivity,
                            SettingsActivity::class.java
                        )
                    )
                    6L -> startActivity(Intent(this@HomeActivity, AboutActivity::class.java))
                }
                false
            }
            multiSelect = false
        }

        actionBarDrawerToggle.isDrawerSlideAnimationEnabled = true

        if (savedInstanceState == null) {
            binding.slider.setSelection(1, true)
        } else {
            binding.slider.setSavedInstance(savedInstanceState)
            binding.slider.setSelection(
                savedInstanceState.getInt(
                    "showing_fragment",
                    WEATHER_FRAGMENT
                ).toLong() + 1, true
            )
        }
    }

    private fun showFragment(fragmentI: Int) {
        showingFragment = fragmentI
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        for (i in fragments.indices) {
            if (i == fragmentI)
                fragments[i]?.let { transaction.show(it) }
            else
                fragments[i]?.let { transaction.hide(it) }
        }
        transaction.commit()
    }

    override fun onBackPressed() {
        if (binding.root.isDrawerOpen(binding.slider))
            binding.root.closeDrawer(binding.slider)
        else
            super.onBackPressed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        actionBarDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        actionBarDrawerToggle.syncState()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (actionBarDrawerToggle.onOptionsItemSelected(item))
            return true
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("showing_fragment", showingFragment)
        outState.putBoolean("dialog_showing", dialogShowing)
        binding.slider.saveInstanceState(outState)
    }

    private fun showDatePickerDialog() {
        val minCalendar = Calendar.getInstance()
        minCalendar.set(Calendar.YEAR, 2007)
        minCalendar.set(Calendar.MONTH, 0)
        minCalendar.set(Calendar.DATE, 1)
        val nowCalendar = Calendar.getInstance()

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val intent = Intent(this@HomeActivity, FinderActivity::class.java)
            intent.putExtra("year", year)
            intent.putExtra("month", month + 1) //January = 0
            intent.putExtra("dayOfMonth", dayOfMonth)
            startActivity(intent)
        }

        val datePickerDialog = DatePickerDialog(
            this@HomeActivity, dateSetListener,
            nowCalendar.get(Calendar.YEAR),
            nowCalendar.get(Calendar.MONTH),
            nowCalendar.get(Calendar.DATE)
        )

        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis
        datePickerDialog.datePicker.maxDate = nowCalendar.timeInMillis
        datePickerDialog.show()
        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
            .setTextColor(getAccentColor())
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
            .setTextColor(getAccentColor())
    }

    private fun Context.getAccentColor(): Int {
        val typedValue = TypedValue()
        val a = obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorAccent))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

    private fun loadLiveImage() {
        val builder = AlertDialog.Builder(this@HomeActivity)
        val prefs = PreferencesManager.getPreferencesInstance(this)

        val layout =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) R.layout.dialog_live_view
            else R.layout.dialog_live_view_landscape
        val customLayout = View.inflate(this@HomeActivity, layout, null)

        val playerView = customLayout.findViewById<PlayerView>(R.id.player_view)
        val loadingProgressBar = customLayout.findViewById<ProgressBar>(R.id.loading)

        val player = SimpleExoPlayer.Builder(this).build()
        player.setMediaItem(MediaItem.fromUri(prefs.getLiveViewUrl()))
        playerView.player = player

        player.playWhenReady = true
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying)
                    loadingProgressBar.visibility = View.GONE
                else
                    loadingProgressBar.visibility = View.VISIBLE

                super.onIsPlayingChanged(isPlaying)
            }
        })
        player.prepare()

        builder.setOnCancelListener {
            player.stop()
            dialogShowing = false
            liveViewDialog = null
        }

        builder.setView(customLayout)
        liveViewDialog = builder.create()

        dialogShowing = true
        liveViewDialog?.show()
        playerView.post {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                liveViewDialog?.window?.setLayout(playerView.width - 40, playerView.height)
            }
        }
    }

    override fun onDestroy() {
        liveViewDialog?.cancel()
        liveViewDialog = null
        super.onDestroy()
    }

    companion object {
        private const val WEATHER_FRAGMENT = 0
        private const val CHART_FRAGMENT = 1
        private const val FRAGMENT_COUNT = CHART_FRAGMENT + 1
    }
}