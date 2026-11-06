# Weather-chan

Türkçe ve İngilizce çalışan bir hava durumu uygulaması. Şehirlerin anlık
havasını, saatlik ve günlük tahminlerini gösterir; ekranın üstünde yaşayan
**Weather-chan** karakteri havaya göre poz değiştirir ve tamamen çevrimdışı
bir tavsiye motorundan gelen kısa, samimi öneriler sunar.

## Özellikler

- Çoklu şehir listesi, şehir arama ve konumdan ekleme
- Anlık hava, saatlik grafik ve 7 günlük tahmin
- Konumdan otomatik şehir bulma
- TR/EN arayüz, °C/°F ve açık/koyu tema
- Havaya göre değişen arka plan paleti ve ambiyans animasyonları
- **Weather-chan**: yedi poz, dokunuşa tepki, kısa sesler ve havaya göre
  tavsiye balonu
- Ana ekran widget'ları: Weather-chan (2x2/4x2) ve karaktere yer vermeyen
  bilgi widget'ı; sıcaklık, durum, şehir ve saatlik şerit
- Yükleme, boş, hata ve yenileme durumlarının tamamı ele alınır

Weather-chan'in tavsiyeleri kural tabanlıdır; LLM, API veya ağ bağlantısı
kullanmaz. Metinler TR/EN kaynaklardan gelir, seçim deterministiktir.

## Teknoloji

Kotlin · Jetpack Compose (Material 3) · Clean Architecture · Coroutines &
StateFlow · Hilt · Retrofit · DataStore · Navigation Compose · WorkManager ·
Glance (widget)

## Veri kaynağı

Birincil kaynak [Open-Meteo](https://open-meteo.com/)'dur ve API anahtarı
gerektirmez. Birincil kaynak başarısız olursa `wttr.in` yedeği devreye girer;
yedekte gece/gündüz, gözlem saati ile gün doğumu/batımından hesaplanır.

## Çalıştırma

```bash
./gradlew assembleDebug        # debug APK
./gradlew installDebug         # bağlı cihaza/emülatöre kur
./gradlew test                 # birim testleri
./gradlew spotlessCheck        # biçim kontrolü (düzeltme: spotlessApply)
```

Enstrüman testleri için cihaz/emülatör açıkken:

```bash
./gradlew installDebugAndroidTest
adb shell am instrument -w \
  io.github.rojhatince47.weatherchan.test/androidx.test.runner.AndroidJUnitRunner
```

## Mimari

Katmanlar `core`, `feature` ve `app` altında ayrılır:

- `domain`: saf Kotlin modeller, kurallar ve tavsiye motoru
- `data`: Open-Meteo/wttr.in kaynakları, DataStore önbelleği ve tercihleri
- `presentation`: Compose ekranları, ViewModel'lar ve UI modelleri

Ayrıntılar: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Proje notları

- Gerçek karakter görselleri ve sesleri geldiğinde yalnız
  `CompanionArtAssets.overrides` ve `CompanionSoundAssets.tapClips`
  güncellenir; kodun geri kalanı değişmez.

## Lisans

Bu proje **tüm hakları saklı** olarak yayınlanmıştır. Kaynak kodu
görüntülenebilir; ancak önceden yazılı izin olmadan kopyalanamaz,
değiştirilemez, dağıtılamaz veya ticari olarak kullanılamaz.

Weather-chan adı, karakter tasarımı ve ileride eklenecek görsel/ses
varlıkları da aynı koşullara tabidir.

Ayrıntılar: [LICENSE](LICENSE)
