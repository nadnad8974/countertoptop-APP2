# Ramsier's secure payment service

This small Vercel Function creates a one-time Stripe Checkout Session for a completed countertop job.

The Android app sends only:

- `amountCents`: the full final quote amount in US cents
- `quoteReference`: a random opaque reference such as `rq_...`

The service rejects all additional fields, including customer names, phone numbers, addresses, and drawings. Stripe credentials are read only from the server-side `STRIPE_RESTRICTED_KEY` environment variable.

## Production setup

1. Deploy this `payment-service` directory as its own Vercel project.
2. Add a least-privilege Stripe restricted key as the sensitive production environment variable `STRIPE_RESTRICTED_KEY`.
3. Set `PUBLIC_BASE_URL` to the production HTTPS origin.
4. Enable **US bank account (ACH Direct Debit)** in Stripe's payment-method settings. Checkout uses dynamic payment methods and does not hardcode a client-side payment method list.

Never commit a real Stripe key or place one in the Android app.

## Verification

Run:

```bash
npm ci
npm test
npm audit --omit=dev
```
