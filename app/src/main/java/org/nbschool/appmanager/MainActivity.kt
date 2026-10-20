package org.nbschool.appmanager

import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.*
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private lateinit var refresh: Button
    private lateinit var address: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 36, 24, 24) }
        root.addView(TextView(this).apply { text = "School App Manager"; textSize = 24f })
        root.addView(TextView(this).apply { text = "Етап 1: перевірка каталогу. Встановлення буде додано наступним кроком." })
        address = EditText(this).apply {
            hint = "HTTPS-адреса JSON-каталогу"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(getPreferences(MODE_PRIVATE).getString("catalogUrl", ""))
        }
        root.addView(address)
        refresh = Button(this).apply { text = "Перевірити застосунки"; setOnClickListener { load() } }
        root.addView(refresh)
        status = TextView(this); root.addView(status)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun load() {
        val url = address.text.toString().trim()
        getPreferences(MODE_PRIVATE).edit().putString("catalogUrl", url).apply()
        refresh.isEnabled = false; status.text = "Завантаження каталогу…"; list.removeAllViews()
        worker.execute {
            val result = runCatching {
                Catalog.load(url).map { app ->
                    val installed = try {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(app.packageName, 0).longVersionCode
                    } catch (_: PackageManager.NameNotFoundException) { null }
                    Triple(app, installed, compareVersion(installed, app.versionCode))
                }
            }
            runOnUiThread {
                if (!isDestroyed) {
                    refresh.isEnabled = true
                    result.fold(onSuccess = { rows ->
                        status.text = "Перевірено: ${rows.size}. Потребують встановлення/оновлення: ${rows.count { it.third != AppStatus.CURRENT }}"
                        rows.forEach { (app, installed, state) ->
                            list.addView(TextView(this).apply {
                                text = "${app.name}\n${app.packageName}\nУ каталозі: ${app.versionName} (${app.versionCode})\nНа планшеті: ${installed ?: "немає"}\n${state.label}"
                                textSize = 17f; setPadding(0, 20, 0, 20)
                            })
                        }
                    }, onFailure = { status.text = "Помилка: ${it.message ?: "Не вдалося прочитати каталог"}" })
                }
            }
        }
    }

    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }
}
