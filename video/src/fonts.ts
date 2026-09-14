import { useEffect, useState } from 'react';
import { continueRender, delayRender, staticFile } from 'remotion';

// Marka fontları (statik görsel boru hattıyla aynı: AlfaSlabOne başlık, NotoSans gövde).
const alfaSlabOne = new FontFace(
  'AlfaSlabOne',
  `url('${staticFile('fonts/AlfaSlabOne-Regular.ttf')}') format('truetype')`
);
const notoSans = new FontFace(
  'NotoSans',
  `url('${staticFile('fonts/NotoSans-Regular.ttf')}') format('truetype')`
);

const fontsLoaded = Promise.all([alfaSlabOne.load(), notoSans.load()])
  .then((fonts) => fonts.forEach((font) => document.fonts.add(font)))
  .catch((err) => console.error('Font yüklenemedi, sistem fontuna düşülüyor', err));

/**
 * Render fontlar yüklenene kadar bekler. delayRender modül seviyesinde DEĞİL, bileşen içinde:
 * bundle değerlendirilirken Remotion `window.remotion_delayRenderHandles`'ı sıfırlıyor, modül
 * seviyesindeki handle listeden düşüyor, continueRender onu temizleyemiyor ve 28sn sonra render
 * iptal oluyordu (still'ler ~1sn sürdüğü için yakalanmadı, Reel her seferinde patlıyordu).
 */
export const useBrandFonts = () => {
  const [handle] = useState(() => delayRender('Marka fontları yükleniyor'));
  useEffect(() => {
    fontsLoaded.then(() => continueRender(handle));
  }, [handle]);
};

export const FONT_TITLE = 'AlfaSlabOne, system-ui, sans-serif';
export const FONT_BODY = 'NotoSans, system-ui, sans-serif';
