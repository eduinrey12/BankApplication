package com.devsu.hackerearth.backend.account.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "accounts")
public class Account extends Base {

	@Column(name = "number", unique = true, nullable = false)
	private String number;

	@Column(name = "type")
	private String type;

	@Column(name = "initial_amount")
	private double initialAmount;

	@Column(name = "is_active")
	private boolean isActive;

	@Column(name = "client_id")
	private Long clientId;
}
