package com.asaf.whatnext.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "events")
public class TheaterEvent extends Event {
    private String playwright;
    private List<Actor> actors;
    private Director director;
    private String stage;
}
