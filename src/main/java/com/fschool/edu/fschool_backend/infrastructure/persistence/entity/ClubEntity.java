package com.fschool.edu.fschool_backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clubs")
public class ClubEntity extends AuditableEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 20)
    private String publicId;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "teacher_code", length = 20)
    private String teacherCode;

    @Column(name = "teacher_name", length = 150)
    private String teacherName;

    @Column(name = "teacher_phone", length = 20)
    private String teacherPhone;

    @Column(name = "teacher_email", length = 150)
    private String teacherEmail;

    @Column(length = 100)
    private String location;

    @Column(name = "weekday_label", length = 20)
    private String weekday;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "member_count", nullable = false)
    private int memberCount;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "registration_open", nullable = false)
    private boolean registrationOpen = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
