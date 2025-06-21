package com.pamu.model;

import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class EmployeeNode {
    private final Long id;
    private final Long managerId;
    private final String name;
    private final String role;
    private final List<EmployeeNode> reportees = new ArrayList<>();

    public EmployeeNode(Long id, Long managerId, String name, String role) {
        this.id = id;
        this.managerId = managerId;
        this.name = name;
        this.role = role;
    }

    public void addReportee(EmployeeNode reportee) {
        this.reportees.add(reportee);
    }
}

