package com.asaf.whatnext.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "artists")
@EqualsAndHashCode(callSuper = true)
public class Artist extends BaseEntity{
    private String name;
    private String genre;
    private String biography;
}
