# Ramsiers QuickBooks Desktop invoice bridge

This module is the test integration path for creating Ramsiers invoices in QuickBooks Desktop 2024 through Intuit QuickBooks Web Connector (QBWC).

## Current scope

- Accept the same final customer/job/line-item totals used by the Ramsiers quote workflow.
- Preserve a stable `jobId` as the idempotency key so the same finished job cannot intentionally be queued twice.
- Build conservative qbXML 14.0 requests for customer lookup/add and `InvoiceAdd`.
- Keep bank account numbers, card data, Stripe secrets, and QuickBooks credentials out of the Android payload and GitHub.
- Require the invoice line total to exactly equal the final quote total before a request can be queued.

## Not live yet

Nothing in this folder is deployed and the Android app does not yet send production invoices. A live connection must not be enabled until the QuickBooks Desktop company file is backed up and a single test invoice is verified.

## Required next implementation steps

1. Add persistent queue storage for invoice jobs and enforce a unique key on `jobId`.
2. Implement the QBWC SOAP methods (`authenticate`, `sendRequestXML`, `receiveResponseXML`, `getLastError`, `closeConnection`, and version methods).
3. Add a `.qwc` configuration file that points only to the approved HTTPS bridge.
4. Map Ramsiers quote line names to existing QuickBooks Item names. Do not create or rename QuickBooks Items automatically without owner approval.
5. Wire the Android finished-job workflow to queue one invoice only after the job is marked finished and the user explicitly chooses the QuickBooks action during testing.
6. Back up the QuickBooks company file and create one clearly labeled test customer/invoice.
7. After the test is verified, enable automatic queuing for genuine finished jobs.

## Duplicate protection

The service must store these states for each `jobId`: `queued`, `customer_checked`, `invoice_sent`, `confirmed`, or `failed`. Once QuickBooks returns a successful `InvoiceAddRs` with a transaction ID, that `jobId` must never be submitted again unless an owner-only recovery action explicitly resets it.

## Tests

Run:

```bash
npm test
```

The tests verify cent-accurate money formatting, XML escaping, stable invoice references, and rejection of mismatched totals.
