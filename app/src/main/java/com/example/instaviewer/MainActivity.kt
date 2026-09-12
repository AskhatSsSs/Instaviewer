package com.example.instaviewer

import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var history: HistoryStore
    private lateinit var etUsername: AutoCompleteTextView
    private lateinit var etServiceUrl: EditText
    private lateinit var spinnerMode: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        history = HistoryStore(this)
        AppCompatDelegate.setDefaultNightMode(
            if (history.darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etUsername = findViewById(R.id.etUsername)
        etServiceUrl = findViewById(R.id.etServiceUrl)
        spinnerMode = findViewById(R.id.spinnerMode)
        webView = findViewById(R.id.webView)
        val btnOpen = findViewById<Button>(R.id.btnOpen)
        val btnClear = findViewById<Button>(R.id.btnClearHistory)
        val switchDark = findViewById<Switch>(R.id.switchDark)

        val modes = listOf(
            "Без входа (профиль и посты)",
            "Сторис через сторонний сервис",
            "Свой отдельный аккаунт"
        )
        spinnerMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        spinnerMode.setSelection(history.viewMode)
        etServiceUrl.setText(history.serviceUrlTemplate)
        updateModeUi(history.viewMode)
        setupWebView()
        refreshHistoryAdapter()

        spinnerMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                history.viewMode = pos
                updateModeUi(pos)
                setupWebView()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        switchDark.isChecked = history.darkMode
        switchDark.setOnCheckedChangeListener { _, checked ->
            history.darkMode = checked
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        btnOpen.setOnClickListener { openProfile() }
        etUsername.setOnItemClickListener { _, _, _, _ -> openProfile() }
        btnClear.setOnClickListener {
            history.clear()
            refreshHistoryAdapter()
            Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateModeUi(mode: Int) {
        etServiceUrl.visibility =
            if (mode == HistoryStore.MODE_SERVICE) View.VISIBLE else View.GONE
    }

    private fun setupWebView() {
        val cookies = CookieManager.getInstance()
        val useAccount = history.viewMode == HistoryStore.MODE_ACCOUNT
        cookies.setAcceptCookie(useAccount)
        if (!useAccount) cookies.removeAllCookies(null)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = useAccount
            cacheMode = if (useAccount) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_NO_CACHE
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
        }
        webView.webViewClient = WebViewClient()
    }

    private fun openProfile() {
        val username = etUsername.text.toString().trim().removePrefix("@")
        if (username.isEmpty()) {
            Toast.makeText(this, "Введите имя пользователя", Toast.LENGTH_SHORT).show()
            return
        }
        history.add(username)
        refreshHistoryAdapter()
        etUsername.dismissDropDown()

        val url = when (history.viewMode) {
            HistoryStore.MODE_SERVICE -> {
                val template = etServiceUrl.text.toString().trim()
                history.serviceUrlTemplate = template
                if (!template.contains("{user}")) {
                    Toast.makeText(this, "В шаблоне должен быть {user}", Toast.LENGTH_LONG).show()
                    return
                }
                template.replace("{user}", username)
            }
            HistoryStore.MODE_ACCOUNT -> "https://www.instagram.com/stories/$username/"
            else -> "https://www.instagram.com/$username/"
        }
        webView.loadUrl(url)
    }

    private fun refreshHistoryAdapter() {
        etUsername.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, history.getHistory())
        )
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (history.viewMode != HistoryStore.MODE_ACCOUNT) {
            webView.clearCache(true)
            webView.clearHistory()
            CookieManager.getInstance().removeAllCookies(null)
        }
        super.onDestroy()
    }
}
