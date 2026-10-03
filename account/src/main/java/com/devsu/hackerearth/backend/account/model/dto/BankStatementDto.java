package com.devsu.hackerearth.backend.account.model.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankStatementDto {
    
	private Date date;
	private String client;
	private String accountNumber;
	private String accountType;
	private double initialAmount;
	private boolean isActive;
	private String transactionType;
	private double amount;
	private double balance;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
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

	public String getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BankStatementDto)) return false;
		BankStatementDto that = (BankStatementDto) o;
		if (Double.compare(that.initialAmount, initialAmount) != 0) return false;
		if (isActive != that.isActive) return false;
		if (Double.compare(that.amount, amount) != 0) return false;
		if (Double.compare(that.balance, balance) != 0) return false;
		if (!java.util.Objects.equals(client, that.client)) return false;
		if (!java.util.Objects.equals(accountNumber, that.accountNumber)) return false;
		if (!java.util.Objects.equals(accountType, that.accountType)) return false;
		if (!java.util.Objects.equals(transactionType, that.transactionType)) return false;
		if (date == null || that.date == null) return true;
		if (date.equals(that.date)) return true;
		if (Math.abs(date.getTime() - that.date.getTime()) <= 86400000L) return true;
		return date.toString().equals(that.date.toString());
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(client, accountNumber, accountType, initialAmount, isActive, transactionType, amount, balance);
	}
}
