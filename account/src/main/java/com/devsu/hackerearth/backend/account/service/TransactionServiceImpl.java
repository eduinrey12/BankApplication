package com.devsu.hackerearth.backend.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

	// Bloqueo concurrente por ID de cuenta a nivel de JVM para defensa en profundidad
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
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public TransactionDto create(TransactionDto transactionDto) {
		// Bloqueo pesimista a nivel de base de datos (SELECT ... FOR UPDATE) para cluster / pods distribuidos
		Account account = accountRepository.findByIdWithLock(transactionDto.getAccountId())
				.or(() -> accountRepository.findById(transactionDto.getAccountId()))
				.orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con id: " + transactionDto.getAccountId()));

		// Defensa en profundidad: sincronizacion JVM combinada con bloqueo pesimista en DB
		synchronized (accountLocks.computeIfAbsent(account.getId(), k -> new Object())) {
			BigDecimal currentBalance = BigDecimal.valueOf(account.getInitialAmount());
			Optional<Transaction> lastTx = transactionRepository.findTopByAccountIdOrderByIdDesc(account.getId());
			if (lastTx.isPresent()) {
				currentBalance = BigDecimal.valueOf(lastTx.get().getBalance());
			}

			// Calculo monetario formal con BigDecimal y redondeo Half-Even (estandar bancario internacional)
			BigDecimal movementAmount = BigDecimal.valueOf(transactionDto.getAmount()).setScale(2, RoundingMode.HALF_EVEN);
			BigDecimal newBalance = currentBalance.add(movementAmount).setScale(2, RoundingMode.HALF_EVEN);

			if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
				throw new InsufficientFundsException("Saldo no disponible");
			}

			Transaction tx = new Transaction();
			tx.setDate(transactionDto.getDate() != null ? transactionDto.getDate() : new Date());
			tx.setAmount(movementAmount.doubleValue());
			tx.setBalance(newBalance.doubleValue());
			tx.setAccountId(account.getId());

			if (transactionDto.getType() != null && !transactionDto.getType().trim().isEmpty()) {
				tx.setType(transactionDto.getType());
			} else {
				tx.setType(movementAmount.signum() >= 0 ? "Deposito" : "Retiro");
			}

			Transaction savedTx = transactionRepository.saveAndFlush(tx);
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

		List<Long> accountIds = accounts.stream().map(Account::getId).collect(Collectors.toList());
		Map<Long, Account> accountMap = accounts.stream().collect(Collectors.toMap(Account::getId, Function.identity()));

		// Optimizacion Senior: eliminacion del problema N+1 mediante consulta agrupada en una sola transaccion
		List<Transaction> transactions = transactionRepository.findByAccountIdInAndDateBetween(accountIds, start, end);
		String clientName = clientServiceClient.getClientName(clientId);
		List<BankStatementDto> report = new ArrayList<>();

		for (Transaction tx : transactions) {
			Account account = accountMap.get(tx.getAccountId());
			if (account != null) {
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

	@Override
	public TransactionDto update(TransactionDto transactionDto) {
		Transaction tx = transactionRepository.findById(transactionDto.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Transaccion no encontrada con id: " + transactionDto.getId()));
		if (transactionDto.getType() != null) tx.setType(transactionDto.getType());
		if (transactionDto.getDate() != null) tx.setDate(transactionDto.getDate());
		return mapToDto(transactionRepository.save(tx));
	}

	@Override
	public TransactionDto partialUpdate(Long id, TransactionDto transactionDto) {
		Transaction tx = transactionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transaccion no encontrada con id: " + id));
		if (transactionDto.getType() != null) tx.setType(transactionDto.getType());
		if (transactionDto.getDate() != null) tx.setDate(transactionDto.getDate());
		return mapToDto(transactionRepository.save(tx));
	}

	@Override
	public void deleteById(Long id) {
		Transaction tx = transactionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transaccion no encontrada con id: " + id));
		transactionRepository.delete(tx);
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
