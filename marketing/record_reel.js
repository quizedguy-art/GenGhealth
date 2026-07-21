const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');
const { execSync } = require('child_process');
const ffmpeg = require('fluent-ffmpeg');
const ffmpegPath = require('@ffmpeg-installer/ffmpeg').path;

// Set local FFmpeg path
ffmpeg.setFfmpegPath(ffmpegPath);

async function main() {
    console.log("--------------------------------------------------");
    console.log("STEP 1: Generating voiceover audio using PowerShell...");
    console.log("--------------------------------------------------");
    const audioPath = path.resolve(__dirname, 'voiceover.wav');
    
    // Voiceover script matching the timing of the scenes (aligned with the reference video)
    const voiceoverText = "I love GenGhealth! It actually pays me to step away from the screen and go for a walk. I've already earned fifteen dollars today! Redeem points instantly for premium gift cards. Get healthy, get paid. Download the app now!";
    
    try {
        if (fs.existsSync(audioPath)) {
            fs.unlinkSync(audioPath);
        }
        
        const psScriptPath = path.resolve(__dirname, 'temp_synth.ps1');
        const psScript = [
            'Add-Type -AssemblyName System.Speech',
            '$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer',
            '$synth.SelectVoice("Microsoft Zira Desktop")',
            '$synth.Rate = 2', // Speed up the voice to a natural fast rate (default is 0, range -10 to 10)
            `$synth.SetOutputToWaveFile("${audioPath.replace(/"/g, '`"')}")`,
            `$synth.Speak("${voiceoverText.replace(/"/g, '`"')}")`,
            '$synth.Dispose()'
        ].join('\r\n');
        
        fs.writeFileSync(psScriptPath, psScript, 'utf8');

        console.log("Running speech synthesizer script file...");
        execSync("powershell -ExecutionPolicy Bypass -File temp_synth.ps1", { stdio: 'inherit', cwd: __dirname });
        
        if (fs.existsSync(psScriptPath)) {
            fs.unlinkSync(psScriptPath);
        }
        console.log(`Voiceover saved successfully to: ${audioPath}`);
    } catch (err) {
        console.error("Error generating voiceover audio:", err);
        throw err;
    }

    console.log("\n--------------------------------------------------");
    console.log("STEP 2: Rendering animated reel frame-by-frame...");
    console.log("--------------------------------------------------");
    const reelHtmlPath = path.resolve(__dirname, 'animated-reel', 'index.html');
    const outputDir = path.resolve(__dirname);
    const tempFramesDir = path.join(outputDir, 'temp_frames');
    
    if (fs.existsSync(tempFramesDir)) {
        fs.rmSync(tempFramesDir, { recursive: true, force: true });
    }
    fs.mkdirSync(tempFramesDir);

    // Launch Chromium browser
    const browser = await chromium.launch({
        headless: true
    });
    
    const context = await browser.newContext({
        viewport: { width: 1080, height: 1920 }
    });

    const page = await context.newPage();
    
    // Navigate with render=true to pause automatic animations
    const fileUrl = `file://${reelHtmlPath.replace(/\\/g, '/')}`;
    console.log(`Opening reel webpage: ${fileUrl}?render=true`);
    await page.goto(`${fileUrl}?render=true`);
    await page.waitForLoadState('networkidle');

    const fps = 30;
    const durationSeconds = 15;
    const totalFrames = fps * durationSeconds;

    console.log(`Rendering ${totalFrames} frames at ${fps} FPS...`);
    
    for (let frame = 0; frame <= totalFrames; frame++) {
        const time = frame / fps;
        
        // Seek the GSAP timeline to the exact frame time
        await page.evaluate((t) => {
            gsap.globalTimeline.seek(t);
        }, time);
        
        // Brief pause to allow paint/layout to complete
        await new Promise(resolve => setTimeout(resolve, 10));

        const frameFilename = `frame_${String(frame).padStart(4, '0')}.png`;
        const framePath = path.join(tempFramesDir, frameFilename);
        
        await page.screenshot({ path: framePath, type: 'png' });

        if (frame % 50 === 0 || frame === totalFrames) {
            console.log(`Rendered frame ${frame}/${totalFrames} (${(frame/totalFrames*100).toFixed(0)}%) - time: ${time.toFixed(2)}s`);
        }
    }

    await browser.close();
    console.log("Frame rendering complete.");

    console.log("\n--------------------------------------------------");
    console.log("STEP 3: Compiling frames and merging audio using FFmpeg...");
    console.log("--------------------------------------------------");
    const finalOutputPath = path.join(outputDir, 'animated-reel-ad.mp4');
    
    if (fs.existsSync(finalOutputPath)) {
        fs.unlinkSync(finalOutputPath);
    }

    console.log("Stitching frames together and merging voiceover...");
    
    return new Promise((resolve, reject) => {
        ffmpeg()
            .input(path.join(tempFramesDir, 'frame_%04d.png'))
            .inputOptions([
                `-framerate ${fps}`,
                '-f image2'
            ])
            .input(audioPath)
            .outputOptions([
                '-c:v libx264',
                '-pix_fmt yuv420p',
                '-c:a aac',
                '-shortest'
            ])
            .output(finalOutputPath)
            .on('end', () => {
                console.log(`\nSuccess! Final promotional reel video created: ${finalOutputPath}`);
                
                // Cleanup temp files
                console.log("Cleaning up temporary files and directories...");
                try {
                    fs.rmSync(tempFramesDir, { recursive: true, force: true });
                    fs.unlinkSync(audioPath);
                    console.log("Cleanup complete.");
                } catch (e) {
                    console.warn("Failed to delete temp files:", e.message);
                }
                resolve();
            })
            .on('error', (err) => {
                console.error("FFmpeg compile/merge error:", err);
                reject(err);
            })
            .run();
    });
}

main().catch(err => {
    console.error("\nExecution failed:", err);
    process.exit(1);
});
