package com.asaf.whatnext.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class Gallery extends BaseEntity {
    private String name;
    private String location;
    private String description;
    
    @OneToMany(mappedBy = "gallery")
    private List<ExhibitionEvent> events;
}
