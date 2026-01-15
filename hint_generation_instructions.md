# Smart Hints Generation - Agent Instructions

## Objective
Extract all questions from the "Знам ли?" quiz app databases, generate 2 AI-powered hints for each using Gemini 2.0 Flash free tier, and export everything to an Excel file for review.

---

## Project Context

**App:** "Знам ли?" - Bulgarian biology quiz app for grades 8-12  
**Database files location:** `app/src/main/assets/`  
**Database files:**
- `class8.db`
- `class9.db`
- `class10.db`
- `db_entrance_exam.db`

**Table schema:**
```sql
CREATE TABLE questions (
    id INTEGER PRIMARY KEY,
    questionText TEXT,
    options TEXT,  -- semicolon-separated: "option1;option2;option3;option4"
    correctAnswer TEXT
);
```

**Gemini API key:** Located in `.env` file

**Output:** Excel file with all questions, options, correct answers, and generated hints for manual review.

---

## Task: Create Hint Generation Script with Excel Export

Create a Python script `generate_hints_to_excel.py` in the project root.

### Requirements
```bash
pip install google-generativeai python-dotenv openpyxl
```

### Script Logic

1. **Load environment:** Read Gemini API key from `.env`

2. **Rate limiting:** 
   - Gemini free tier: 15 RPM, 1,500 RPD
   - Use 4.4 seconds delay between requests (15 RPM = 4 sec, +10% = 4.4 sec)
   - Track daily count, stop at 1,400 to stay safe

3. **For each database file:**
   - Connect to SQLite
   - Fetch all questions
   - For each question, call Gemini API to generate hints
   - Collect all data in memory

4. **Export to Excel:**
   - Create workbook with one sheet per grade level
   - Columns: ID, Question, Option1, Option2, Option3, Option4, Correct Answer, Hint1, Hint2, Status
   - Save as `hints_review.xlsx`

5. **Prompt template for Gemini:**

```python
HINT_PROMPT = """Ти си учител по биология. Ученик трябва да отговори на следния въпрос:

Въпрос: {question}

Правилен отговор: {correct_answer}

Генерирай 2 подсказки на български език, които да насочат ученика към правилния отговор БЕЗ да го разкриват директно.

Подсказка 1: Трябва да е по-обща, да насочи мисленето в правилната посока.
Подсказка 2: Трябва да е по-конкретна, да използва аналогия или ключова дума.

Всяка подсказка трябва да е 1-2 изречения.

Отговори САМО в следния формат:
HINT1: [първа подсказка]
HINT2: [втора подсказка]
"""
```

6. **Parse response:** Extract text after `HINT1:` and `HINT2:`

7. **Error handling:**
   - If API fails, mark status as "ERROR" and continue
   - If parsing fails, mark status as "PARSE_ERROR"
   - Always include question in Excel even if hints failed

### Script Structure

```python
import os
import re
import time
import sqlite3
from dotenv import load_dotenv
import google.generativeai as genai
from openpyxl import Workbook
from openpyxl.styles import Font, Alignment
from openpyxl.utils import get_column_letter

load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
model = genai.GenerativeModel("gemini-2.0-flash")

DATABASES = {
    "8-ми клас": "app/src/main/assets/class8.db",
    "9-ти клас": "app/src/main/assets/class9.db",
    "10-ти клас": "app/src/main/assets/class10.db",
    "Кандидатстудентски": "app/src/main/assets/db_entrance_exam.db"
}

DELAY_SECONDS = 4.4  # 15 RPM + 10% buffer
DAILY_LIMIT = 1400   # Stay under 1,500 RPD
OUTPUT_FILE = "hints_review.xlsx"

HINT_PROMPT = """Ти си учител по биология. Ученик трябва да отговори на следния въпрос:

Въпрос: {question}

Правилен отговор: {correct_answer}

Генерирай 2 подсказки на български език, които да насочат ученика към правилния отговор БЕЗ да го разкриват директно.

Подсказка 1: Трябва да е по-обща, да насочи мисленето в правилната посока.
Подсказка 2: Трябва да е по-конкретна, да използва аналогия или ключова дума.

Всяка подсказка трябва да е 1-2 изречения.

Отговори САМО в следния формат:
HINT1: [първа подсказка]
HINT2: [втора подсказка]
"""

def generate_hints(question: str, correct_answer: str) -> tuple[str, str, str]:
    """Returns (hint1, hint2, status)"""
    prompt = HINT_PROMPT.format(question=question, correct_answer=correct_answer)
    try:
        response = model.generate_content(prompt)
        text = response.text
        
        hint1_match = re.search(r"HINT1:\s*(.+?)(?=HINT2:|$)", text, re.DOTALL)
        hint2_match = re.search(r"HINT2:\s*(.+?)$", text, re.DOTALL)
        
        if hint1_match and hint2_match:
            return hint1_match.group(1).strip(), hint2_match.group(1).strip(), "OK"
        return "", "", "PARSE_ERROR"
    except Exception as e:
        print(f"API error: {e}")
        return "", "", f"ERROR: {str(e)[:50]}"

def process_database(db_path: str, daily_count: int) -> tuple[list, int]:
    """Returns (rows_data, updated_daily_count)"""
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    cursor.execute("SELECT id, questionText, options, correctAnswer FROM questions")
    questions = cursor.fetchall()
    conn.close()
    
    print(f"Processing {db_path}: {len(questions)} questions")
    
    rows = []
    for q_id, question, options, correct in questions:
        # Split options
        opts = options.split(";")
        opt1 = opts[0] if len(opts) > 0 else ""
        opt2 = opts[1] if len(opts) > 1 else ""
        opt3 = opts[2] if len(opts) > 2 else ""
        opt4 = opts[3] if len(opts) > 3 else ""
        
        if daily_count >= DAILY_LIMIT:
            hint1, hint2, status = "", "", "LIMIT_REACHED"
        else:
            hint1, hint2, status = generate_hints(question, correct)
            daily_count += 1
            time.sleep(DELAY_SECONDS)
        
        rows.append([q_id, question, opt1, opt2, opt3, opt4, correct, hint1, hint2, status])
        print(f"  Question {q_id}: {status}")
    
    return rows, daily_count

def create_excel(all_data: dict):
    """Create Excel workbook with one sheet per grade"""
    wb = Workbook()
    wb.remove(wb.active)  # Remove default sheet
    
    headers = ["ID", "Въпрос", "Опция 1", "Опция 2", "Опция 3", "Опция 4", 
               "Правилен отговор", "Подсказка 1", "Подсказка 2", "Статус"]
    
    for sheet_name, rows in all_data.items():
        ws = wb.create_sheet(title=sheet_name)
        
        # Header row
        for col, header in enumerate(headers, 1):
            cell = ws.cell(row=1, column=col, value=header)
            cell.font = Font(bold=True)
            cell.alignment = Alignment(wrap_text=True)
        
        # Data rows
        for row_idx, row_data in enumerate(rows, 2):
            for col_idx, value in enumerate(row_data, 1):
                cell = ws.cell(row=row_idx, column=col_idx, value=value)
                cell.alignment = Alignment(wrap_text=True, vertical="top")
        
        # Adjust column widths
        column_widths = [5, 50, 30, 30, 30, 30, 30, 40, 40, 15]
        for i, width in enumerate(column_widths, 1):
            ws.column_dimensions[get_column_letter(i)].width = width
    
    wb.save(OUTPUT_FILE)
    print(f"\nExcel file saved: {OUTPUT_FILE}")

def main():
    daily_count = 0
    all_data = {}
    
    for sheet_name, db_path in DATABASES.items():
        if daily_count >= DAILY_LIMIT:
            print(f"Daily limit reached. Skipping {sheet_name}")
            # Still include questions without hints
            conn = sqlite3.connect(db_path)
            cursor = conn.cursor()
            cursor.execute("SELECT id, questionText, options, correctAnswer FROM questions")
            questions = cursor.fetchall()
            conn.close()
            
            rows = []
            for q_id, question, options, correct in questions:
                opts = options.split(";")
                rows.append([q_id, question, 
                           opts[0] if len(opts) > 0 else "",
                           opts[1] if len(opts) > 1 else "",
                           opts[2] if len(opts) > 2 else "",
                           opts[3] if len(opts) > 3 else "",
                           correct, "", "", "LIMIT_REACHED"])
            all_data[sheet_name] = rows
        else:
            rows, daily_count = process_database(db_path, daily_count)
            all_data[sheet_name] = rows
    
    create_excel(all_data)
    print(f"\nTotal API calls: {daily_count}")
    
    # Summary
    for sheet_name, rows in all_data.items():
        ok_count = sum(1 for r in rows if r[9] == "OK")
        print(f"  {sheet_name}: {ok_count}/{len(rows)} hints generated")

if __name__ == "__main__":
    main()
```

---

## Execution

1. Install dependencies: `pip install google-generativeai python-dotenv openpyxl`
2. Run: `python generate_hints_to_excel.py`
3. If daily limit reached, re-run next day (questions without hints show "LIMIT_REACHED" status)
4. Review `hints_review.xlsx` - each grade level is a separate sheet

---

## Excel Output Format

| ID | Въпрос | Опция 1 | Опция 2 | Опция 3 | Опция 4 | Правилен отговор | Подсказка 1 | Подсказка 2 | Статус |
|----|--------|---------|---------|---------|---------|------------------|-------------|-------------|--------|
| 1 | Каква е... | Синтез на... | Съхранение... | Транспорт... | Регулиране... | Съхранение... | Помисли какво... | Вакуолата е... | OK |

**Status values:**
- `OK` - Hints generated successfully
- `PARSE_ERROR` - Gemini response didn't match expected format
- `ERROR: ...` - API call failed
- `LIMIT_REACHED` - Daily quota exhausted

---

## Notes

- Script processes all questions even if limit is reached (just marks status)
- Excel file allows manual review and editing before importing to app
- Re-run script to fill in missing hints after quota resets
- Total questions across all DBs determines days needed (1,400/day max)
