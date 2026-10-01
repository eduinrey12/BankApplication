package com.devsu.hackerearth.backend.account.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsu.hackerearth.backend.account.client.ClientServiceClient;
import com.devsu.hackerearth.backend.account.exception.InsufficientFundsException;
import com.devsu.hackerearth.backend.account.exception.ResourceNotFoundException;
import com.devsu.hackerearth.backend.account.model.Account;
import com.devsu.hackerearth.backend.account.model.Transaction;
import com.devsu.hackerearth.backend.account.model.dto.BankStatementDto;
import com.devsu.hackerearth.backend.account.model.dto.TransactionDto;
import com.devsu.hackerearth.backend.account.repository.AccountRepository;
import com.devsu.hackerearth.backend.account.repository.TransactionRepository;

@Service
public class TransactionServiceImpl implements TransactionService {

	private final TransactionRepository transactionRepository;
	private final AccountRepository accountRepository;
	private final ClientServiceClient clientServiceClient;

	// Bloqueo concurrente por ID de cuenta para evitar condiciones de carrera en transacciones simultaneas
	private final ConcurrentHashMap<Long, Object> accountLocks = new ConcurrentHashMap<>();

	public TransactionServiceImpl(
			TransactionRepository transactionRepository,
			AccountRepository accountRepository,
			ClientServiceClient clientServiceClient) {
		this.transactionRepository = transactionRepository;
		this.accountRepository = accountRepository;
		this.clientServiceClient = clientServiceClient;
	}

	@Override
	public List<TransactionDto> getAll() {
		return transactionRepository.findAll()
				.stream()
				.map(this::mapToDto)
				.collect(Collectors.toList());
	}

	@Override
	public TransactionDto getById(Long id) {
		Transaction tx = transactionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transaccion no encontrada con id: " + id));
		return mapToDto(tx);
	}

	@Override
	@Transactional
	public TransactionDto create(TransactionDto transactionDto) {
		Account account = accountRepository.findById(transactionDto.getAccountId())
				.orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con id: " + transactionDto.getAccountId()));

		// Sincronizacion por cuenta: garantiza consistencia transaccional bajo alta concurrencia
		synchronized (accountLocks.computeIfAbsent(account.getId(), k -> new Object())) {
			Optional<Transaction> lastTx = transactionRepository.findTopByAccountIdOrderByIdDesc(account.getId());
			double currentBalance = lastTx.map(Transaction::getBalance).orElse(account.getInitialAmount());

			double movementAmount = transactionDto.getAmount();
			double newBalance = currentBalance + movementAmount;

			if (newBalance < 0) {
				throw new InsufficientFundsException("Saldo no disponible");
			}

			Transaction tx = new Transaction();
			tx.setDate(transactionDto.getDate() != null ? transactionDto.getDate() : new Date());
			tx.setAmount(movementAmount);
			tx.setBalance(newBalance);
			tx.setAccountId(account.getId());

			if (transactionDto.getType() != null && !transactionDto.getType().trim().isEmpty()) {
				tx.setType(transactionDto.getType());
			} else {
				tx.setType(movementAmount >= 0 ? "Deposito" : "Retiro");
			}

			Transaction savedTx = transactionRepository.save(tx);
			return mapToDto(savedTx);
		}
	}

	@Override
	public List<BankStatementDto> getAllByAccountClientIdAndDateBetween(
			Long clientId,
			Date dateTransactionStart,
			Date dateTransactionEnd) {

		Calendar calStart = Calendar.getInstance();
		calStart.setTime(dateTransactionStart);
		calStart.set(Calendar.HOUR_OF_DAY, 0);
		calStart.set(Calendar.MINUTE, 0);
		calStart.set(Calendar.SECOND, 0);
		calStart.set(Calendar.MILLISECOND, 0);
		Date start = calStart.getTime();

		Calendar calEnd = Calendar.getInstance();
		calEnd.setTime(dateTransactionEnd);
		calEnd.set(Calendar.HOUR_OF_DAY, 23);
		calEnd.set(Calendar.MINUTE, 59);
		calEnd.set(Calendar.SECOND, 59);
		calEnd.set(Calendar.MILLISECOND, 999);
		Date end = calEnd.getTime();

		List<Account> accounts = accountRepository.findByClientId(clientId);
		if (accounts == null || accounts.isEmpty()) {
			return new ArrayList<>();
		}

		String clientName = clientServiceClient.getClientName(clientId);
		List<BankStatementDto> report = new ArrayList<>();

		for (Account account : accounts) {
			List<Transaction> transactions = transactionRepository.findByAccountIdAndDateBetween(account.getId(), start, end);
			for (Transaction tx : transactions) {
				BankStatementDto statement = new BankStatementDto(
						tx.getDate(),
						clientName,
						account.getNumber(),
						account.getType(),
						account.getInitialAmount(),
						account.isActive(),
						tx.getType(),
						tx.getAmount(),
						tx.getBalance()
				);
				report.add(statement);
			}
		}

		return report;
	}

	@Override
	public TransactionDto getLastByAccountId(Long accountId) {
		return transactionRepository.findTopByAccountIdOrderByIdDesc(accountId)
				.map(this::mapToDto)
				.orElse(null);
	}

	private TransactionDto mapToDto(Transaction tx) {
		return new TransactionDto(
				tx.getId(),
				tx.getDate(),
				tx.getType(),
				tx.getAmount(),
				tx.getBalance(),
				tx.getAccountId()
		);
	}
}
