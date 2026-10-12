# מתחילים כאן

1. חלץ את כל תיקיית `MatrixLab` מה־ZIP.
2. פתח PowerShell בתוך התיקייה ובדוק שיש Java 17 ומעלה:

```powershell
java -version
```

3. הרץ את ההדגמה המוכנה:

```powershell
java -jar release/matrixlab-1.0.0.jar race 128 4 5 results 42
Start-Process .\results\matrix-race.html
```

4. כדי להריץ ביטוי מהדוגמאות:

```powershell
java -jar release/matrixlab-1.0.0.jar solve 4 examples/example1.json result.json
Get-Content result.json
```

5. כדי לפתח, לבדוק ולבנות מחדש צריך JDK 17 ומעלה ו־Maven:

```powershell
mvn clean verify
java -jar target/matrixlab-1.0.0.jar race 128 4 5 results 42
```

לא צריך Docker או Node.js. קרא את `docs/INTERVIEW_HE.md` להכנה לראיון ואת README להסברים ולמגבלות. תיקיית `demo-preview` מכילה דוח אמיתי מהרצת בדיקה ואפשר לפתוח אותו גם לפני התקנת Java.

לפני עדכון ריפו קיים, שמור את העבודה המקומית שלך וסקור את ההבדלים. אין צורך למחוק את היסטוריית Git הקיימת. ה־ZIP לא כולל היסטוריית Git, וההרחבות כאן טרם הועלו לריפו שלך.
