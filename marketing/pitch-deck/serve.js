const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;

const MIME_TYPES = {
    '.html': 'text/html',
    '.css': 'text/css',
    '.js': 'text/javascript',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.json': 'application/json',
};

const server = http.createServer((req, res) => {
    // Resolve requested url relative to current folder or parent folders
    let requestedPath = req.url.split('?')[0];
    let filePath;
    
    if (requestedPath === '/') {
        filePath = path.join(__dirname, 'index.html');
    } else if (requestedPath.includes('docs/assets/')) {
        // Resolve assets outside the pitch-deck folder
        const assetsIndex = requestedPath.indexOf('docs/assets/');
        filePath = path.join(__dirname, '../..', requestedPath.substring(assetsIndex));
    } else {
        filePath = path.join(__dirname, requestedPath);
    }

    const extname = String(path.extname(filePath)).toLowerCase();
    const contentType = MIME_TYPES[extname] || 'application/octet-stream';

    fs.readFile(filePath, (error, content) => {
        if (error) {
            if (error.code === 'ENOENT') {
                res.writeHead(404, { 'Content-Type': 'text/html' });
                res.end('<h1>404 File Not Found</h1>', 'utf-8');
            } else {
                res.writeHead(500);
                res.end(`Server Error: ${error.code} ..\n`);
            }
        } else {
            res.writeHead(200, { 'Content-Type': contentType });
            res.end(content, 'utf-8');
        }
    });
});

server.listen(PORT, () => {
    console.log(`Server running at http://localhost:${PORT}/`);
    console.log('Press Ctrl+C to stop');
});
