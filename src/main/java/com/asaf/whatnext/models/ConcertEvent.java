package com.asaf.whatnext.models;

import com.asaf.whatnext.enums.ConcertType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "events")
@EqualsAndHashCode(callSuper = true)
public class ConcertEvent extends Event {
    private Artist artist;
    private Venue venue;
    private ConcertType concertType;
}
