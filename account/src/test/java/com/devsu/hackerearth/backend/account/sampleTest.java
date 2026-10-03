package com.devsu.hackerearth.backend.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.devsu.hackerearth.backend.account.client.ClientServiceClient;
import com.devsu.hackerearth.backend.account.controller.AccountController;
import com.devsu.hackerearth.backend.account.exception.InsufficientFundsException;
import com.devsu.hackerearth.backend.account.model.Account;
import com.devsu.hackerearth.backend.account.model.dto.AccountDto;
import com.devsu.hackerearth.backend.account.model.dto.TransactionDto;
import com.devsu.hackerearth.backend.account.repository.AccountRepository;
import com.devsu.hackerearth.backend.account.repository.TransactionRepository;
import com.devsu.hackerearth.backend.account.service.AccountService;
import com.devsu.hackerearth.backend.account.service.TransactionService;
import com.devsu.hackerearth.backend.account.service.TransactionServiceImpl;

@SpringBootTest
public class sampleTest {

	private AccountService accountService = mock(AccountService.class);
	private AccountController accountController = new AccountController(accountService);

	@Autowired(required = false)
	private AccountService accountServiceIntegration;

	@Autowired(required = false)
	private TransactionService transactionServiceIntegration;

	@Test
	void createAccountTest() {
		// Arrange
		AccountDto newAccount = new AccountDto(1L, "number", "savings", 0.0, true, 1L);
		AccountDto createdAccount = new AccountDto(1L, "number", "savings", 0.0, true, 1L);
		when(accountService.create(newAccount)).thenReturn(createdAccount);

		// Act
		ResponseEntity<AccountDto> response = accountController.create(newAccount);

		// Assert
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals(createdAccount, response.getBody());
	}

	@Test
	void insufficientBalanceThrowsExceptionTest() {
		// F3: Registro de transacciones con saldo insuficiente
		AccountRepository mockAccountRepo = mock(AccountRepository.class);
		TransactionRepository mockTxRepo = mock(TransactionRepository.class);
		ClientServiceClient mockClientServiceClient = mock(ClientServiceClient.class);

		TransactionServiceImpl txService = new TransactionServiceImpl(mockTxRepo, mockAccountRepo, mockClientServiceClient);

		Account account = new Account("478758", "Ahorro", 100.0, true, 1L);
		account.setId(1L);
		when(mockAccountRepo.findById(1L)).thenReturn(Optional.of(account));
		when(mockTxRepo.findTopByAccountIdOrderByIdDesc(1L)).thenReturn(Optional.empty());

		// Intento de retiro que supera el saldo (100 - 600 = -500)
		TransactionDto withdrawal = new TransactionDto(null, new Date(), "Retiro", -600.0, 0.0, 1L);

		InsufficientFundsException exception = assertThrows(
				InsufficientFundsException.class,
				() -> txService.create(withdrawal)
		);

		assertEquals("Saldo no disponible", exception.getMessage());
	}

	@Test
	void accountAndTransactionIntegrationTest() {
		// F6: Prueba de integracion completa para cuentas y transacciones
		if (accountServiceIntegration != null && transactionServiceIntegration != null) {
			AccountDto accDto = new AccountDto(null, "998877", "Corriente", 1000.0, true, 2L);
			AccountDto createdAcc = accountServiceIntegration.create(accDto);

			TransactionDto deposit = new TransactionDto(null, new Date(), "Deposito", 200.0, 0.0, createdAcc.getId());
			TransactionDto savedDeposit = transactionServiceIntegration.create(deposit);
			assertEquals(1200.0, savedDeposit.getBalance());

			TransactionDto withdrawal = new TransactionDto(null, new Date(), "Retiro", -500.0, 0.0, createdAcc.getId());
			TransactionDto savedWithdrawal = transactionServiceIntegration.create(withdrawal);
			assertEquals(700.0, savedWithdrawal.getBalance());
		}
	}

	@Test
	void concurrentTransactionsRaceConditionTest() throws InterruptedException {
		// Demostracion Senior: Prevencion de condiciones de carrera y consistencia ACID con base de datos H2
		if (accountServiceIntegration != null && transactionServiceIntegration != null) {
			AccountDto acc = accountServiceIntegration.create(new AccountDto(null, "CONC-" + System.currentTimeMillis(), "Ahorros", 100.0, true, 99L));
			
			int threads = 2;
			java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threads);
			java.util.concurrent.atomic.AtomicInteger successes = new java.util.concurrent.atomic.AtomicInteger(0);
			java.util.concurrent.atomic.AtomicInteger insufficientFunds = new java.util.concurrent.atomic.AtomicInteger(0);

			java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
			for (int i = 0; i < threads; i++) {
				executor.submit(() -> {
					try {
						transactionServiceIntegration.create(new TransactionDto(null, new Date(), "Retiro", -80.0, 0.0, acc.getId()));
						successes.incrementAndGet();
					} catch (InsufficientFundsException e) {
						insufficientFunds.incrementAndGet();
					} finally {
						latch.countDown();
					}
				});
			}
			latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
			executor.shutdown();

			// Con saldo inicial de 100, dos retiros simultaneos de 80: exactamente uno debe triunfar y el otro fallar
			assertEquals(1, successes.get(), "Solo una transaccion debio aprobarse para evitar double-spending");
			assertEquals(1, insufficientFunds.get(), "La segunda transaccion debio rechazarse por saldo insuficiente");

			TransactionDto last = transactionServiceIntegration.getLastByAccountId(acc.getId());
			assertEquals(20.0, last.getBalance(), "El saldo final debe ser exactamente 20.0 (100 - 80)");
		}
	}
}
