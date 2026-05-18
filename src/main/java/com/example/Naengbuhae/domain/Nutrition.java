package com.example.Naengbuhae.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Nutrition {
    private Integer calories; // kcal
    private Integer protein;  // g
    private Integer carbs;    // g
    private Integer fat;      // g
    private Integer sodium;   // mg
}
