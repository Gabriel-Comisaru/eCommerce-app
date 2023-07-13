package com.qual.store.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qual.store.model.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class Review extends BaseEntity<Long> implements Serializable {

    private double rating;

    @Column(unique = true)
    private String title;

    private String comment;

    @CreationTimestamp
    private LocalDateTime date;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "product_id")
//    @JsonBackReference
    @JsonIgnore
    private Product product;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private AppUser user;
}
