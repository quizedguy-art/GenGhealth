document.addEventListener('DOMContentLoaded', () => {
    const tl = gsap.timeline({ repeat: -1 });

    // Scene 1: Intro (0-3s)
    tl.to('#scene-intro', { autoAlpha: 1, duration: 0.5 })
      .from('.logo-wrapper', { y: 50, opacity: 0, duration: 1, ease: 'back.out(1.7)' }, '-=0.3')
      .from('.main-title', { scale: 0.8, opacity: 0, duration: 1, ease: 'power4.out' }, '-=0.5')
      .to('#scene-intro', { autoAlpha: 0, duration: 0.5, delay: 1.5 });

    // Scene 2: Features (3-7s)
    tl.to('#scene-features', { autoAlpha: 1, duration: 0.5 })
      .from('.feature-card', { 
        y: 100, 
        opacity: 0, 
        stagger: 0.2, 
        duration: 0.8, 
        ease: 'power3.out' 
      })
      .to('.feature-card', { 
        scale: 1.05, 
        duration: 0.4, 
        stagger: {
            each: 0.2,
            repeat: 1,
            yoyo: true
        }
      })
      .to('#scene-features', { autoAlpha: 0, duration: 0.5, delay: 1 });

    // Scene 3: Rewards (7-11s)
    tl.to('#scene-rewards', { autoAlpha: 1, duration: 0.5 })
      .from('.app-mockup', { x: -200, opacity: 0, duration: 1, ease: 'power2.out' })
      .from('.cash-item', { 
        scale: 0, 
        opacity: 0, 
        stagger: 0.3, 
        duration: 0.8, 
        ease: 'elastic.out(1, 0.5)' 
      }, '-=0.5')
      .to('.cash-item', { 
        y: -20, 
        duration: 2, 
        repeat: -1, 
        yoyo: true, 
        stagger: 0.4 
      })
      .to('#scene-rewards', { autoAlpha: 0, duration: 0.5, delay: 1.5 });

    // Scene 4: Final CTA (11-15s)
    tl.to('.cta-overlay', { opacity: 1, duration: 1 })
      .from('.cta-slogan', { y: 20, opacity: 0, duration: 0.8 })
      .from('.blink-btn', { scale: 0, duration: 0.8, ease: 'back.out(1.7)' }, '-=0.4')
      .to('.cta-overlay', { opacity: 0, duration: 0.5, delay: 2.5 });

    // Background blobs slow float
    gsap.to('#blob-1', { 
        x: 100, 
        y: 100, 
        duration: 8, 
        repeat: -1, 
        yoyo: true, 
        ease: 'sine.inOut' 
    });
    gsap.to('#blob-2', { 
        x: -100, 
        y: -100, 
        duration: 10, 
        repeat: -1, 
        yoyo: true, 
        ease: 'sine.inOut' 
    });
});
