package com.asaf.whatnext.models;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "events")
public class ExhibitionEvent extends Event {
    private Artist artist;
    private Gallery gallery;
    private Artist artType;
}
