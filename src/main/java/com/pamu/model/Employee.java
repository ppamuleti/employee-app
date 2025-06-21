package com.pamu.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"manager"})
public class Employee {

    @Id
    private Long id;

    private String name;
    private String city;
    private String state;
    private String category;
    private Double salary;
    private LocalDate doj;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;


}

