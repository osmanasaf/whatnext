# WhatNext Haftalık Reel

Remotion ile haftalık etkinlik Reel videosu üretimi.

## Kurulum

```bash
cd video
npm install
```

## Geliştirme

```bash
npm start
```

## Render (CLI)

```bash
npx remotion render src/index.ts WeeklyReel out.mp4 --props='{"events":[],"weekRange":"02 Şubat - 08 Şubat"}'
```

## Gereksinimler

- Node.js 18+
- Remotion 4

## Arka plan müziği (telif)

`video/public/bg-music.mp3` dosyası `WeeklyReel` içinde döngüde çalınır. Parçayı [Pixabay Music](https://pixabay.com/music/), [YouTube Audio Library](https://www.youtube.com/audiolibrary) veya lisansını okuduğunuz telifsiz kaynaklardan seçin; CC BY gibi atıf gerektiren lisanslarda Instagram caption’da kaynak belirtin. Instagram’da telif uyarısı alırsanız parçayı değiştirin veya `WeeklyReel.tsx` içindeki `<Audio />` satırını yorumlayarak sessiz export alın.

## Not

Spring Boot uygulaması Salı 12:00 (TSİ) otomatik render ve Instagram paylaşımı yapar. Lokal test için `WeeklyVideoService` veya endpoint üzerinden tetiklenebilir.
