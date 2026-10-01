package com.devsu.hackerearth.backend.account.model;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Account extends Base {
    private String number;
	private String type;
	private double initialAmount;
	private boolean isActive;

    @Column(name = "client_id")
    private Long clientId;
}
