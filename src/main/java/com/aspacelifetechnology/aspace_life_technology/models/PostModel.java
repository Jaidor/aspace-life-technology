package com.aspacelifetechnology.aspace_life_technology.models;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "posts")
public class PostModel {
    @Id
    private Long id; // use the JSONPlaceholder post ID

    private Long userId;

    @Column(length = 2000)
    private String title;

    @Column(length = 5000)
    private String body;
}
