package org.nbschool.appmanager

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val cancelled = AtomicBoolean(false)
    private var busy = false
    private var pending: CatalogApp? = null
    private var installing: CatalogApp? = null
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private lateinit var refresh: Button
    private lateinit var install: Button
    private lateinit var cancel: Button
    private lateinit var progress: ProgressBar
    private lateinit var address: EditText
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 36, 24, 24) }
        @Suppress("DEPRECATION")
        val ownVersion = packageManager.getPackageInfo(packageName, 0).versionName
        root.addView(TextView(this).apply { text = "School App Manager $ownVersion"; textSize = 24f })
        address = EditText(this).apply {
            hint = "HTTPS-адреса JSON-каталогу"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(getPreferences(MODE_PRIVATE).getString("catalogUrl", "https://raw.githubusercontent.com/nbschool2026/School-App-Manager/main/catalog/catalog.json"))
        }; root.addView(address)
        refresh = Button(this).apply { text = "Перевірити застосунки"; setOnClickListener { load() } }; root.addView(refresh)
        status = TextView(this); root.addView(status)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { visibility = View.GONE }; root.addView(progress)
        cancel = Button(this).apply { text = "Скасувати завантаження"; visibility = View.GONE; setOnClickListener { cancelled.set(true); isEnabled = false; status.text = "Скасування…" } }; root.addView(cancel)
        install = Button(this).apply { text = "Встановити перевірений APK"; visibility = View.GONE; setOnClickListener { prepareInstall() } }; root.addView(install)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        installing = saved?.getString("pkg")?.let { CatalogApp(it, it, saved.getLong("version"), "", "", "", "") }
    }
    private fun ui(action: () -> Unit) { runOnUiThread { if (!isDestroyed) action() } }
    @Suppress("DEPRECATION")
    private fun installedVersion(pkg: String): Long? = try { packageManager.getPackageInfo(pkg, 0).longVersionCode } catch (_: PackageManager.NameNotFoundException) { null }
    private fun setBusy(value: Boolean) { busy = value; refresh.isEnabled = !value; address.isEnabled = !value; install.isEnabled = !value }
    private fun load() {
        if (busy) return
        pending = null; install.visibility = View.GONE
        val url = address.text.toString().trim(); getPreferences(MODE_PRIVATE).edit().putString("catalogUrl", url).apply()
        setBusy(true); status.text = "Завантаження каталогу…"; list.removeAllViews()
        worker.execute {
            val result = runCatching { Catalog.load(url).map { app -> val v = installedVersion(app.packageName); Triple(app, v, compareVersion(v, app.versionCode)) } }
            ui {
                setBusy(false)
                result.fold(onSuccess = { rows ->
                    status.text = "Перевірено: ${rows.size}. Потребують встановлення/оновлення: ${rows.count { it.third != AppStatus.CURRENT }}"
                    rows.forEach { (app, version, state) ->
                        list.addView(TextView(this).apply { text = "${app.name}\n${app.packageName}\nУ каталозі: ${app.versionName} (${app.versionCode})\nНа планшеті: ${version ?: "немає"}\n${state.label}"; textSize = 17f; setPadding(0,20,0,8) })
                        if (state != AppStatus.CURRENT) list.addView(Button(this).apply { text = "Завантажити: ${app.name}"; setOnClickListener { if (!busy) download(app) } })
                    }
                }, onFailure = { status.text = "Помилка каталогу: ${it.message}" })
            }
        }
    }
    private fun download(app: CatalogApp) {
        if (compareVersion(installedVersion(app.packageName), app.versionCode) == AppStatus.CURRENT) { load(); return }
        pending = null; install.visibility = View.GONE; cancelled.set(false); setBusy(true)
        progress.visibility = View.VISIBLE; progress.isIndeterminate = true; cancel.visibility = View.VISIBLE; cancel.isEnabled = true
        status.text = "Завантаження ${app.name}…"
        worker.execute {
            val result = runCatching { ApkTransfer.download(this, app, cancelled) { bytes, total -> ui {
                progress.isIndeterminate = total <= 0; if (total > 0) progress.progress = (100 * bytes / total).toInt()
                status.text = "Завантажено ${bytes / 1024} КБ" + if (total > 0) " / ${total / 1024} КБ" else ""
            } } }
            ui {
                setBusy(false); progress.visibility = View.GONE; cancel.visibility = View.GONE
                result.fold(onSuccess = { pending = app; install.visibility = View.VISIBLE; status.text = "${app.name}: SHA-256, підпис і сумісність перевірено. Натисніть «Встановити перевірений APK»." },
                    onFailure = { status.text = "${if (cancelled.get()) "Скасовано" else "Помилка APK"}: ${it.message}" })
            }
        }
    }
    @Suppress("DEPRECATION")
    private fun prepareInstall() {
        val app = pending ?: return
        if (busy) return
        if (!packageManager.canRequestPackageInstalls()) {
            android.app.AlertDialog.Builder(this).setTitle("Дозвіл на встановлення")
                .setMessage("У налаштуваннях Android дозвольте встановлення через School App Manager, поверніться сюди й знову натисніть «Встановити».")
                .setPositiveButton("Налаштування") { _, _ -> try { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))) } catch (e: Exception) { status.text = "Помилка: ${e.message}" } }
                .setNegativeButton("Скасувати", null).show(); return
        }
        setBusy(true); status.text = "Повторна перевірка перед встановленням…"
        worker.execute {
            val result = runCatching { ApkTransfer.verify(this, File(cacheDir,"verified.apk"), app) }
            ui { result.fold(onSuccess = {
                val uri = Uri.parse("content://org.nbschool.appmanager.apk/verified.apk")
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply { setDataAndType(uri,"application/vnd.android.package-archive"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); clipData = ClipData.newRawUri("APK",uri); putExtra(Intent.EXTRA_RETURN_RESULT,true) }
                try { installing = app; startActivityForResult(intent,100); status.text = "Підтвердьте встановлення у вікні Android." }
                catch (e: Exception) { installing = null; setBusy(false); status.text = "Помилка встановлення: ${e.message}" }
            }, onFailure = { setBusy(false); pending = null; install.visibility = View.GONE; status.text = "Встановлення заблоковано: ${it.message}" }) }
        }
    }
    @Deprecated("Android 9 activity result")
    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request,result,data)
        if (request != 100) return
        val app = installing; installing = null; pending = null; setBusy(false); install.visibility = View.GONE
        val version = app?.let { installedVersion(it.packageName) }
        status.text = if (app != null && version != null && version >= app.versionCode) "Встановлено версію $version. Натисніть «Перевірити застосунки» для оновлення списку."
            else if (result == RESULT_CANCELED) "Встановлення скасовано або не завершено. Повторіть перевірку списку."
            else "Android не встановив APK. Перевірте вільне місце та повідомлення системи."
        revokeUriPermission(Uri.parse("content://org.nbschool.appmanager.apk/verified.apk"),Intent.FLAG_GRANT_READ_URI_PERMISSION)
        File(cacheDir,"verified.apk").delete()
    }
    override fun onSaveInstanceState(out: Bundle) { installing?.let { out.putString("pkg",it.packageName); out.putLong("version",it.versionCode) }; super.onSaveInstanceState(out) }
    override fun onDestroy() { cancelled.set(true); worker.shutdownNow(); super.onDestroy() }
}
