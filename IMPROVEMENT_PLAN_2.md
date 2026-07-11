# WhatNext İyileştirme Planı 2 — Tüm Scraper'ların Doğrulanması, Saat Verisi, Görsel/Reel Yenileme

> Bu plan 2026-07-11'de, Plan 1'in (IMPROVEMENT_PLAN.md, 4 faz tamamlandı) üzerine hazırlandı.
> Uygulamak için yeni bir Claude Code oturumunda: "IMPROVEMENT_PLAN_2.md dosyasını oku ve Faz 4'ü uygula" demeniz yeterli.
> Sıra kasıtlı: önce veri doğruluğu (parse), sonra veri zenginliği (saat), sonra sunum (görsel + Reel).
>
> **Durum (2026-07-11):** Faz 1 ✅, Faz 2 ✅ ve Faz 3 ✅ tamamlandı, `mvn test` yeşil (79/80; tek istisna
> `WhatnextApplicationTests.contextLoads` — bu ortamdan Postgres'e erişim yok, kod dışı bilinen kısıt).
> Faz 4 (Reel) sırada. Faz 3.1'de post/cover 4:5'e geçirilmedi — kullanıcı 1:1'de kalmayı seçti (kırpma
> köşe illüstrasyonlarını kesiyordu). Canlı scrape/health/DB doğrulaması, secrets rotate ve story'nin
> telefonda gözle onaylanması kullanıcının kendi ortamında yapılmalı (bkz. ilgili fazların "Doğrulama"
> bölümleri).

## Bağlam (Context)

Plan 1 yalnızca Bubilet'i canlı DOM'la doğrulayıp düzeltti. Yapısal olarak diğer üç scraper
(Biletix, Biletino, Biletinial) aynı kırılganlığı taşıyor ve **hiçbiri doğrulanmadı**:

- `BiletixScraperConfig.EVENT_CARD_SELECTOR = ".grid_21.alpha.omega.listevent.searchResultEvent"` gibi
  eski görünümlü seçiciler; site yenilemesinde sessizce 0 kart veya çöp veri üretir.
- Sağlık mekanizması (`ScrapeRunStats`, `ScrapeHealthService`, `GET /api/scraper/health`) **sadece
  BubiletScraper'da**; diğer kaynaklar bozulursa haber vermez.
- `whatnext.scraper.allowed-sources` boş → dört scraper da her koşuda çalışıyor; bozuk kaynak
  veri kalitesini sessizce bozuyor (Faz 3 dedup'ı çöp kaydı temizlemez, sadece aynı etkinliği eşler).
- Zamanlayıcı bug'ları: `EventSchedulerService` aylık cron'u `0 0 2 30 * ?` — **Şubat'ta ayın 30'u yok**,
  aylık scrape o ay hiç çalışmaz; bu sınıftaki cron'larda `zone` yok; `LocalDate.now()` çağrıları
  `TrClock`'a geçmemiş.
- **Saat verisi hiç yok**: `Event.startDate` sadece `LocalDate`. Bubilet liste satırı "17 Temmuz Cum
  21:00" saati içeriyor ama parser atıyor. Planlama içeriği üreten hesap için saat kritik.
- Görsel katman zayıf: story şablonları 1024×1536 (2:3) — Instagram story 9:16 ister, kenarlar
  kırpılıyor; post 1:1 (4:5 daha fazla alan kaplar); slaytta tarih/gün yok; story seçimi `shuffle`
  (skorlama varken rastgele); başlık sarma tahmini kaba (`pointsize × 0.58`).
- Reel: ilk 2,5 sn logo intro'suyla harcanıyor (retention'ın en pahalı anı); yapı gün değil kategori
  odaklı; etkinlik görseli 320×420 küçük kutu; kartlar eşit 3 sn (~31,5 sn toplam); `system-ui` fontu;
  Reel içi dedup birebir başlık+tarih (EventIdentity kullanılmıyor).

**Kapsam dışı (karar):** fiyat görsellere yazılmayacak (`minPriceTl` DB'de kalır, sunumda kullanılmaz).

**Fazlar:** 1 (parse doğrulama+sağlık) → 2 (saat) → 3 (statik görsel) → 4 (Reel) → 5 (karusel+ölçüm).
Faz 3 ve 4, Faz 2'den saati alır; Faz 1'den bağımsız başlayabilirler ama veri doğrulanmadan
sunuma emek vermek anlamsız — sıra korunmalı.

---

## Faz 1 — Tüm scraper'ların canlı doğrulanması + sağlık altyapısının genellenmesi

### 1.1 Sağlık altyapısını kaynak-bazlı yap
- `ScrapeRunStats` üretimi `BubiletScraper`'dan `BaseScraper`'a (veya ortak yardımcıya) taşınır:
  cardsSeen, parsed, parseFailed, missingVenue, missingDate, süre + kova başına 3 örnek başlık
  her kaynak için tutulur.
- `ScrapeHealthService`: tek "son koşu" yerine `Map<sourceName, ScrapeRunStats>`; her kaynağın
  son koşusu ayrı saklanır.
- `GET /api/scraper/health`: tüm kaynakların son koşu istatistikleri + kaynak başına
  `parsed/cardsSeen` oranı; eşik altında (`bubilet.scrape-health.min-parse-ratio` genelleştirilip
  `whatnext.scrape-health.min-parse-ratio` olur) kaynak `"status": "WARN"` işaretlenir.

### 1.2 Canlı DOM denetimi (tarayıcıyla) — SONUÇ (2026-07-11)
Her kaynak için listeleme sayfası tarayıcıda açılıp mevcut seçiciler konsolda
`document.querySelectorAll(...)` ile test edildi:

| Kaynak | Test URL | Kart seçicisi | Sonuç (eşleşme/gerçek) | Karar |
|---|---|---|---|---|
| Biletix | `biletix.com/search/TURKIYE/tr?category_sb=MUSIC&date_sb=thisweek&city_sb=istanbul` | `.grid_21.alpha.omega.listevent.searchResultEvent` | 31/31, başlık/tarih/mekân tümü dolu | **sağlam**, değişiklik yok |
| Biletino | `biletino.com/en/search/?...&category=100&location=istanbul&date=next10days` | `div.col-md-4.product` | 9/9, başlık/tarih/mekân tümü dolu | **sağlam** ama mekân alanı gerçek mekân değil ilçe adı (ör. "Kadıköy") — bkz. 1.3 |
| Biletinial | `biletinial.com/tr-tr/muzik/istanbul-avrupa?date=...&enddate=...` | `#kategori__etkinlikler li` | 5/5 (müzik), 12/12 (tiyatro), başlık/adres tümü dolu | **sağlam**, değişiklik yok |
| Bubilet | (Plan 1'de doğrulandı) | `div.min-h-0.min-w-0:has(...)` | ✓ | sağlam |

**Hiçbir kaynakta kart seçicisi kırık çıkmadı (0 eşleşme yok)** — dört kaynak da hâlâ canlı DOM ile
uyumlu. Buna rağmen iki somut bulgu çıktı:

1. **Biletino mekân kalitesi**: liste kartındaki mekân alanı gerçekte ilçe adı
   ("Kadıköy, İstanbul" → venueName="Kadıköy"), gerçek mekân yalnızca detay sayfasının `<title>`
   etiketinde (`"{başlık} | {tarih} | {mekân} | Biletino"`). Ayrıca `location=istanbul` parametresi
   sitede tam filtrelemiyor (Ankara/Mersin etkinlikleri de dönüyor) — ama `EventSaveService` şehri
   `data.getCity()`'den alıyor, bu yüzden veri bozulmuyor, sadece İstanbul koşusunda fazladan tarama
   oluyor (düşük öncelik, düzeltilmedi).
2. **Biletinial saat verisi hazır**: detay sayfasında `time[itemprop='startDate']` schema.org
   etiketi tam ISO datetime içeriyor (`content="2026-07-11T21:00"`), ama
   `BiletinialDetailParser.loadSessionsAndVenue` bunu `ldt.toLocalDate()` ile atıyor. **Faz 2'nin
   en ucuz girdisi bu** — ek parse gerekmeden saat çekilebilir.

**Saat verisi kaynak bazında (Faz 2 girdisi):**

| Kaynak | Liste satırında saat | Detay sayfasında saat |
|---|---|---|
| Bubilet | var ("17 Temmuz Cum 21:00") | değişken |
| Biletinial | yok (sadece tarih aralığı) | **var, ISO datetime, hazır** |
| Biletix | yok (tarih aralığı, "Cmt 11/07 - Paz 12/07") | denetlenmedi |
| Biletino | yok ("11 July Saturday") | yok |

### 1.3 Kırık çıkan parser'ların onarımı — SONUÇ
Denetimde (1.2) hiçbir kaynağın kart seçicisi kırık çıkmadığı için kapsamlı bir yeniden yazım
gerekmedi. Bulunan tek gerçek kalite kusuru düzeltildi:

- **`scraper/biletino/BiletinoVenueEnricher.java`** (yeni): Biletino'nun mekân alanı ilçe adı
  döndürdüğü için, geçerli her kartta ticketUrl'e düz HTTP GET (Jsoup, Selenium değil — `<title>`
  sunucu tarafında render ediliyor) ile detay sayfası title'ından gerçek mekân adı çekilir
  (`"... | ... | {mekân} | Biletino"` deseni). Ağ hatası/zaman aşımında mevcut (ilçe) değer korunur
  — sıfır regresyon riski. `biletino.enrich-venue-from-detail=true` ile açık/kapalı.
  Test: `BiletinoVenueEnricherTest` (gerçek denetim örnekleriyle).
- Biletix/Biletinial/Bubilet'te seçici veya parser değişikliği gerekmedi.
- `whatnext.scraper.allowed-sources` ile kaynak kapatma gerekmedi (hiçbiri "onarılamayacak kadar
  bozuk" değildi).

### 1.4 Zamanlayıcı düzeltmeleri — `services/EventSchedulerService.java`
- Aylık cron `0 0 2 30 * ?` → `0 0 2 L * ?` (Spring `L` = ayın son günü; Şubat düzelir).
- Bu sınıftaki tüm `@Scheduled`'lara `zone = "Europe/Istanbul"` eklenir.
- `LocalDate.now()` çağrıları `TrClock`'a geçer (Plan 1 Faz 1'de yazıldı).

### 1.5 Doğrulama
1. `mvn test` yeşil (yeni fixture testleri dahil).
2. Uygulama 8086'da; her **açık** kaynak için Postman'den istanbul scrape koşulur.
3. `GET /api/scraper/health`: tüm açık kaynaklarda parse oranı > %90, missingDate/missingVenue makul.
4. Kaynak başına 5 DB satırı tarayıcıda canlı siteyle karşılaştırılır (başlık/tarih/mekân doğru mu).
5. 1.2 tablosu doldurulmuş ve karar sütunu boş değil.

---

## Faz 2 — Saat verisi (start_time)

### 2.1 Şema + taşıma katmanı
- `models/Event.java`: `@Column(name = "start_time") LocalTime startTime` (nullable; ddl-auto=update
  kolonu ekler, mevcut satırlar NULL kalır — sorun değil, saat "varsa göster" mantığıyla kullanılır).
- `ScrapedEventData`: `LocalTime startTime` alanı.
- `EventSaveService.setCommonFields`: set eder; `EventIdentity` birleştirmesinde **eksik saat
  doldurulur** (gelen kayıtta saat var, mevcutta yoksa güncelle).

### 2.2 Parser'lar (1.2 denetiminin sonucuna göre kaynak bazında)
- **Bubilet**: `BubiletListCardParser` — liste satırı deseni zaten saati içeriyor
  (`... (\d{1,2}[:.]\d{2})?`, bkz. `BubiletDateRules.looksLikeListDateLine`). Yakalanan grup
  `LocalTime`'a çevrilip draft'a (`BubiletListCardDraft`) ve `BubiletEventMapper.toScraped`'e taşınır.
  Detay sayfası akışında da saat varsa alınır.
- **Biletinial** (en ucuz kazanç — denetimde doğrulandı): `BiletinialDetailParser.loadSessionsAndVenue`
  içinde `time[itemprop='startDate']`'in `content` attribute'u zaten tam ISO datetime
  (`"2026-07-11T21:00"`). Şu an `ldt.toLocalDate()` ile saat atılıyor; `ldt.toLocalTime()` alınıp
  `BiletinialEventMapper.fromDetailSession`'a taşınır. Ek parse/seçici gerekmez, sadece taşıma.
- **Biletix/Biletino**: denetimde liste ve (Biletino için) detay sayfasında saat bulunamadı
  (Biletix: "Cmt, 11/07/26 - Paz, 12/07/26" aralık; Biletino: "11 July Saturday"). Saat NULL bırakılır
  — zorlama sezgisel yok.
- `TurkishDateParser`'a dokunulmaz (tarih-saf kalır); saat ayıklama kaynak parser'ında yapılır.

### 2.3 DTO + tüketiciler
- `EventSummaryDTO` + `EventDetailDTO` + `EventMapper`: `startTime` eklenir.
- Bu fazda sadece veri taşınır; görsele/Reel'e yazımı Faz 3 ve 4'te.

### 2.4 Test + doğrulama
- Parser testleri: saatli satır ("17 Temmuz Cum 21:00" → 21:00), saatsiz satır (→ NULL),
  nokta ayraçlı ("21.00").
- Canlı scrape sonrası: `SELECT count(*) FROM event WHERE start_time IS NOT NULL` — Bubilet
  kaynaklı yeni kayıtların büyük çoğunluğunda dolu olmalı.

---

## Faz 3 — Statik görsel katmanı (post / story / cover)

Şablon estetiği korunur; değişiklik boyutlarda ve annotate (metin) katmanında.

### 3.1 Doğru boyutlar + güvenli alanlar — SONUÇ (2026-07-11)
- **Story: 1080×1920 (9:16) — uygulandı.** 12 şablonun tamamı ImageMagick "cover + merkez kırpma"
  (`-resize 1080x1920^ -gravity center -extent 1080x1920`) ile dönüştürüldü. Temmuz (marquee-ışık
  çerçeve, kenara kadar dolu tasarım) ve Şubat (farklı kaynak çözünürlük 832×1248) dahil 4 farklı
  şablon gözle doğrulandı — hiçbir illüstrasyon/metin öğesi kesilmedi. `June` zaten şablonsuzdu
  (Faz 4'ten beri bilinen durum, ilgisiz). `GeneratedTemplateProvider.DIMENSIONS.story` → 1080×1920.
- **Post/cover: 1:1'de kalıyor — kullanıcı kararı.** 4:5'e "cover" kırpma denendi (Temmuz post'ta
  kenara yakın dansçı illüstrasyonları görünür şekilde kesildi); dolgu (letterbox) alternatifi
  illüstrasyonları koruyor ama kağıt dokusuyla uyuşmayan düz renkli bar bırakıyor. Kullanıcı her iki
  görseli de inceleyip **1:1'de kalmayı seçti** — Instagram'da geçerli bir format, story'nin 9:16
  düzeltmesi zaten en kritik sorunu çözüyor. `GeneratedTemplateProvider.DIMENSIONS.post/cover`
  1024×1024'te değişmedi.
- **Önemli bulgu (3.2'yi etkiler):** Story'nin paylaşılan `layout.region` fraksiyonları
  (`left=0.08 top=0.15 width=0.84 height=0.80`, 12 ayda ortak) tasarıma göre değişken uyum gösteriyor
  — Temmuz'da metin kutusuna tam oturuyor, Şubat/Aralık'ta alt illüstrasyonlara (yürüyen figürler,
  müzisyenler) hafif taşıyor. Bu, benim kırpmamdan **önce de vardı** (yükseklik oranı kırpılmadı,
  sadece genişlik — matematiksel olarak fraksiyon→piksel eşlemesi dikeyde değişmedi), yani bir
  regresyon değil, mevcut bir tasarım gerilimi. Ortak fraksiyonları değiştirmek Temmuz gibi iyi oturan
  şablonları bozma riski taşıdığı için dokunulmadı. **Sonuç: 3.2'de etkinlik başına yeni bir satır
  eklemek yerine mevcut satırlara ekleme yapılmalı** (dikey alan bütçesini büyütmemek için).
- `readImageDimensions` zaten gerçek boyutu okuyor — motor tarafında başka değişiklik gerekmedi.

### 3.2 Slayt üst bilgisi: gün + tarih + saat
- `buildImageMagickCommandAuto`: tür başlığının ("KONSER") hemen altına ikinci satır —
  `"Cuma • 17 Temmuz"` (gün adı `TextStyle.FULL`, `tr` locale).
- Her etkinlik satırına saat: başlık satırının başına veya mekân satırına `"21:00 • @Mekân"`
  (yalnızca `startTime != null` ise; NULL'da bugünkü görünüm birebir korunur).
- `createCover`: tarihin altına ikinci satır — `"Bugün 14 etkinlik"` (o günün post'a giren toplam
  etkinlik sayısı; servis zaten listeyi biliyor, sayı parametre olarak geçilir).

### 3.3 Story seçimi skor bazlı — UYGULANDI
- `createSmartStory`: `Collections.shuffle` kaldırıldı; listeler `EventScoringStrategy.calculateScore`
  ile sıralanıyor (`sortByScoreDescending`), 4/4/2 kotası korunuyor. `findTopNEventsForDateAndCity`
  zaten sıralı gelmiyordu (DB sorgu sırası) — önceki davranış tamamen rastgeleydi, artık
  `PopularEventService` ile tutarlı skor sırası kullanılıyor.
- **Ertelendi (kapsam dışı):** "skor eşitse daha önce story'ye girmemiş olan öne alınır" ikincil
  kuralı uygulanmadı — `InstagramImage` kaydı hangi *etkinliklerin* story'ye girdiğini tutmuyor
  (sadece render edilmiş PNG + `eventType="smart_story"`), bu yüzden "son N günün başlıkları" sorgusu
  yeni bir şema (etkinlik↔story ilişki tablosu veya alanı) gerektiriyor — tek sorguyla yapılamıyor.
  Ayrı bir iyileştirme olarak ele alınmalı.

### 3.4 Gerçek font ölçümü ile sarma — UYGULANDI
- `wrapForAnnotate` artık `Font.createFont` + `FontMetrics.stringWidth` ile gerçek piksel genişliği
  ölçüyor (`pointsize × 0.58` tahmini kaldırıldı). İmza tam olarak plandaki gibi:
  `wrapForAnnotate(text, regionWidthPx, font, pointsize, maxLines)`. Tek kelime bile sığmıyorsa
  ikili aramayla "…" ile en uzun sığan önek kesiliyor (`truncateToWidth`).
  `computeMaxEventRowHeightPx`'teki satır-yüksekliği tahmini artık `buildVenueLineWithTime` kullanıyor
  (saat eklendiğinde uzayan mekân satırını da hesaba katıyor — önceden sadece "@mekân" ölçülüyordu).
  Legacy (fixed-pixel) yol da aynı Font'u kullanacak şekilde güncellendi (şu an 12 ayın hiçbiri
  kullanmıyor ama tutarlılık için).
- `ImageGenerationServiceTest` (yeni): `buildImageMagickCommandAuto`'yu gerçek Aralık story şablonu +
  gerçek fontlarla uçtan uca render edip PNG'yi görsel doğrulama için scratchpad'e kopyalıyor. Uzun
  başlık testi ("Cumhurbaşkanlığı Senfoni Orkestrası: Beethoven'in 9. Senfonisi Özel Gecesi") temiz
  kelime-sınırlı sarma gösterdi, taşma/erken kesme yok. Windows'ta `Font.createFont`'un dosya kilidini
  JVM çıkana kadar bırakmaması bilinen bir JDK davranışı — test fontları `@TempDir` yerine
  `deleteOnExit()` ile ayrı geçici dosyaya kopyalanıyor (üretim kodundaki `copyResourceToTempFile` ile
  aynı desen).

### 3.5 CTA + caption — UYGULANDI
- Story alt güvenli bölgeye annotate ile (şablona gömülmez) çağrı satırı eklendi. **Emoji'siz**:
  annotate fontu (NotoSans) emoji glyph'i içermiyor, ilk denemede 🔖 "tofu" kutusu olarak çıktı —
  metin `"KAYDET • whatnext.tr"`a çevrildi. Konumlandırma da elden geçti: sabit stroke yerine
  `-undercolor` ile yarı saydam "pill" arka plan — 12 ayın alt kenarındaki illüstrasyon yoğunluğu
  farklı olduğu için (bazı aylarda boşluk, Aralık'ta müzisyen figürleri) tek bir stroke rengi hepsinde
  okunaklı olmuyordu, pill hangi zeminde olursa olsun çalışıyor. Konum: alttan `%12` (Instagram story
  yanıt çubuğu güvenli bölgesi).
- `InstagramSchedulerService.buildDailyCaption(LocalDate)` (yeni, paket-görünür/test edilebilir):
  `EventService.findByDate` + `EventScoringStrategy` ile o günün gerçek etkinliklerini (İstanbul,
  çocuk hariç, skora göre, en çok 8) çeker; her satır `"• Başlık · SS:DD @ Mekân"`, sonda
  "🔖 Kaydet, hafta içinde lazım olacak!" (caption emoji'leri Instagram'ın kendi UI'sinde render
  olduğu için sorun değil — sadece görsele annotate ile gömülen metinde emoji sorunu var). Etkinlik
  yoksa eski jenerik satıra düşer. Eski `DAILY_CAPTION_TEMPLATE` sabiti kaldırıldı.
  `InstagramSchedulerServiceTest` (yeni, Mockito) ile doğrulandı.
- **Not:** `InstagramController`'ın `/share-for-date` endpoint'inde de ayrı, kısmen aynı isimli bir
  `DAILY_CAPTION_TEMPLATE` sabiti var (manuel test endpoint'i, farklı cron akışı) — kapsam dışı
  bırakıldı, plan yalnızca `InstagramSchedulerService`'i hedefliyordu.

### 3.6 Kod sağlığı — UYGULANDI
- `ImageGenerationService` + `InstagramSchedulerService`: tüm `System.out/err.println` → SLF4J
  `LOGGER` (30+ çağrı). Exception'lar artık `LOGGER.error(msg, e)` ile stack trace'i birlikte
  logluyor; ayrık `e.printStackTrace()` çağrıları kaldırıldı.
- `executeCommand`: ImageMagick başarısız olursa `null` yerine `IOException` fırlatıyor (imza zaten
  `throws IOException` bildiriyordu, çağıranlarda imza değişikliği gerekmedi). Bu, "o gün/tür için
  etkinlik yok" (bilinçli erken `null` dönüşü, `createPost`/`createSmartStory`'nin kendi mantığında)
  ile "render gerçekten patladı" (gerçek hata) durumlarının `InstagramService.createImage`'da aynı
  şekilde sessizce yutulmasını önlüyor.
- Küçük not: PowerShell ile toplu `println→LOGGER` metin değişimi dosyaya BOM (`﻿`) ekleyip
  derlemeyi bozdu — BOM'suz UTF-8 ile yeniden yazılıp düzeltildi.

### 3.7 Doğrulama
1. `mvn test` yeşil — 79/80 (tek istisna hep aynı bilinen kısıt: `WhatnextApplicationTests.contextLoads`,
   bu ortamdan Postgres'e erişim yok). Yeni testler: `ImageGenerationServiceTest` (6, gerçek ImageMagick
   render + görsel doğrulama scratchpad'e kopyalanarak yapıldı), `InstagramSchedulerServiceTest` (2,
   Mockito), `BiletinialEventMapperTest`, `BiletinoVenueEnricherTest`, genişletilmiş
   `TurkishDateParserTest`.
2. Postman'den test tarihine post + story + cover üret; çıktılar gözle: 9:16 story telefonda tam
   ekran (kırpılma yok), gün/saat satırları doğru, NULL saatli etkinlikte satır düzgün.
3. `mode=generated` ve `mode=static` ikisinde de üretim çalışıyor (TemplateProvider sözleşmesi bozulmadı).
4. Instagram'a göndermeden önce bir hafta çıktıları yan yana karşılaştırılır; onaydan sonra cron'lara bırakılır.

---

## Faz 4 — Reel yenileme (video/src + WeeklyVideoService)

### 4.1 Veri: WeeklyVideoService
- Event map'ine `startTime` (Faz 2) ve `dayName` (tr gün adı) eklenir.
- Kartlar **kategoriye göre değil `startDate`'e göre** sıralanır (Pzt→Paz); tür etiketi kartta rozet
  olarak kalır.
- Reel içi dedup `titleDateKey` yerine `EventIdentity.isSameEvent` ile (Plan 1 Faz 3 aracı).
- `LocalDate.now()` → `TrClock`.
- Kart sayısı/sıra yapısı props'a taşınır: `featured` (ilk 3) + `quick` (kalan) listeleri — tempo
  Java'dan değil kompozisyondan yönetilir.

### 4.2 Hook: intro'nun değiştirilmesi
- `IntroScreen` (2,5 sn logo/marka) kaldırılır. Açılış: haftanın en yüksek skorlu **görselli**
  etkinliğinin afişi tam ekran + tek satır: `"İstanbul'da bu hafta N etkinlik 👇"` (~1,2 sn).
- Marka zaten sabit footer'da (`whatnext.tr`); ayrıca intro'ya gerek yok.

### 4.3 Kart tasarımı
- **Tam ekran görsel**: afiş `AbsoluteFill`'e blur'lu dolgu + net ön kopya (mevcut Ken Burns korunur);
  metin alt üçte-birlik gradient üzerine. Görselsiz etkinlikte bugünkü metin kartı fallback kalır.
- **Gün rozeti**: sol üstte büyük `"CUMA"` + altında `"17 Temmuz • 21:00"` (saat varsa).
- **Hafta ilerleme çubuğu**: üstte 7 nokta, aktif gün dolu — "haftanı planla" hissinin görsel karşılığı.
- **Tempo**: `featured` kartlar 75 kare (2,5 sn), `quick` kartlar 35 kare (~1,2 sn, tek satır
  başlık+gün+saat). Hedef toplam ≤ 22 sn.
- **Font**: `system-ui` yerine `staticFile` + `@font-face` ile `AlfaSlabOne` (başlık) ve `NotoSans`
  (gövde) — statik görsellerle aynı dil. Font dosyaları `video/public/`'e kopyalanır.

### 4.4 Müzik senkronu + outro
- `bg-music.mp3` sabit olduğu için vuruş kareleri elle bir diziye yazılır
  (`const BEATS = [12, 41, ...]`); kart geçişleri en yakın vuruşa hizalanır (analiz kütüphanesi gerekmez).
- Outro CTA: `"Hemen Biletini Al!"` → `"Kaydet 🔖 — hafta içinde lazım olacak"` + `whatnext.tr`.
- Caption: gün-gün metin listesi (başlık • gün • saat), mevcut hashtag seti korunur.

### 4.5 Doğrulama
1. `npx remotion render` örnek props'la (3 görselli + 1 görselsiz + saatli/saatsiz karışık) —
   çıktı gözle: hook ilk saniyede etkinlik gösteriyor, gün sırası doğru, toplam süre ≤ 22 sn.
2. `npx remotion studio` ile kare kare vuruş hizası kontrolü.
3. Java tarafı: `WeeklyVideoServiceTest` (varsa) + dedup'ın EventIdentity ile çalıştığı birim testi.
4. İlk gerçek paylaşım öncesi video dosyası telefonda izlenir (safe zone: sağ ikon sütunu + alt caption
   alanı metinle çakışmıyor).

---

## Faz 5 — Haftalık plan karuseli + ölçüm

### 5.1 Haftalık plan karuseli
- Yeni üretim: kapak ("Bu Hafta İstanbul • N etkinlik") + Pzt→Paz 7 slayt (gün başına en iyi 3-4
  etkinlik, saatli). Mevcut `createPost` altyapısı `postType=weekly` varyantıyla kullanılır;
  şablon aylık post şablonunun kendisi (yeni tasarım gerekmez), üst bilgi satırı gün adını taşır.
- Paylaşım: Pazar akşamı **tek karusel** (planlamanın yapıldığı an). Mevcut Pazar 17:00'deki
  3 günlük seri bununla değiştirilir; Pzt 10:00 / Salı 10:00 serileri ilk etapta korunur, ölçüm
  (5.2) sonuçlarına göre sadeleştirilir.
- `InstagramService`'teki mevcut carousel yolu kullanılır (halihazırda 10 görsel limitiyle var).

### 5.2 Ölçüm
- Yeni `services/InstagramInsightsService`: Graph API'den medya başına `reach, saves, shares, plays,
  avg_watch_time` çeker; yeni tablo `instagram_media_metrics` (media_id, tip, yayın zamanı, metrikler,
  çekim zamanı). Günlük cron ile son 14 günün medyası güncellenir.
- `GET /api/instagram/metrics/summary`: içerik tipine (daily-post / weekly-carousel / reel / story)
  göre son 4 hafta karşılaştırması.
- İlk deney: Reel yayın saati — 4 hafta Salı 13:00 vs 4 hafta Pazar 19:00; `avg_watch_time` ve
  `reach` karşılaştırılır. Sonuç bu dosyaya işlenir.

### 5.3 Doğrulama
1. Karusel Postman'den üretilip gözle kontrol; test paylaşımı onay sonrası.
2. Metrik cron'u bir gün çalıştıktan sonra tablo dolu; summary endpoint'i makul değerler dönüyor.

---

## Kesişen notlar

- **Güvenlik (Plan 1'den beri açık, bu planın dışında ama önce yapılmalı):** `application.properties`
  içindeki Instagram token'ı, DB şifresi, Cloudinary/Apify anahtarları hem dosyada hem git geçmişinde.
  Dosyadan silmek yetmez — **anahtarlar rotate edilmeli**, yenileri `application-local.properties`'te
  (gitignore'lu) tutulmalı. Ayrıca repo kökündeki `compile_output.txt` silinip gitignore'a eklenmeli.
- **DB değişiklikleri (eklemeli, ddl-auto=update ile güvenli):** `event.start_time`,
  `instagram_media_metrics` tablosu (Faz 5).
- **Şablon dönüştürmeleri (Faz 3.1) geri alınabilir olmalı:** orijinal PNG'ler
  `src/main/resources/instagram-posts/` altında değiştirilmeden önce `instagram-posts-legacy/`
  altına kopyalanır (veya sadece git'e güvenilir — commit öncesi çıktılar gözle onaylanır).
- Her faz bağımsız yeşil kapanır: `mvn test` + Postman smoke + (Faz 1 sonrası) `/api/scraper/health`.

## Uçtan uca doğrulama
1. Faz 1: dört kaynak için health raporu temiz veya bilinçli kapatılmış; DB örnekleri canlı sitelerle uyumlu.
2. Faz 2: yeni scrape'lerde `start_time` dolu (Bubilet ≥ %80); merge eksik saati dolduruyor.
3. Faz 3: story telefonda tam ekran; slaytlarda gün+saat; story seçimi deterministik.
4. Faz 4: yeni Reel ≤ 22 sn, ilk kare etkinlik görseli, gün sıralı; telefonda safe-zone kontrolü.
5. Faz 5: Pazar karuseli yayında; 4 haftalık metrik karşılaştırması bu dosyaya işlendi.
