'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const { invoiceAdd, customerAdd, moneyFromCents } = require('../lib/qbxml');

const invoice = {
  schemaVersion: 1,
  jobId: 'job_123456789',
  customerName: 'Test & Customer',
  phone: '440-555-0100',
  email: 'test@example.com',
  projectAddress: '123 Main St',
  memo: 'Ramsiers countertop job',
  totalCents: 125000,
  lines: [
    { itemName: 'Countertop and installation', description: '20 sq. ft.', amountCents: 120000 },
    { itemName: 'Sink cutout', description: '1 cutout', amountCents: 5000 }
  ]
};

test('formats exact cents as QuickBooks money', () => {
  assert.equal(moneyFromCents(125000), '1250.00');
});

test('builds escaped InvoiceAdd with stable job reference', () => {
  const value = invoiceAdd(invoice);
  assert.match(value, /<CustomerRef><FullName>Test &amp; Customer<\/FullName><\/CustomerRef>/);
  assert.match(value, /<Amount>1200\.00<\/Amount>/);
  assert.match(value, /<Amount>50\.00<\/Amount>/);
  assert.match(value, /<RefNumber>job_1234567<\/RefNumber>/);
});

test('builds CustomerAdd without payment credentials', () => {
  const value = customerAdd(invoice);
  assert.match(value, /<Phone>440-555-0100<\/Phone>/);
  assert.match(value, /<Email>test@example\.com<\/Email>/);
  assert.doesNotMatch(value, /routing|account number|card/i);
});

test('rejects a total that does not equal the invoice lines', () => {
  assert.throws(() => invoiceAdd({ ...invoice, totalCents: 125001 }), /does not match/);
});
