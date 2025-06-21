/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package housemate.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import housemate.constants.Enum.TaskStatus;

/**
 *
 * @author ThanhF
 */
@Entity
@Table(name = "task_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_report_id")
    private int taskReportId;

    @Column(name = "task_id")
    private int taskId;

    @Column(name = "report_status_name")
    @Enumerated(EnumType.STRING)
    private TaskStatus taskStatus;

    @Column(name = "report_at")
    private LocalDateTime reportAt;

    @Column(name = "note", length = 3000)
    private String note;
    
    @Transient
    private int qtyOver;

    @Transient
    List<Image> taskReportImages;
    
    

}

