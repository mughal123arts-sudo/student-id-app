# Student ID Collector (Android App)

School students ka data collect karne ke liye ek simple Android app — Kotlin,
Jetpack Compose, aur Room (local database) se bani hai. 50–500 students ka
data aasani se handle karti hai, koi internet ki zaroorat nahi.

## Features
- **Admin Login screen** — app khulte hi login karna zaroori hai
  (default: username `admin`, password `admin123` — code mein
  `AdminLoginScreen.kt` ke top par `ADMIN_USERNAME` / `ADMIN_PASSWORD`
  change kar sakte hain)
- Student add/edit form: Student Name, Father Name, Class,
  Roll Number / ID Number, Contact/Mobile Number, aur Photo
- Photo **camera se turant li ja sakti hai** ya gallery se select
  (tap on "Take Photo / Choose from Gallery")
- Search bar — naam, roll no, ya class se filter
- Sab data local phone storage mein save hota hai (Room database) — offline
  kaam karta hai
- One-tap "Export & Share as CSV" — data Excel/Google Sheets mein khulne
  layak CSV banata hai aur WhatsApp/Email se share kar sakte hain

## Kaise chalayein
1. [Android Studio](https://developer.android.com/studio) install karein
   (agar pehle se nahi hai)
2. Is poore `StudentIDApp` folder ko Android Studio mein **File > Open**
   se open karein
3. Gradle sync hone dein (pehli baar thoda time lagega, internet chahiye
   dependencies download karne ke liye)
4. Ek Android phone/emulator connect karein aur ▶️ Run dabayein

## Login credentials badalna
`app/src/main/java/com/school/studentid/ui/AdminLoginScreen.kt` file kholein
aur `ADMIN_USERNAME` / `ADMIN_PASSWORD` values apni pasand ki daal dein.
(Ye simple on-device check hai — agar future mein alag-alag admins ke
separate accounts chahiye hon, toh isay proper backend se jodna hoga.)

## APK banana (bina Android Studio install kiye) — GitHub Actions se
Is project mein ek automatic build workflow already add hai
(`.github/workflows/build-apk.yml`). Bas ye steps follow karein:

1. [github.com](https://github.com) pe free account banayein (agar nahi hai)
2. Ek naya **New repository** banayein (Public ya Private, koi bhi)
3. Is poore `StudentIDApp` folder ka content us repository mein upload
   kar dein (GitHub website par **"uploading an existing file"** link se
   sara folder drag-and-drop kar sakte hain, ya `git push` se)
4. Repository ke **Actions** tab mein jaayein — "Build APK" workflow
   khud-ba-khud chalna shuru ho jayega (agar na chale to **Run workflow**
   button dabayein)
5. 3-5 minute wait karein — jab green ✅ tick aa jaye, us workflow run
   par click karein
6. Neeche **Artifacts** section mein "StudentIDApp-debug-apk" milega —
   usay download kar lein (ye ek zip hogi jisme APK file hogi)
7. Zip se APK nikal kar apne phone mein bhej dein, aur install kar lein
   ("unknown sources" allow karna hoga install karte waqt)

Har baar jab bhi code mein koi change karke push karenge, naya APK
automatically ban jayega.

## Data kahan save hota hai
Student records phone ke andar Room (SQLite) database mein save hote hain —
app uninstall karne tak data safe rehta hai. CSV export files
`Android/data/com.school.studentid/files/exports/` folder mein banti hain.

## Aage kya add kar sakte hain (future ideas)
- Actual printable ID card layout (photo + QR code ke saath PDF banana)
- Bulk import students CSV/Excel se
- Cloud backup taake multiple devices se access ho sake
- Class-wise ya section-wise grouping/filter

Koi bhi cheez add/change karni ho, bata dijiyega.
