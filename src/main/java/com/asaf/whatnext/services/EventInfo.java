package com.asaf.whatnext.services;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventInfo {

    private final String title;

    private final String dateStr;

    private final String venueName;

    private final String location;

    private final String ticketUrl;

}
