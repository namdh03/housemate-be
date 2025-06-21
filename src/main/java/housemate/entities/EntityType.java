/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package housemate.entities;

import jakarta.persistence.*;

/**
 *
 * @author ThanhF
 */
@Entity
@Table(name = "entity_type")
public class EntityType {

    @Id
    @Column(name = "entity_type_id")
    private int id;

    @Column(name = "entity_type_name")
    private String entityTypeName;

    public EntityType(int id, String entityTypeName) {
        this.id = id;
        this.entityTypeName = entityTypeName;
    }

}
