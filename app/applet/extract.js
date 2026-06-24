const fs = require('fs');
const { execSync } = require('child_process');

const files = execSync('grep -rlI "[А-Яа-я]" app/src/main/java/').toString().split('\n').filter(Boolean);

let stringMap = {};
let counter = 1;

const regex = /"([^"\\]*(?:\\.[^"\\]*)*[А-Яа-я]+[^"\\]*(?:\\.[^"\\]*)*)"/g;

files.forEach(file => {
    if (file.includes('UpdParser.kt') || file.includes('ExportService.kt') || file.includes('AppDatabase.kt')) return;
    let content = fs.readFileSync(file, 'utf-8');
    
    let match;
    while ((match = regex.exec(content)) !== null) {
        let str = match[1];
        if (!stringMap[str]) {
            stringMap[str] = 'str_' + counter++;
        }
    }
});

console.log(JSON.stringify(stringMap, null, 2));
