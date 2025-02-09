package com.asaf.whatnext.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "actors")
@EqualsAndHashCode(callSuper = true)
public class Actor extends BaseEntity{
    private String name;
    private String biography;
    private String birthDate;
}
