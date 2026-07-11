# WhatNext İyileştirme Planı — Scraper, Çocuk Etkinlikleri, Tekilleştirme, Görsel Otomasyonu

> Bu plan 2026-07-11 tarihinde, kod tabanı keşfi + bubilet.com.tr'nin canlı DOM incelemesi (tarayıcı ile) sonucunda hazırlandı.
> Uygulamak için yeni bir Claude Code oturumunda: "IMPROVEMENT_PLAN.md dosyasını oku ve Faz 1'i uygula" demeniz yeterli.

## Bağlam (Context)

Uygulama, bilet sitelerinden (ağırlıklı Bubilet; Selenium+Jsoup) etkinlik toplayıp günlük/haftalık Instagram gönderileri (ImageMagick + aylık şablon PNG + koordinat .properties) ve haftalık Reel (Remotion) üretiyor. Dört sorun raporlandı ve araştırmada kök nedenleri doğrulandı:

1. **Yanlış scrape** — Canlı sitede tarayıcıyla doğrulandı: Bubilet DOM'u değişmiş. `h3 + p` seçicileri **0 eşleşme** veriyor; her etkinlik artık 3 ayrı `<a>`'ya bölünmüş (görsel / başlık / tarih+fiyat). Scraper her `<a>`'yı ayrı kart sanıyor (164 anchor ≈ 68 gerçek etkinlik). Ek buglar: ay adı içeren mekânlar ("Mayıs Sahnesi") tarih sanılıyor; şehirler arası dedup aynı turne etkinliğinin bir şehrini düşürüyor; tarih parse sistem saat dilimiyle çalışıyor (Europe/Istanbul değil).
2. **Çocuk etkinliği tespiti zayıf** — `EventAudience` sadece başlıkta "çocuk"/"yaş grubu" arıyor; "Pinokyo", "Karlar Ülkesi", "3+ Yaş" gibi başlıklar kaçıyor. Bubilet'in `cocuk-aktiviteleri` etiketi mevcut (canlıda doğrulandı) ama scrape edilmiyor. **Karar: tespiti güçlendir, gönderilerden hariç tutmaya devam et.**
3. **Aynı etkinlik tekrar gösteriliyor** — Kayıt anındaki dedup sadece birebir başlık+tarih eşitliği; farklı sitelerdeki farklı isimli aynı etkinlik ayrı satır oluyor. **Karar: aynı gün içinde, sanatçı/mekân/başlık zekâsıyla (normalizasyon + mekân slug + token benzerliği) kayıt anında akıllı eşleştirme.**
4. **Görsel üretimi manuel** — 12 ay × 3 tür = 36 el yapımı şablon PNG + 36 elle ayarlanan koordinat dosyası. **Karar: bayrakla seçilebilir çift akış — parametrik şablon üretici VE AI görsel üretimi; mevcut tasarım dili (otantiklik) korunacak.** Motorda zaten `layout.auto=true` + `layout.region.*` otomatik yerleşim modu var — üretilen şablonlar bunu kullanacak, boru hattının kalanı değişmeyecek.

Fazlar bağımlılık sırasına göre: Faz 1 (scraper) hepsini besler; Faz 4 bağımsız.

---

## Faz 1 — Bubilet scraper'ı yeni DOM'a göre yeniden yaz + fiyat + saat dilimi + doğrulama

### 1.1 Konteyner bazlı seçiciler — `src/main/java/com/asaf/whatnext/config/BubiletScraperConfig.java`
Canlıda doğrulanan yapı: `div.group/dateprice.min-h-0.min-w-0` içinde:
- `a[href*='/etkinlik/'] > h3` = başlık
- `a[href*='/mekan/']` = mekân adı (metin) + kanonik mekân slug'ı (href) — metin sezgisi gerekmez
- `a.date-price-trigger > p` = tarih satırı ("17 Temmuz Cum 21:00"); `₺` span'ları = fiyat (örn. 1712)

Değişiklikler:
- `EVENT_CARD_SELECTOR` → `"div.min-h-0.min-w-0:has(a[href*='/etkinlik/'] h3)"`
- Yeni sabitler: `EVENT_TITLE_ANCHOR`, `EVENT_VENUE_ANCHOR = "a[href*='/mekan/']"`, `EVENT_DATEPRICE_TRIGGER = "a.date-price-trigger"`
- `EVENT_VENUE_AFTER_H3`, `EVENT_DATE_AFTER_H3`, `EVENT_VENUE_ALTERNATE_SELECTORS` deprecated (canlı DOM'da ölü)

### 1.2 Parser yeniden yazımı — `src/main/java/com/asaf/whatnext/scraper/bubilet/BubiletListCardParser.java`
- Başlık/ticketUrl h3 anchor'dan; mekân adı + `venueSlug` mekân anchor'ından; tarih + `minPriceTl` date-price-trigger'dan.
- **Çok-mekânlı tuzak** (canlıda doğrulandı): `/mekan/` anchor'ı yok VE `<p>` satırı tarih desenine uymuyorsa → satır virgülle ayrılmış MEKÂN listesidir ("Hilltown Seyirlik Sahne, Trump Sahne…") → `multiVenue=true`, tarihler detay sayfasından.
- Eski `h3 + p` yolu son çare fallback olarak kalır.

`scraper/bubilet/BubiletDateRules.java`:
- `looksLikeListDateLine`: "ay adı içeriyor" yerine baştan bağlantılı desen `^\d{1,2}\s+(Ocak|…|Aralık)(\s+\S+)?(\s+\d{1,2}[:.]\d{2})?` — "Mayıs Sahnesi" bug'ı düzelir. `₺`/"Yakında" dışlamaları kalır.
- Yeni `looksLikeVenueList(String)`.

Draft kayıtları (`BubiletListCardDraft`, `BubiletCardDraft`): `venueSlug`, `minPriceTl`, `multiVenue`, `venueNames` alanları eklenir.

### 1.3 Veri modeli: fiyat
- `ScrapedEventData`: `Integer minPriceTl` + `String venueSlug` ekle.
- `models/Event.java`: `@Column(name="min_price_tl") Integer minPriceTl` (ddl-auto=update kolonu ekler). `EventSaveService.setCommonFields` (satır ~211) set eder.
- `BubiletEventMapper.toScraped` taşır.

### 1.4 Şehirler arası dedup bug'ı — `scraper/BubiletScraper.java`
- `dedupeByPathAndDate` anahtarı `city|path|date` olur (`canonicalEtkinlikPath` şu an `/{city}/` önekini atıyor → İstanbul+Ankara aynı slug'a çöküyor, satır ~234).
- `parseCity`: bilinmeyen şehirde sessiz ISTANBUL varsayımı yerine WARN log + istatistik sayacı.

### 1.5 Saat dilimi + yıl devri
- Yeni `utils/TrClock.java`: `LocalDate.now(ZoneId.of("Europe/Istanbul"))`. `scraper/` altındaki tüm `LocalDate.now()` çağrıları buna geçer (`TurkishDateParser` parametreli/saf kalır).
- Yılbaşı sınırı testi eklenir (28 Aralık'ta "5 Ocak" scrape'i → gelecek yıl).

### 1.6 Scrape doğrulama mekanizması (sürekli güven)
- Yeni `scraper/ScrapeRunStats.java`: cardsSeen, parsed, parseFailed, missingVenue, missingDate, multiVenue, priceExtracted, unknownCity, süre + kova başına 3 örnek başlık.
- `scrapeCategory` doldurur; tek satır özet log; eşik aşımında WARN (örn. parsed/cardsSeen < 0.8).
- Son koşu istatistikleri `ScrapeHealthService`'te; `GET /api/scraper/health` endpoint'i (Postman koleksiyonuna eklenecek). Site değişince tek istekle anlaşılır.

### 1.7 Fixture + testler
- Yeni `src/test/resources/fixtures/bubilet/`: canlı sayfadan kopyalanan gerçek konteyner HTML'leri — `card-single-venue.html`, `card-multi-venue.html`, `card-venue-with-month-word.html`, `card-kids.html`.
- `BubiletListCardParserTest` fixture'larla genişler (başlık/mekân/slug/tarih/fiyat/multiVenue); mevcut inline testler fallback regresyonu olarak kalır.
- `BubiletDateRulesTest`: "Mayıs Sahnesi" ≠ tarih; "17 Temmuz Cum 21:00" = tarih; mekân listesi tespiti.

### 1.8 Doğrulama
1. `mvn test` yeşil.
2. Uygulama 8086'da; Postman'den istanbul/tiyatro scrape → log'da ~48+ kart, parse oranı >%90, fiyatlar dolu.
3. `GET /api/scraper/health` makul; 5 DB satırı tarayıcıda canlı siteyle karşılaştırılır.
4. Ankara tekrarı → aynı slug iki şehirde de var (cross-city fix kanıtı).

---

## Faz 2 — Çocuk etkinlikleri: etiket scrape'i (ground truth) + kalıcı alan + güçlü sezgisel

### 2.1 Şema
- Yeni `enums/AudienceType.java`: `GENERAL, CHILDREN, UNKNOWN`.
- `Event.java`: `@Enumerated(STRING) @Column(name="audience") AudienceType audience` (NULL = UNKNOWN kabul edilir).
- `ScrapedEventData`'ya `audience`; `EventSaveService.setCommonFields` kalıcılaştırır.

### 2.2 Ground truth: çocuk etiketi scrape'i
- `enums/Bubilet/BubiletCategory.java`: `KIDS("cocuk-aktiviteleri")` eklenir (canlıda doğrulandı: `/istanbul/etiket/cocuk-aktiviteleri`, aynı kart yapısı; "Pinokyo Tiyatro Oyunu", "Karlar Ülkesi", "Kaptan Pengu", "Rapunzel Çocuk Oyunu" burada).
- `BubiletScraper.scrapeEvents`: önce KIDS scrape → `Set<String> kidsKeys` (city+path). KIDS etkinlikleri `audience=CHILDREN` ile kaydedilir; tiyatro/konser/standup sonuçlarında anahtarı kids setinde olanlar da CHILDREN işaretlenir (aynı oyun iki etikette de listeleniyor).

### 2.3 Sezgisel (diğer kaynaklar + eski satırlar) — `utils/EventAudience.java` yeniden yazımı
- `classify(title, venueName)`: yaş deseni regex (`\d+\s*\+?\s*yaş`, "yaş grubu", "0-3 yaş"); anahtar kelimeler (çocuk/cocuk, bebek, minik, masal, kukla); etiket scrape'inden tohumlanan bilinen çocuk oyunu adları (pinokyo, rapunzel, karlar ülkesi, kaptan pengu…); mekân adında çocuk.
- `isChildrensEvent(Event)`: `audience==CHILDREN` → true; `GENERAL` → false; `UNKNOWN` → `classify` fallback. Çağrı noktaları (`ImageGenerationService` ~95/191/204, `PopularEventService` ~70) şekil olarak aynı kalır — render'da hariç tutma sürer.

### 2.4 Geri doldurma
- Yeni `services/AudienceBackfillService.java` + `POST /api/events/audience/backfill?dryRun=true`: audience NULL/UNKNOWN satırları `classify` ile günceller; dryRun değişecekleri JSON döner.

### 2.5 Test + doğrulama
- Yeni `EventAudienceTest`: canlı etiketten gerçek başlıklar ("Pinokyo Tiyatro Oyunu", "Karlar Ülkesi", "Rapunzel Çocuk Oyunu"), yaş desenleri, negatif örnekler (yetişkin stand-up başlıkları eşleşmemeli).
- KIDS scrape sonrası `SELECT count(*) FROM event WHERE audience='CHILDREN'`; Postman'den story üret → çocuk başlığı görünmemeli.

---

## Faz 3 — Kayıt anında akıllı tekilleştirme (aynı gün, siteler arası kimlik)

### 3.1 Eşleştirme aracı — yeni `utils/EventIdentity.java`
- Normalizasyon `EventDuplicateKeys`'ten çıkarılıp paylaşılır (tek doğruluk kaynağı): NFC, tr-lower, U+0307 temizliği, alfasayısal dışı → boşluk.
- `isSameEvent(Event aday, ScrapedEventData gelen)` — adaylar zaten aynı startDate; kademeler:
  1. Normalize başlık birebir eşit → true
  2. Mekân eşleşiyor (ikisinde de varsa venueSlug, yoksa normalize mekân adı) VE başlık Jaccard ≥ 0.5 → true
  3. Başlık Jaccard ≥ 0.85 → mekândan bağımsız true
  4. Konser: sanatçı token'ları diğer başlıkta tamamen içeriliyor VE mekân eşleşiyor → true
- Eşikler property: `whatnext.dedup.title-jaccard=0.5`, `whatnext.dedup.title-jaccard-strong=0.85`.
- `models/Venue.java`'ya `slug` kolonu; `EventSaveService.findOrCreateVenue` (satır ~99) önce slug'la, sonra adla eşler (mekân dublikasyonunu da bitirir).

### 3.2 Kayıt entegrasyonu — `services/EventSaveService.java`
- Dört `existsByTitleAndDate` kontrolü (satır ~142/148/181/187 — 148 ve 187'deki "normalize edilmiş" sorgular ölü kod: normalize string ham başlıklarla karşılaştırılıyor) tek yolla değişir:
  - `EventRepository`'ye `findByStartDateWithVenue(LocalDate)` (LEFT JOIN FETCH venue; şehir filtresi `EventRepositoryCityMatch.JPQL_EQ_PARAM` ile).
  - `findDuplicate(data)` → adaylar → `EventIdentity.isSameEvent`.
  - Eşleşmede **atlamak yerine birleştir**: ticketUrl ekle, eksik mekân/görsel/fiyat doldur (fiyatta min), gelen CHILDREN ise audience'ı yükselt.
- `Event`'e `normalized_title` kolonu (ileride SQL indeksi için); eşleştirme Java'da kalır (mevcut dublikeler unique constraint'i engelliyor).

### 3.3 Mevcut veri temizliği
- Yeni `POST /api/events/dedup?dryRun=true`: tüm etkinlikler startDate'e göre gruplanır, gün içi ikili `EventIdentity` taraması; dryRun birleştirme gruplarını raporlar, gerçek modda en düşük id kalır, ticketUrl'ler taşınır, diğerleri silinir.

### 3.4 Test + doğrulama
- Yeni `EventIdentityTest`: ("FoodieFest İstanbul" ↔ "Foodiefest"), aynı venueSlug farklı başlık, tr büyük/küçük harf tuzakları (İ/ı), eşik sınırı belgeleyen negatif örnek.
- Dedup dry-run canlı DB'de çalıştırılır, gruplar gözle kontrol edilir; `GROUP BY normalized_title, start_date HAVING count(*)>1` küçülmeli.

---

## Faz 4 — Şablon otomasyonu: `TemplateProvider` — static | generated | ai (bayrakla)

### 4.1 Sağlayıcı soyutlaması — yeni paket `services/templates/`
- `TemplateAssets(File templatePng, Properties coordinates)` + `TemplateProvider.resolve(monthName, postType)`.
- **`StaticTemplateProvider`** — mevcut classpath mantığı buraya taşınır (`ImageGenerationService` satır ~62-63/134-135/176-177/252-257'deki çözümleme). Bugünkü davranış birebir korunur.
- **`GeneratedTemplateProvider`** (parametrik, Java2D):
  - Stil konfigürasyonu `src/main/resources/instagram-templates/style.properties` + ay başına palet (`palette.<month>.bg1/bg2/accent/text`) — paletler mevcut 36 PNG'den bir kez örneklenir → **otantiklik: tasarım dili mevcut şablonlardan türetilir**.
  - Graphics2D: gradyan arka plan, ay başlığı (mevcut AlfaSlabOne/Audiowide fontları), dekoratif öğeler; 1080×1350 (post/cover), 1080×1920 (story).
  - Çıktı: `${whatnext.templates.workdir}/<month>/<postType>-template.png` + `layout.auto=true` ve `layout.region.*` içeren koordinat dosyası → **mevcut otomatik yerleşim modu (`buildImageMagickCommandAuto`) aynen kullanılır, akışın kalanı değişmez**. Sidecar hash ile önbellek.
- **`AiTemplateProvider`** — aynı çıktı sözleşmesi:
  - `whatnext.templates.ai.provider=gemini|openai|stability` (Anthropic'te görsel üretim API'si yok; varsayılan Gemini image REST, `RestTemplate` ile ince `AiImageClient` arayüzü).
  - Prompt stil konfig + aydan üretilir ("minimal poster background, {palet}, no text…"); dönen görsel ImageMagick ile tam tuvale kırpılır; aynı auto-layout koordinatları yazılır. **API hatasında GeneratedTemplateProvider'a düşer** (11:50 cron'u asla bloklanmaz).
- **`DelegatingTemplateProvider`** (`@Primary`): `whatnext.templates.mode=static|generated|ai` (varsayılan static); çözüm sırası: workdir dosyası (mode≠static) → classpath static fallback.

### 4.2 Entegrasyon
`ImageGenerationService`'e `TemplateProvider` enjekte edilir; üç kaynak-çözümleme bloğu `provider.resolve(...)` olur. ImageMagick komutları, fontlar, Cloudinary, scheduler dokunulmaz.

### 4.3 Yeni property'ler
```
whatnext.templates.mode=static
whatnext.templates.workdir=generated-templates
whatnext.templates.ai.provider=gemini
whatnext.templates.ai.api-key=
whatnext.templates.ai.model=
whatnext.dedup.title-jaccard=0.5
whatnext.dedup.title-jaccard-strong=0.85
```

### 4.4 Test + doğrulama
- `GeneratedTemplateProviderTest`: temp dizine ay render → PNG boyutları + koordinatta `layout.auto=true` (headless, ImageMagick gerekmez).
- `DelegatingTemplateProviderTest`: mod yönlendirme + static fallback.
- Görsel karşılaştırma: `mode=generated` ile Postman'den test tarihine post/story üret, Temmuz static çıktısıyla yan yana bak; `mode=ai` ile aynı kontrol; `mode=static` bugünle birebir aynı olmalı.

---

## Faz sırası ve kesişen notlar

- **Sıra: 1 → 2 → 3 → 4** (4, Faz 1'den sonra herhangi bir anda paralel başlayabilir). Her faz bağımsız yeşil kapanır: `mvn test` + Postman smoke + `/api/scraper/health`.
- **DB değişiklikleri (hepsi ddl-auto=update ile eklemeli, güvenli):** `event.min_price_tl`, `event.audience`, `event.normalized_title`, `venue.slug`.
- **Yan bulgu (plan dışı, bilginize):** `application.properties` içinde canlı Instagram token'ı, DB şifresi, Cloudinary/Apify anahtarları düz metin commit'li. Ayrıca repo kökünde `compile_output.txt` gereksiz. Bunları ayrıca ele almak isteyebilirsiniz.

## Uçtan uca doğrulama
1. Faz 1 sonrası: canlı scrape → `/api/scraper/health` + DB örneklerinin tarayıcıyla karşılaştırılması.
2. Faz 2 sonrası: KIDS scrape → story üretimi → çocuk başlığı yok.
3. Faz 3 sonrası: çift kaynak scrape → duplike satır artışı yok; dedup dry-run raporu temiz.
4. Faz 4 sonrası: üç modda da görsel üretimi Postman'den test edilip Instagram'a göndermeden gözle karşılaştırılır.
