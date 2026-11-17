# Saha Operasyon – Android uygulaması (WebView)

Panel (http://gorkemcakir.net.tr/) uygulama içinde açılır. Giriş, yetkiler, veriler sunucudan gelir;
panelde yaptığınız her güncelleme APK'yı yeniden derlemeden uygulamaya yansır.

## APK'yı derleme (Android Studio gerekmez) – GitHub Actions
1. github.com'da ücretsiz hesap açın, **New repository** → ad: `saha-operasyon-app` → **Private** → Create.
2. Repo sayfasında **uploading an existing file** → bu klasörün İÇİNDEKİ her şeyi (app, gradle, .github, settings.gradle ...)
   sürükleyip bırakın → Commit. (.github klasörü görünmüyorsa zip'i bilgisayarda açıp klasörleri tek tek sürükleyin.)
3. **Actions** sekmesi → "APK derle" → **Run workflow**. ~4-6 dk sonra yeşil olur.
4. Çalışmanın içine girin → en altta **Artifacts → SahaOperasyon-apk** indirin → zip içinden `app-debug.apk`.

## Telefona kurulum (acente / yönetici)
1. APK'yı telefona gönderin (WhatsApp/Drive/e-posta/USB).
2. Dosyaya dokunun → "Bilinmeyen kaynaklardan yüklemeye izin ver" sorarsa bu uygulama için izin verin → Yükle.
3. "Saha Operasyon" simgesine dokunup kullanıcı adı/parola ile girin.
Play Protect uyarısı çıkabilir ("Yine de yükle"): uygulama Play Store dışı ve debug imzalı olduğu için normaldir.

## Güncelleme gerekirse
- Panel içeriği: hiçbir şey yapmayın, otomatik güncel.
- Adres/SSL: `app/build.gradle` içindeki BASE_URL'yi `https://...` yapın, `network_security_config.xml` içindeki domain-config bölümünü silin, tekrar derleyin; telefonlara yeni APK'yı yükleyin (üstüne kurulur).

## Bilinen sınır
- Admin → toplu kullanıcı CSV'si (POST ile indirme) uygulamada çalışmaz; yönetici işlerini bilgisayarda yapın.
- HTTP olduğu için parolalar şifresiz gider; mobil veride de aynı. SSL kurmanız önerilir.
