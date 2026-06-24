const fs = require('fs');
const { execSync } = require('child_process');

const files = execSync('grep -rlI "[А-Яа-я]" app/src/main/java/').toString().split('\n').filter(Boolean);

let allStrings = new Set();
const regex = /"([^"\\]*(?:\\.[^"\\]*)*[А-Яа-я]+[^"\\]*(?:\\.[^"\\]*)*)"/g;

files.forEach(file => {
    if (file.includes('UpdParser.kt') || file.includes('ExportService.kt')) return;
    const content = fs.readFileSync(file, 'utf-8');
    let match;
    while ((match = regex.exec(content)) !== null) {
        allStrings.add(match[1]);
    }
});

console.log(JSON.stringify(Array.from(allStrings), null, 2));
