document.addEventListener('DOMContentLoaded', () => {
    // 15 seconds timeline
    const tl = gsap.timeline({ repeat: -1 });
    window.tl = tl;

    // Background Blobs float animations
    gsap.to('#blob-1', { x: 80, y: 120, duration: 7, repeat: -1, yoyo: true, ease: 'sine.inOut' });
    gsap.to('#blob-2', { x: -100, y: -60, duration: 9, repeat: -1, yoyo: true, ease: 'sine.inOut' });
    gsap.to('#blob-3', { x: 50, y: -80, duration: 8, repeat: -1, yoyo: true, ease: 'sine.inOut' });

    // Check if we are rendering frame-by-frame
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('render')) {
        gsap.globalTimeline.pause();
    }

    // ==========================================
    // SCENE 1: HOOK / INTRO (0.0s - 3.0s)
    // ==========================================
    tl.to('#scene-intro', { autoAlpha: 1, duration: 0.3 })
      .from('#scene-intro .logo-wrapper', { y: -40, opacity: 0, duration: 0.6, ease: 'power3.out' }, '-=0.2')
      .from('.hook-title', { scale: 0.8, opacity: 0, duration: 0.8, ease: 'back.out(1.5)' }, '-=0.4')
      .from('.hook-subtitle', { y: 20, opacity: 0, duration: 0.6 }, '-=0.4')
      .from('.floating-cash', { 
          scale: 0, 
          opacity: 0, 
          y: -100,
          rotation: () => Math.random() * 90 - 45,
          stagger: 0.1, 
          duration: 0.8, 
          ease: 'back.out(1.7)' 
      }, '-=0.6')
      .from('.phone-mockup-wrapper .app-mockup', { y: 300, opacity: 0, duration: 0.8, ease: 'power4.out' }, '-=0.5')
      // Scene 1 Exit (starts at 2.6s)
      .to('#scene-intro', { autoAlpha: 0, y: -100, duration: 0.4, delay: 0.8 });

    // ==========================================
    // SCENE 2: USAGE ANALYTICS (3.0s - 6.0s)
    // ==========================================
    tl.to('#scene-analytics', { autoAlpha: 1, duration: 0.3 })
      .from('#scene-analytics .section-tag', { scale: 0, opacity: 0, duration: 0.4, ease: 'back.out(2)' }, '-=0.2')
      .from('#scene-analytics .scene-title', { y: 30, opacity: 0, duration: 0.5 }, '-=0.2')
      .from('#scene-analytics .scene-desc', { y: 20, opacity: 0, duration: 0.5 }, '-=0.3')
      .from('.chart-card', { y: 150, opacity: 0, duration: 0.6, ease: 'power3.out' }, '-=0.4')
      .from('.chart-bars .bar', { 
          scaleY: 0, 
          transformOrigin: 'bottom center', 
          stagger: 0.1, 
          duration: 0.8, 
          ease: 'power2.out' 
      }, '-=0.4')
      .from('.limit-status', { scale: 0, opacity: 0, duration: 0.4, ease: 'back.out(1.5)' }, '-=0.4')
      // Scene 2 Exit (starts at 5.6s)
      .to('#scene-analytics', { autoAlpha: 0, scale: 0.9, duration: 0.4, delay: 0.8 });

    // ==========================================
    // SCENE 3: POINTS ENGINE (6.0s - 9.0s)
    // ==========================================
    // Setup counter object
    const counterObj = { value: 0 };

    tl.to('#scene-points', { autoAlpha: 1, duration: 0.3 })
      .from('#scene-points .section-tag', { scale: 0, opacity: 0, duration: 0.4, ease: 'back.out(2)' }, '-=0.2')
      .from('#scene-points .scene-title', { y: 30, opacity: 0, duration: 0.5 }, '-=0.2')
      .from('#scene-points .scene-desc', { y: 20, opacity: 0, duration: 0.5 }, '-=0.3')
      .from('.progress-card', { y: 150, opacity: 0, duration: 0.6, ease: 'power3.out' }, '-=0.4')
      // Progress circle ring animation
      .to('.progress-ring-bar', { 
          strokeDashoffset: 133, // 75% complete (534 * 0.25)
          duration: 1.2, 
          ease: 'power2.out'
      }, '-=0.3')
      // Count points animation
      .to(counterObj, {
          value: 750,
          duration: 1.2,
          ease: 'power1.out',
          onUpdate: () => {
              document.getElementById('pts-counter').innerText = Math.floor(counterObj.value);
          }
      }, '-=1.2')
      .from('.pts-status', { scale: 0, opacity: 0, duration: 0.5, ease: 'elastic.out(1, 0.6)' }, '-=0.2')
      // Scene 3 Exit (starts at 8.6s)
      .to('#scene-points', { autoAlpha: 0, x: 100, duration: 0.4, delay: 0.8 });

    // ==========================================
    // SCENE 4: REWARDS (9.0s - 12.0s)
    // ==========================================
    tl.to('#scene-rewards', { autoAlpha: 1, duration: 0.3 })
      .from('#scene-rewards .section-tag', { scale: 0, opacity: 0, duration: 0.4, ease: 'back.out(2)' }, '-=0.2')
      .from('#scene-rewards .scene-title', { y: 30, opacity: 0, duration: 0.5 }, '-=0.2')
      .from('#scene-rewards .scene-desc', { y: 20, opacity: 0, duration: 0.5 }, '-=0.3')
      .from('.rewards-mockup', { scale: 0.6, rotation: -10, opacity: 0, duration: 0.8, ease: 'back.out(1.2)' }, '-=0.4')
      .from('.reward-tag-item', { 
          y: 40, 
          opacity: 0, 
          stagger: 0.15, 
          duration: 0.6, 
          ease: 'power3.out' 
      }, '-=0.5')
      // Scene 4 Exit (starts at 11.6s)
      .to('#scene-rewards', { autoAlpha: 0, y: 100, duration: 0.4, delay: 0.8 });

    // ==========================================
    // SCENE 5: OUTRO & CALL TO ACTION (12.0s - 15.0s)
    // ==========================================
    tl.to('#scene-outro', { autoAlpha: 1, duration: 0.3 })
      .from('#scene-outro .logo-wrapper', { y: -50, scale: 0.8, opacity: 0, duration: 0.6, ease: 'back.out(1.5)' }, '-=0.2')
      .from('.outro-slogan', { scale: 0.8, opacity: 0, duration: 0.6, ease: 'power2.out' }, '-=0.3')
      .from('.app-mockup-outro', { y: 200, opacity: 0, duration: 0.8, ease: 'power4.out' }, '-=0.4')
      .from('.download-button', { scale: 0, opacity: 0, duration: 0.6, ease: 'back.out(1.8)' }, '-=0.5')
      // End duration pad to total 15.0s
      .to('#scene-outro', { autoAlpha: 0, duration: 0.4, delay: 1.8 });
});
