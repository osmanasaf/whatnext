package com.asaf.whatnext.services.images;

import java.io.File;
import java.io.IOException;

/**
 * Bir görsel üretim motoru (custom | remotion | ai) — {@link DelegatingImageEngine} bayrağa göre
 * bunlardan birine yönlendirir. Başarısızlıkta {@code null} DEĞİL exception fırlatılır (Faz 3.6'daki
 * karar: "veri yok" ile "üretim gerçekten patladı" birbirine karışmasın); "o gün/tür için etkinlik
 * yok" durumu da {@code null} dönerek ifade edilir — bu, {@code InstagramService.createImage}'in
 * zaten beklediği sözleşme.
 */
public interface ImageRenderEngine {
    File render(ImageRenderRequest request) throws IOException, InterruptedException;
}
