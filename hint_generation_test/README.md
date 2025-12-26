# Smart Hint Generation Feature

This directory contains the tools and results for the AI-powered hint generation feature.

## Actions Taken So Far:
1.  **Environment Setup**:
    *   Created a `.env` file to securely store the `GEMINI_API_KEY`.
    *   Installed required Python libraries: `google-generativeai`, `python-dotenv`, `openpyxl`.
2.  **Script Development**:
    *   Developed `generate_hints_to_excel.py` to automate hint generation.
    *   Integrated **Gemini 2.0 Flash** (stable) for high-quality Bulgarian hints.
    *   Implemented strict rate limiting (10 RPM / 6.6s delay) to respect API quotas.
    *   Added logic to process multiple SQLite databases (`class8.db`, `class9.db`, etc.).
    *   Ensured progress is saved to Excel after every question to prevent data loss.
3.  **Verification**:
    *   Successfully ran a test batch of 10 questions from `class8.db`.
    *   Verified the output in `hints_batch_1_v4.xlsx`.
    *   Confirmed the hints are accurate, in Bulgarian, and follow the HINT1/HINT2 format.

## Files:
*   `generate_hints_to_excel.py`: The main automation script.
*   `hints_batch_1_v4.xlsx`: Results from the first verification batch.
*   `README.md`: This documentation file.

## Next Steps:
*   Run larger batches to cover all questions in the databases.
*   Import the generated hints back into the SQLite databases.
