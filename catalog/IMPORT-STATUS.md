# Стан додавання всього списку — 20.10.2026

## У робочому catalog.json: 4 записи

- School App Manager 0.2.2 — тест оновлення менеджера.
- Windows Learning Desktop 0.1.8 — користувач підтвердив встановлення через менеджер.
- BlockDrop 1.0.69, versionCode 662 — завантажено з F-Droid; SHA-256, криптографічний підпис для Android 9, minSdk 24 та ARM64 перевірено.
- Шахи 10.6.0, versionCode 298, packageName jwtc.android.chess — завантажено з F-Droid; SHA-256, підпис для Android 9, minSdk 24 та ARM64 перевірено.

## APK отримано, але в робочий каталог ще не додано

- Google Sheets 1.25.501.00.90, versionCode 220666337 — 107306389 байт, minSdk 26, ARM64, Google signer SHA-256 3d7a1223019aa39d9ea0e3436ab7c0896bfb4fb679f4de5fe7c23f326c8f994a. SHA-256 APK aabfa3af10eee376934db753486631d3ac4191127d0b6153b0b1d9d68ccbcd7e.
- Google Slides 1.25.391.01.90, versionCode 220658844 — 155808054 байт, minSdk 26, ARM64, той самий Google signer. SHA-256 APK 897d9b2a323bf688d185d174516310cde77d02248b201d2444af62b95dd46d01.

Обидва підписи успішно перевірено apksigner для API 28. Файли збережено локально поза Git у папці outputs/Verified-APKs поруч із каталогом проєкту. Поточне джерело видає тимчасові URL, які не можна зберігати як постійні посилання каталогу. Кожний APK перевищує ліміт звичайного GitHub-файлу 100 MiB. Потрібне постійне сховище (наприклад GitHub Release). Поточний менеджер 0.2.2 відхиляє перенаправлення, тому перед використанням Release URLs потрібно додати обмежену підтримку HTTPS redirects і зібрати менеджер.

Джерела отриманих файлів:
- https://apk.cafe/download?file_id=3390769/google-sheets
- https://apk.cafe/download?file_id=3326890/google-slides

Зіставлені сторінки потрібних версій:
- https://www.apkmirror.com/apk/google-inc/sheets/google-sheets-1-25-501-00-release/google-sheets-1-25-501-00-90-android-apk-download/
- https://www.apkmirror.com/apk/google-inc/slides/google-slides-1-25-391-01-release/google-slides-1-25-391-01-90-2-android-apk-download/

## Файли точних версій ще не отримано

- Google Docs 1.19.372.08.40 — знайдена сторінка потрібного ARM64 APK; завантаження не підтверджено.
  https://www.apkmirror.com/apk/google-inc/docs/docs-1-19-372-08-release/google-docs-1-19-372-08-40-android-apk-download/
- Google Meet 2024.06.02.643059695.Release — знайдена потрібна версія; доступний на альтернативному джерелі файл 2024.06.02.639595590.Release не підставлявся замість неї.
  https://www.apkmirror.com/apk/google-inc/hangouts-meet/
- Viber 22.4.3.0 — знайдена потрібна ARM64 версія; альтернативне джерело не відповіло в межах таймауту.
  https://www.apkmirror.com/apk/viber-media-s-a-r-l/viber/viber-22-4-3-0-release/rakuten-viber-messenger-22-4-3-0-4-2-android-apk-download/
- YouTube 21.23.485 — знайдено окремий universal APK для Android 9; у доступному альтернативному джерелі є 21.23.483, заміну не зроблено.
  https://www.apkmirror.com/apk/google-inc/youtube/youtube-21-23-485-release/youtube-21-23-485-android-apk-download/
- Gmail 2019.06.09.254811277.release — знайдена потрібна версія, але файл не отримано. Існують різні підписи, тому сертифікат потрібно звірити з установленим на планшеті застосунком.
  https://www.apkmirror.com/apk/google-inc/gmail/gmail-2019-06-09-254811277-release-release/gmail-2019-06-09-254811277-release-2-android-apk-download/

Запити до APKMirror із командного середовища повернули HTTP 403. У звичайному браузері сторінка відкрилася, проте завантаження не завершилося за час доступного очікування. Наявність сторінки не підтверджує отримання/перевірку APK. Для продовження можна використати оригінальні APK, які вже працювали на планшеті.

Google Drive — окремий системний застосунок. Потрібні встановлені versionCode та сертифікат; не обирати довільну версію для оновлення.
