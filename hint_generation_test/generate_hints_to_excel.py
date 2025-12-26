import os
import sqlite3
import time
import openpyxl
from openpyxl import Workbook
import google.generativeai as genai
from dotenv import load_dotenv

# Load environment variables
load_dotenv()
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

if not GEMINI_API_KEY:
    print("Error: GEMINI_API_KEY not found in .env file.")
    exit(1)

# Configure Gemini
genai.configure(api_key=GEMINI_API_KEY)
model = genai.GenerativeModel('gemini-2.0-flash')

# Constants
DATABASES = [
    "app/src/main/assets/class8.db",
    "app/src/main/assets/class9.db",
    "app/src/main/assets/class10.db",
    "app/src/main/assets/db_entrance_exam.db"
]
OUTPUT_FILE = "hints_batch_1_v4.xlsx"
RPM_LIMIT = 10
DELAY_BETWEEN_CALLS = 6.6
DAILY_LIMIT = 1400
BATCH_SIZE = 10  # For verification

def get_questions_from_db(db_path, limit=None, offset=0):
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    query = "SELECT id, questionText, options, correctAnswer FROM questions"
    if limit:
        query += f" LIMIT {limit}"
    if offset:
        query += f" OFFSET {offset}"
    cursor.execute(query)
    rows = cursor.fetchall()
    conn.close()
    return rows

def generate_hints(question, options, correct_answer):
    prompt = f"""
    Ти си експерт по биология. Генерирай две кратки подсказки (hints) на български език за следния въпрос:
    Въпрос: {question}
    Опции: {options}
    Правилен отговор: {correct_answer}

    Изисквания:
    1. Подсказките трябва да са на български език.
    2. Първата подсказка (HINT1) трябва да е по-обща.
    3. Втората подсказка (HINT2) трябва да е по-конкретна, но без да казва директно отговора.
    4. Форматът на отговора ТРЯБВА да бъде точно такъв:
    HINT1: [първа подсказка]
    HINT2: [втора подсказка]
    """
    
    max_retries = 3
    for attempt in range(max_retries):
        try:
            response = model.generate_content(prompt)
            text = response.text.strip()
            
            hint1 = ""
            hint2 = ""
            
            for line in text.split('\n'):
                if line.startswith("HINT1:"):
                    hint1 = line.replace("HINT1:", "").strip().strip('[]')
                elif line.startswith("HINT2:"):
                    hint2 = line.replace("HINT2:", "").strip().strip('[]')
            
            return hint1, hint2, "Success"
        except Exception as e:
            error_str = str(e)
            if "429" in error_str or "RESOURCE_EXHAUSTED" in error_str:
                wait_time = (2 ** attempt) * 15  # 15s, 30s, 60s
                print(f"Rate limited (429). Waiting {wait_time}s before retry {attempt + 1}/{max_retries}...")
                time.sleep(wait_time)
                continue
            print(f"Error generating hints: {e}")
            return "", "", f"Error: {error_str[:100]}"
    
    return "", "", "Error: Max retries exceeded (429)"

def run_batch(batch_num, start_offset, batch_size):
    output_file = f"hints_batch_{batch_num}.xlsx"
    print(f"\n=== Starting Batch {batch_num} (Offset: {start_offset}) ===")
    
    wb = Workbook()
    ws = wb.active
    ws.title = "Hints"
    ws.append(["ID", "Question", "Options", "Correct Answer", "Hint1", "Hint2", "Status", "Source DB"])

    total_processed = 0
    current_offset = start_offset
    
    for db_path in DATABASES:
        if total_processed >= batch_size:
            break
            
        print(f"Processing database: {db_path}")
        # For simplicity, we assume we are still in the first DB for these small batches
        # In a real scenario, we'd need more complex offset tracking across DBs
        questions = get_questions_from_db(db_path, limit=batch_size - total_processed, offset=current_offset)
        
        for q_id, q_text, q_options, q_correct in questions:
            print(f"Generating hints for ID {q_id} in {db_path}...")
            h1, h2, status = generate_hints(q_text, q_options, q_correct)
            
            ws.append([q_id, q_text, q_options, q_correct, h1, h2, status, os.path.basename(db_path)])
            total_processed += 1
            
            wb.save(output_file)
            
            if total_processed < batch_size:
                print(f"Waiting {DELAY_BETWEEN_CALLS:.2f}s...")
                time.sleep(DELAY_BETWEEN_CALLS)
    
    print(f"Finished Batch {batch_num}. Saved to {output_file}")
    return total_processed

def main():
    # We already did batch 1 (offset 0, size 10)
    # Now we do 5 more batches of 10 questions each
    batch_size = 10
    for i in range(2, 7):
        offset = (i - 1) * batch_size
        run_batch(i, offset, batch_size)

if __name__ == "__main__":
    main()
