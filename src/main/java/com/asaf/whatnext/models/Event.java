package com.asaf.whatnext.models;

import java.lang.String;
import java.time.LocalDateTime;
import lombok.Data;

import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "events")
@EqualsAndHashCode(callSuper = true)
public abstract class Event extends BaseEntity {
    private String title;
    private String eventType;
    private String location;
    private LocalDateTime date;
    private String description;
    private String url;
}
