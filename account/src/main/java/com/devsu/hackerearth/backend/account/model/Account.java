package com.devsu.hackerearth.backend.account.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
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
@Table(name = "accounts", indexes = {
	@Index(name = "idx_acc_client_id", columnList = "client_id"),
	@Index(name = "idx_acc_number", columnList = "number", unique = true)
})
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

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double getInitialAmount() {
		return initialAmount;
	}

	public void setInitialAmount(double initialAmount) {
		this.initialAmount = initialAmount;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		isActive = active;
	}

	public Long getClientId() {
		return clientId;
	}

	public void setClientId(Long clientId) {
		this.clientId = clientId;
	}
}
