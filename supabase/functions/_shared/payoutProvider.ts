// ============================================================
// Interfaz agnóstica de desembolso (money-out) a profesores.
// ------------------------------------------------------------
// MercadoPago no tiene API de money-out (confirmado: su documentación dice
// que su API es solo para vender productos/servicios, no para transferir
// entre cuentas). El desembolso real se hace con un proveedor distinto —
// Fintoc (open banking chileno, transferencia directa a datos bancarios) —
// detrás de esta interfaz, para no repetir el vendor lock-in si en el
// futuro cambia el proveedor de payouts.
//
// Scaffold: todavía sin implementación real (pendiente de credenciales de
// Fintoc). `process-payouts` sigue usando el stub de Fase 0 hasta que se
// implemente `FintocPayoutProvider`.
// ============================================================

/** Datos bancarios del destinatario del pago. Mismo shape que `refund_methods`. */
export interface PayoutRecipient {
  holderName: string;
  holderRut: string;
  bank: string;
  accountType: string;
  accountNumber: string;
}

export interface SendPayoutArgs {
  recipient: PayoutRecipient;
  amount: number;
  /** Referencia idempotente del lado de la plataforma (ej. teacher_payouts.id). */
  externalReference: string;
}

export type PayoutStatus = "PENDING" | "PAID" | "FAILED";

export interface SendPayoutResult {
  providerReference: string;
  status: PayoutStatus;
}

export interface PayoutProvider {
  readonly name: string;
  sendPayout(args: SendPayoutArgs): Promise<SendPayoutResult>;
}
