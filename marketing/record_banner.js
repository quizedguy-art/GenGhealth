const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

async function main() {
    console.log("Starting animation recording...");
    const bannerPath = path.resolve(__dirname, 'animated-banner', 'index.html');
    const outputDir = path.resolve(__dirname);

    // Launch Chromium browser
    const browser = await chromium.launch();
    
    // Create browser context with video recording enabled
    const context = await browser.newContext({
        viewport: { width: 1920, height: 1080 },
        recordVideo: {
            dir: outputDir,
            size: { width: 1920, height: 1080 }
        }
    });

    const page = await context.newPage();
    
    // Open the local HTML file
    console.log(`Opening banner: file://${bannerPath}`);
    await page.goto(`file://${bannerPath}`);
    
    // Wait for the page to load
    await page.waitForLoadState('networkidle');
    console.log("Banner loaded. Recording for 15.5 seconds...");

    // Record for exactly 15.5 seconds (one full cycle of the 15s animation)
    await page.waitForTimeout(15500);

    // Close context to finish writing the video file
    await context.close();
    await browser.close();

    console.log("Recording finished.");

    // Find the recorded video and rename it
    const files = fs.readdirSync(outputDir);
    const videoFile = files.find(file => file.endsWith('.webm') && file !== 'animated-banner-ad.webm');
    
    if (videoFile) {
        const oldPath = path.join(outputDir, videoFile);
        const newPath = path.join(outputDir, 'animated-banner-ad.webm');
        if (fs.existsSync(newPath)) {
            fs.unlinkSync(newPath);
        }
        fs.renameSync(oldPath, newPath);
        console.log(`Successfully saved video ad to: ${newPath}`);
    } else {
        console.error("Error: Could not find the recorded WebM file.");
    }
}

main().catch(err => {
    console.error("Error running recorder:", err);
});
