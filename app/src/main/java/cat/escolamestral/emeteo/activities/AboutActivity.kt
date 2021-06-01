package cat.escolamestral.emeteo.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import cat.escolamestral.emeteo.R
import cat.escolamestral.emeteo.databinding.ActivityAboutBinding
import cat.escolamestral.emeteo.utils.isDarkThemeOn
import com.google.android.material.snackbar.Snackbar
import de.psdev.licensesdialog.LicensesDialog

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        title = getString(R.string.about)

        binding.buttonRate.setOnClickListener {
            val url =
                "https://play.google.com/store/apps/details?id=cat.escolamestral.emeteo"
            val a = Intent(Intent.ACTION_VIEW)
            a.data = Uri.parse(url)
            startActivity(a)
        }

        binding.buttonWebpage.setOnClickListener {
            val url = "https://www.escolamestral.cat/meteo"
            val a = Intent(Intent.ACTION_VIEW)
            a.data = Uri.parse(url)
            startActivity(a)
        }

        binding.buttonContact.setOnClickListener {
            val i = Intent(Intent.ACTION_SENDTO)
            i.data = Uri.parse("mailto:" + Uri.encode("emeteo@escolamestral.cat"))
            try {
                startActivity(Intent.createChooser(i, getString(R.string.send_email_using)))
            } catch (ex: ActivityNotFoundException) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.no_email_client_installed),
                    Snackbar.LENGTH_SHORT
                ).show()
            }

        }

        binding.buttonLicenses.setOnClickListener {
            LicensesDialog.Builder(this)
                .setNotices(R.raw.notices)
                .setEnableDarkMode(isDarkThemeOn())
                .build()
                .show()
        }

        binding.buttonGithub.setOnClickListener {
            val url =
                "https://github.com/hegocre/EMeteo"
            val a = Intent(Intent.ACTION_VIEW)
            a.data = Uri.parse(url)
            startActivity(a)
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }
}
