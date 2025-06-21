/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package housemate.entities;

import jakarta.persistence.*;
import java.util.Date;

/**
 *
 * @author ThanhF
 */
@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {

    @Id
    @Column(name = "payment_transaction_id")
    private int id;

    @Column(name = "order_id")
    private int orderId;

    @Column(name = "amount")
    private int amount;

    @Column(name = "status")
    private String status;

    @Column(name = "content")
    private int content;

    @Column(name = "payment_date")
    private Date paymentDate;

    @Column(name = "payment_method_id")
    private int paymentMethodId;

    @Column(name = "payer_id")
    private int payerId;

    @Column(name = "payee_id")
    private int payeeId;

    @Column(name = "date_create")
    private Date dateCreate;

    @Column(name = "expired_date")
    private Date expiredDate;

    public PaymentTransaction(int id, int orderId, int amount, String status, int content, Date paymentDate, int paymentMethodId, int payerId, int payeeId, Date dateCreate, Date expiredDate) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.content = content;
        this.paymentDate = paymentDate;
        this.paymentMethodId = paymentMethodId;
        this.payerId = payerId;
        this.payeeId = payeeId;
        this.dateCreate = dateCreate;
        this.expiredDate = expiredDate;
    }

    
}

