'use strict';

const QBXML_VERSION = '14.0';

function xml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function moneyFromCents(cents) {
  if (!Number.isSafeInteger(cents) || cents < 0) {
    throw new Error('amountCents must be a non-negative safe integer');
  }
  return (cents / 100).toFixed(2);
}

function validateInvoice(invoice) {
  if (!invoice || invoice.schemaVersion !== 1) throw new Error('Unsupported invoice schema');
  if (!String(invoice.jobId || '').trim()) throw new Error('jobId is required');
  if (!String(invoice.customerName || '').trim()) throw new Error('customerName is required');
  if (!Array.isArray(invoice.lines) || invoice.lines.length === 0) throw new Error('At least one invoice line is required');
  if (!Number.isSafeInteger(invoice.totalCents) || invoice.totalCents <= 0) throw new Error('totalCents must be > 0');
  const lineTotal = invoice.lines.reduce((sum, line) => {
    if (!line || !String(line.itemName || '').trim()) throw new Error('Each line requires itemName');
    if (!Number.isSafeInteger(line.amountCents) || line.amountCents < 0) throw new Error('Invalid line amountCents');
    return sum + line.amountCents;
  }, 0);
  if (lineTotal !== invoice.totalCents) throw new Error('Invoice line total does not match totalCents');
}

function envelope(body) {
  return `<?xml version="1.0"?>\n<?qbxml version="${QBXML_VERSION}"?>\n<QBXML><QBXMLMsgsRq onError="stopOnError">${body}</QBXMLMsgsRq></QBXML>`;
}

function customerQuery(invoice) {
  validateInvoice(invoice);
  return envelope(`<CustomerQueryRq requestID="customer:${xml(invoice.jobId)}"><FullName>${xml(invoice.customerName)}</FullName></CustomerQueryRq>`);
}

function customerAdd(invoice) {
  validateInvoice(invoice);
  const phone = String(invoice.phone || '').trim();
  const email = String(invoice.email || '').trim();
  const address = String(invoice.projectAddress || '').trim();
  return envelope(`<CustomerAddRq requestID="customer-add:${xml(invoice.jobId)}"><CustomerAdd><Name>${xml(invoice.customerName)}</Name>${phone ? `<Phone>${xml(phone)}</Phone>` : ''}${email ? `<Email>${xml(email)}</Email>` : ''}${address ? `<BillAddress><Addr1>${xml(address)}</Addr1></BillAddress>` : ''}</CustomerAdd></CustomerAddRq>`);
}

function invoiceAdd(invoice, options = {}) {
  validateInvoice(invoice);
  const memo = String(invoice.memo || '').trim();
  const refNumber = String(options.refNumber || invoice.jobId).trim().slice(0, 11);
  const lineXml = invoice.lines.map((line) =>
    `<InvoiceLineAdd><ItemRef><FullName>${xml(line.itemName)}</FullName></ItemRef>${line.description ? `<Desc>${xml(line.description)}</Desc>` : ''}<Amount>${moneyFromCents(line.amountCents)}</Amount></InvoiceLineAdd>`
  ).join('');
  return envelope(`<InvoiceAddRq requestID="invoice:${xml(invoice.jobId)}"><InvoiceAdd><CustomerRef><FullName>${xml(invoice.customerName)}</FullName></CustomerRef><RefNumber>${xml(refNumber)}</RefNumber>${memo ? `<Memo>${xml(memo)}</Memo>` : ''}${lineXml}</InvoiceAdd></InvoiceAddRq>`);
}

module.exports = { QBXML_VERSION, xml, moneyFromCents, validateInvoice, customerQuery, customerAdd, invoiceAdd };
