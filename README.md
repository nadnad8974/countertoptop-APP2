# RAMSIER'S GRANITE AND QUARTZ Android App

Android test MVP for customer slab selection and countertop quote requests.

## Included in test version 1.54

- Scan MSI or other slab QR codes
- Save and remove slab selections
- Open saved MSI links
- Upload up to six countertop drawings; start AI automatically, analyze each separately, and add only complete, verified estimates
- Compare every original drawing with its own saved AI verification redraw
- Zoom, edit redraw dimensions, correct shape/opening types, and add your own short labels
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
- After a job is marked finished, create an ACH-only Stripe Checkout link for the full quote and place it in the prepared manual text message with the quote JPEG

Each uploaded drawing is treated as a separate countertop area. Do not upload two alternate photos of the same plan as separate drawings, because that would count the same area twice.

GitHub Actions runs tests and lint, builds and verifies the debug APK, and publishes the APK plus its SHA-256 checksum as downloadable workflow artifacts without rewriting the source branch.
