package com.devsu.hackerearth.backend.account.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.devsu.hackerearth.backend.account.exception.ResourceNotFoundException;
import com.devsu.hackerearth.backend.account.model.Account;
import com.devsu.hackerearth.backend.account.model.dto.AccountDto;
import com.devsu.hackerearth.backend.account.model.dto.PartialAccountDto;
import com.devsu.hackerearth.backend.account.repository.AccountRepository;

@Service
public class AccountServiceImpl implements AccountService {

	private final AccountRepository accountRepository;

	public AccountServiceImpl(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Override
	public List<AccountDto> getAll() {
		return accountRepository.findAll()
				.stream()
				.map(this::mapToDto)
				.collect(Collectors.toList());
	}

	@Override
	public AccountDto getById(Long id) {
		Account account = accountRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con id: " + id));
		return mapToDto(account);
	}

	@Override
	public AccountDto create(AccountDto accountDto) {
		Account account = mapToEntity(accountDto);
		account.setId(null);
		Account savedAccount = accountRepository.save(account);
		return mapToDto(savedAccount);
	}

	@Override
	public AccountDto update(AccountDto accountDto) {
		if (accountDto == null || accountDto.getId() == null) {
			return null;
		}
		Account existingAccount = accountRepository.findById(accountDto.getId()).orElse(null);
		if (existingAccount == null) {
			return null;
		}

		existingAccount.setNumber(accountDto.getNumber());
		existingAccount.setType(accountDto.getType());
		existingAccount.setInitialAmount(accountDto.getInitialAmount());
		existingAccount.setActive(accountDto.isActive());
		existingAccount.setClientId(accountDto.getClientId());

		Account updatedAccount = accountRepository.save(existingAccount);
		return mapToDto(updatedAccount);
	}

	@Override
	public AccountDto partialUpdate(Long id, PartialAccountDto partialAccountDto) {
		Account existingAccount = accountRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con id: " + id));

		existingAccount.setActive(partialAccountDto.isActive());
		Account updatedAccount = accountRepository.save(existingAccount);
		return mapToDto(updatedAccount);
	}

	@Override
	public void deleteById(Long id) {
		Account existingAccount = accountRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con id: " + id));
		accountRepository.delete(existingAccount);
	}

	private AccountDto mapToDto(Account account) {
		return new AccountDto(
				account.getId(),
				account.getNumber(),
				account.getType(),
				account.getInitialAmount(),
				account.isActive(),
				account.getClientId()
		);
	}

	private Account mapToEntity(AccountDto accountDto) {
		Account account = new Account();
		account.setId(accountDto.getId());
		account.setNumber(accountDto.getNumber());
		account.setType(accountDto.getType());
		account.setInitialAmount(accountDto.getInitialAmount());
		account.setActive(accountDto.isActive());
		account.setClientId(accountDto.getClientId());
		return account;
	}
}
