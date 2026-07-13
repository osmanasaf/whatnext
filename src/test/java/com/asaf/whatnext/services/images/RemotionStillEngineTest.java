package com.asaf.whatnext.services.images;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.services.ImageGenerationService;
import com.asaf.whatnext.services.templates.GeneratedTemplateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemotionStillEngineTest {

    @Mock
    private RemotionRunner remotionRunner;
    @Mock
    private ImageGenerationService imageGenerationService;
    @Mock
    private GeneratedTemplateProvider paletteSource;

    private RemotionStillEngine engine;

    private static final LocalDate DATE = LocalDate.of(2026, 7, 17);
    private static final Map<String, String> PALETTE =
            Map.of("bg1", "#bf868d", "bg2", "#d7ab85", "accent", "#763b35", "text", "#1a1a1a");

    @BeforeEach
    void setUp() {
        engine = new RemotionStillEngine(remotionRunner, imageGenerationService, paletteSource);
        when(paletteSource.paletteHex("july")).thenReturn(PALETTE);
    }

    private static EventSummaryDTO event(Long id, String title) {
        EventSummaryDTO e = new EventSummaryDTO();
        e.setId(id);
        e.setTitle(title);
        e.setVenueName("Zorlu PSM");
        e.setStartTime(LocalTime.of(21, 0));
        return e;
    }

    @Test
    void renderPost_buildsPropsAndCallsDailyPostStill() throws Exception {
        when(imageGenerationService.selectPostEvents("concert", "istanbul", DATE))
                .thenReturn(List.of(event(1L, "Feridun Düzağaç")));
        File expected = new File("out.png");
        when(remotionRunner.run(eq("still"), eq("DailyPostStill"), any(), eq("png"))).thenReturn(expected);

        ImageRenderRequest request = new ImageRenderRequest("concert", "post", "istanbul", DATE);
        File result = engine.render(request);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<Map<String, Object>> propsCaptor = captureProps();
        Map<String, Object> props = propsCaptor.getValue();
        assertThat(props.get("eventType")).isEqualTo("concert");
        assertThat(props.get("monthName")).isEqualTo("july");
        assertThat(props.get("date")).isEqualTo("2026-07-17");
        assertThat(props.get("palette")).isEqualTo(PALETTE);
        assertThat(props).doesNotContainKey("backgroundImageUrl");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) props.get("events");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).get("title")).isEqualTo("Feridun Düzağaç");
        assertThat(events.get(0).get("startTime")).isEqualTo("21:00");
    }

    @Test
    void renderPost_noEvents_returnsNullWithoutCallingRemotion() throws Exception {
        when(imageGenerationService.selectPostEvents("concert", "istanbul", DATE)).thenReturn(List.of());

        File result = engine.render(new ImageRenderRequest("concert", "post", "istanbul", DATE));

        assertThat(result).isNull();
    }

    @Test
    void renderStory_callsSmartStoryStill() throws Exception {
        when(imageGenerationService.selectStoryEvents(DATE)).thenReturn(List.of(event(2L, "Gülinler")));
        when(remotionRunner.run(eq("still"), eq("SmartStoryStill"), any(), eq("png")))
                .thenReturn(new File("story.png"));

        File result = engine.render(new ImageRenderRequest("smart_story", "story", null, DATE));

        assertThat(result).isNotNull();
        verify(remotionRunner).run(eq("still"), eq("SmartStoryStill"), any(), eq("png"));
    }

    @Test
    void renderCover_callsCoverStillWithEventCount() throws Exception {
        when(imageGenerationService.countIstanbulEventsForDate(DATE)).thenReturn(9);
        when(remotionRunner.run(eq("still"), eq("CoverStill"), any(), eq("png"))).thenReturn(new File("cover.png"));

        engine.render(new ImageRenderRequest("cover", "cover", null, DATE));

        ArgumentCaptor<Map<String, Object>> propsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(remotionRunner).run(eq("still"), eq("CoverStill"), propsCaptor.capture(), eq("png"));
        assertThat(propsCaptor.getValue().get("eventCount")).isEqualTo(9);
    }

    @Test
    void renderWithBackgroundOverride_addsBackgroundImageUrlToProps() throws Exception {
        when(imageGenerationService.countIstanbulEventsForDate(DATE)).thenReturn(5);
        when(remotionRunner.run(eq("still"), eq("CoverStill"), any(), eq("png"))).thenReturn(new File("cover.png"));

        engine.render(new ImageRenderRequest("cover", "cover", null, DATE), "ai-backgrounds/july-cover.png");

        ArgumentCaptor<Map<String, Object>> propsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(remotionRunner).run(eq("still"), eq("CoverStill"), propsCaptor.capture(), eq("png"));
        assertThat(propsCaptor.getValue().get("backgroundImageUrl")).isEqualTo("ai-backgrounds/july-cover.png");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> captureProps() throws Exception {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(remotionRunner).run(anyString(), anyString(), captor.capture(), anyString());
        return captor;
    }
}
