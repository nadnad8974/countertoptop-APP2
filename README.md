# RAMSIER'S GRANITE AND QUARTZ Android App

Android test MVP for customer slab selection and countertop quote requests.

## Included in test version 1.83

- Scan MSI or other slab QR codes
- Show the exact installed app version in the top-left of every workflow page
- Keep all saved-job search, history, and shared-Drive actions inside one clean first-page drop-down
- Temporarily allow the testing workflow to advance past Customer name with blank fields; saving, signing, quote, calendar, email, and payment actions still require both names
- Save and remove slab selections
- Open saved MSI links
- Upload up to six countertop drawings; start AI automatically, analyze each separately, and add only complete, verified estimates
- Use the authenticated Ramsiers server-side GPT-5.6 Sol drawing reader without placing the OpenAI key in the app or GitHub
- Reject uncertain, contradictory, or unlinked AI measurements and ask for confirmation instead of pricing a guessed dimension
- Crop each camera or Gallery drawing with a protected edge margin, analyze the original-color copy first, and offer an enhanced black-and-white retry when the first result is incomplete
- Compare every original drawing with its own saved AI verification redraw
- Zoom, edit redraw dimensions, correct shape/opening types, and add your own short labels
- Keep the redraw controls visible while editing, move selected pieces with four arrow buttons, and resize from a fixed left/top edge so only the right/bottom edge extends
- Keep AI-written labels off the redraw; only labels you add yourself are shown
- Render slide-in stove openings with the four-burner symbol and no automatic word label
- Correct each formula piece as included, sink/cooktop, stove, or another deducted opening, and add a missing area correction
- Keep selected drawings, AI results, and edits after closing and reopening the app
- Enter multiple countertop sections or use compact L/W/T manual measurements
- Calculate square footage and estimated pricing
- Keep sink openings included
- Optionally subtract a slide-in stove opening
- Upload a kitchen/countertop photo
- Show product photos on the RAMSIER'S faucet and waterfall screens
- Ask whether the cabinets are in with simple Yes and No choices
- Email customer information, slabs, measurements, estimate, notes, and photo
- Save the office email, slab selections, and measurement sections locally
- Protect price, product-picture, and page-management changes with a locally hashed six-digit owner PIN
- Show every purchased line item on the review, final quote, printable PDF, signed copy, and payment JPEG
- After a job is marked finished, create one ACH-only Stripe Checkout link for the full quote and prepare manual text and/or email choices using the saved customer contact information
- Pick TP and IN dates and times with on-screen calendar/time controls before reviewing the event in Google Calendar
- Keep Chase banking balances out of the employee workflow; card entry stays in a separately permissioned Chase payment account
- Follow the approved 38-page customer-to-payment workflow, with the plumbing notice immediately after sink selection and no duplicate Schedule or Crop Drawing page
- Search the private phone-local job history by saved customer or project information
- Connect each authorized phone to one private shared Google Drive folder, save one folder per customer, and search or reopen all shared jobs
- Back up job answers, measurements, drawing results, drawings, countertop photos, and the signed final quote without storing bank/card numbers or Stripe secrets

Each uploaded drawing is treated as a separate countertop area. Do not upload two alternate photos of the same plan as separate drawings, because that would count the same area twice.

GitHub Actions runs tests and lint, builds and verifies the debug APK, and publishes the APK plus its SHA-256 checksum as downloadable workflow artifacts without rewriting the source branch.
