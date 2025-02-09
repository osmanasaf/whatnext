package com.asaf.whatnext.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "venues")
public class Venue extends BaseEntity{
    private String name;
    private String location;
    private int capacity;
}
