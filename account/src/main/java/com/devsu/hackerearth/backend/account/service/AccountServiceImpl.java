package com.devsu.hackerearth.backend.account.service;

import java.util.List;

import org.springframework.stereotype.Service;

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
        // Get all accounts
		return null;
    }

    @Override
    public AccountDto getById(Long id) {
        // Get accounts by id
		return null;
    }

    @Override
    public AccountDto create(AccountDto accountDto) {
        // Create account
		return null;
    }

    @Override
    public AccountDto update(AccountDto accountDto) {
        // Update account
		return null;
    }

    @Override
    public AccountDto partialUpdate(Long id, PartialAccountDto partialAccountDto) {
        // Partial update account
		return null;
    }

    @Override
    public void deleteById(Long id) {
        // Delete account
    }
    
}
