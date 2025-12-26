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
4.  **Extended Generation**:
    *   Generated 5 additional batches (`hints_batch_2.xlsx` to `hints_batch_6.xlsx`).
    *   Each batch contains 10 questions, covering IDs 12 to 61 from `class8.db`.
    *   Maintained the 6.6s delay and `gemini-2.0-flash` model for consistency.
5.  **Model Upgrade Testing**:
    *   Switched to **Gemini 3 Flash (Preview)** for Batch 7 to evaluate improved reasoning and Bulgarian linguistic accuracy.
    *   Generated `hints_batch_7.xlsx` (IDs 62-71) using the new model.

## Files:
*   `generate_hints_to_excel.py`: The main automation script (updated to support batching and offsets).
*   `hints_batch_1_v4.xlsx`: Results from the first verification batch.
*   `hints_batch_2.xlsx` - `hints_batch_6.xlsx`: Extended test batches for evaluation (Gemini 2.0 Flash).
*   `hints_batch_7.xlsx`: Comparison batch generated with Gemini 3 Flash (Preview).
*   `README.md`: This documentation file.

## Next Steps:
*   Run larger batches to cover all questions in the databases.
*   Import the generated hints back into the SQLite databases.
