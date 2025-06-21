package com.pamu.dto;

import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EmployeeDTO {
    private Long id;
    private String name;
    private double salary;
    private String category;
    private LocalDate doj;
    private Long managerId;
}
