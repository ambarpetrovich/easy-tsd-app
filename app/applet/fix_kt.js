const fs = require('fs');
const glob = require('glob');

const replacements = [
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_1\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_1, codesCount)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_5\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_5, status)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_17\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_17, androidx.compose.ui.res.stringResource(op.type.displayNameRes), dateStr, op.sessionName)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_19\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_19, code)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_20\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_20, warnings.size, androidx.compose.ui.res.stringResource(selectedType!!.displayNameRes))'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_22\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_22, androidx.compose.ui.res.stringResource(selectedType!!.displayNameRes))'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_50\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_50, product.product.name)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_51\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_51, gtinCodes.size)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_60\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_60, androidx.compose.ui.res.stringResource(currentAccounting.displayNameRes))'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_64\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_64, version.first)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_111\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_111, barcode.type)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_112\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_112, barcode.value)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_139\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_139, filenames.size)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_159\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_159, session.totalCodes)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_161\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_161, code.pageNumber)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_162\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_162, truncateMiddle(fileName, 30))'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_164\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_164, scans.size)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_166\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_166, scannedCodes.size)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_167\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_167, product!!.product.name)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_171\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_171, (session!!.progress * 100).toInt())'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_176\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_176, file.pages, file.codesCount)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_178\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_178, session!!.files.size)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_179\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_179, session!!.totalCodes)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_180\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_180, androidx.compose.ui.res.stringResource(session!!.status.displayNameRes))'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_184\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_184, filenames.size)'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_191\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_191, selectedFiles.size + 1, exts.random())'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_196\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_196, (session!!.progress * 100).toInt())'
    },
    {
        rx: /androidx\.compose\.ui\.res\.stringResource\(com\.example\.R\.string\.str_201\)/g,
        rep: 'androidx.compose.ui.res.stringResource(com.example.R.string.str_201, reconnectAttempts, MAX_RECONNECT_ATTEMPTS)'
    }
];

glob('../app/src/main/java/**/*.kt', (err, files) => {
    if (err) throw err;
    files.forEach(file => {
        let content = fs.readFileSync(file, 'utf8');
        let modified = false;
        
        replacements.forEach(r => {
            if (r.rx.test(content)) {
                content = content.replace(r.rx, r.rep);
                modified = true;
            }
        });
        
        if (modified) {
            fs.writeFileSync(file, content, 'utf8');
            console.log('Modified', file);
        }
    });
});
