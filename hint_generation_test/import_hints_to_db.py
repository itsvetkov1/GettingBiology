import openpyxl
import sqlite3
import os

def import_hints(excel_path, db_path, limit=15):
    print(f"Importing hints from {excel_path} to {db_path}...")
    
    # Load Excel
    wb = openpyxl.load_workbook(excel_path)
    ws = wb.active
    
    # Connect to DB
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    count = 0
    # Skip header
    for row in ws.iter_rows(min_row=2, values_only=True):
        if count >= limit:
            break
            
        q_id, q_text, q_options, q_correct, h1, h2, status, source_db = row
        
        if status == "Success":
            print(f"Updating ID {q_id}...")
            cursor.execute(
                "UPDATE questions SET hint1 = ?, hint2 = ? WHERE id = ?",
                (h1, h2, q_id)
            )
            count += 1
        else:
            print(f"Skipping ID {q_id} due to status: {status}")

    conn.commit()
    conn.close()
    print(f"Successfully updated {count} questions.")

if __name__ == "__main__":
    EXCEL_PATH = "hint_generation_test/hints_consolidated_1-6_gemini3.xlsx"
    DB_PATH = "app/src/main/assets/class8.db"
    import_hints(EXCEL_PATH, DB_PATH, limit=15)
