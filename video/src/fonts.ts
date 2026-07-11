import { continueRender, delayRender, staticFile } from 'remotion';

// Marka fontları (statik görsel boru hattıyla aynı: AlfaSlabOne başlık, NotoSans gövde).
// Remotion render'ı ekran görüntüsü almadan önce fontların yüklenmesini bekler (delayRender/continueRender).
const waitForFonts = delayRender('Marka fontları yükleniyor');

const alfaSlabOne = new FontFace(
  'AlfaSlabOne',
  `url('${staticFile('fonts/AlfaSlabOne-Regular.ttf')}') format('truetype')`
);
const notoSans = new FontFace(
  'NotoSans',
  `url('${staticFile('fonts/NotoSans-Regular.ttf')}') format('truetype')`
);

Promise.all([alfaSlabOne.load(), notoSans.load()])
  .then((fonts) => {
    fonts.forEach((font) => document.fonts.add(font));
    continueRender(waitForFonts);
  })
  .catch((err) => {
    console.error('Font yüklenemedi, sistem fontuna düşülüyor', err);
    continueRender(waitForFonts);
  });

export const FONT_TITLE = 'AlfaSlabOne, system-ui, sans-serif';
export const FONT_BODY = 'NotoSans, system-ui, sans-serif';
