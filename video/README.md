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

## Not

Spring Boot uygulaması Salı 12:00 (TSİ) otomatik render ve Instagram paylaşımı yapar. Lokal test için `WeeklyVideoService` veya endpoint üzerinden tetiklenebilir.
