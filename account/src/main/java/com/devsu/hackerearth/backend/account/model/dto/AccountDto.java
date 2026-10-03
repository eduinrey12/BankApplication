package com.devsu.hackerearth.backend.account.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {

	private Long id;
	@javax.validation.constraints.NotBlank(message = "El numero de cuenta es obligatorio")
	private String number;
	private String type;
	private double initialAmount;
	@com.fasterxml.jackson.annotation.JsonProperty("isActive")
	private boolean isActive;
	@javax.validation.constraints.NotNull(message = "El clientId es obligatorio")
	private Long clientId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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
