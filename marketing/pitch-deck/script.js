document.addEventListener('DOMContentLoaded', () => {
    // Select DOM Elements
    const slides = document.querySelectorAll('.slide');
    const prevBtn = document.getElementById('btn-prev');
    const nextBtn = document.getElementById('btn-next');
    const fullscreenBtn = document.getElementById('btn-fullscreen');
    const currentSlideNum = document.getElementById('current-slide-num');
    const totalSlidesNum = document.getElementById('total-slides-num');
    const slideTitleIndicator = document.querySelector('.slide-title-indicator');
    const dotsContainer = document.querySelector('.slide-dots');
    
    let activeIndex = 0;
    const totalSlides = slides.length;
    
    // Set total slide number in header
    totalSlidesNum.textContent = totalSlides;

    // Create Navigation Dots
    slides.forEach((_, idx) => {
        const dot = document.createElement('div');
        dot.classList.add('dot');
        if (idx === 0) dot.classList.add('active');
        dot.addEventListener('click', () => {
            goToSlide(idx);
        });
        dotsContainer.appendChild(dot);
    });

    const dots = document.querySelectorAll('.dot');

    // Function to transition to specific slide
    function goToSlide(index) {
        if (index < 0 || index >= totalSlides) return;
        
        // Remove active class from current slide and dot
        slides[activeIndex].classList.remove('active');
        dots[activeIndex].classList.remove('active');
        
        // Update active index
        activeIndex = index;
        
        // Add active class to new slide and dot
        slides[activeIndex].classList.add('active');
        dots[activeIndex].classList.add('active');
        
        // Update Header indicators
        currentSlideNum.textContent = activeIndex + 1;
        const slideTitle = slides[activeIndex].getAttribute('data-title') || 'Slide';
        slideTitleIndicator.textContent = slideTitle;
        
        // Disable/enable controls at boundaries
        prevBtn.disabled = activeIndex === 0;
        nextBtn.disabled = activeIndex === totalSlides - 1;

        // Re-execute Lucide icons for any dynamic structures
        if (window.lucide) {
            window.lucide.createIcons();
        }
    }

    // Keyboard Listeners
    window.addEventListener('keydown', (e) => {
        if (e.key === 'ArrowRight' || e.key === ' ' || e.key === 'Enter') {
            e.preventDefault();
            if (activeIndex < totalSlides - 1) {
                goToSlide(activeIndex + 1);
            }
        } else if (e.key === 'ArrowLeft' || e.key === 'Backspace') {
            e.preventDefault();
            if (activeIndex > 0) {
                goToSlide(activeIndex - 1);
            }
        }
    });

    // Button Listeners
    prevBtn.addEventListener('click', () => {
        if (activeIndex > 0) {
            goToSlide(activeIndex - 1);
        }
    });

    nextBtn.addEventListener('click', () => {
        if (activeIndex < totalSlides - 1) {
            goToSlide(activeIndex + 1);
        }
    });

    // Fullscreen Toggle logic
    fullscreenBtn.addEventListener('click', () => {
        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen().catch(err => {
                console.error(`Error enabling fullscreen: ${err.message}`);
            });
        } else {
            document.exitFullscreen();
        }
    });

    // Fullscreen Icon update
    document.addEventListener('fullscreenchange', () => {
        const icon = fullscreenBtn.querySelector('i');
        if (document.fullscreenElement) {
            icon.setAttribute('data-lucide', 'minimize');
        } else {
            icon.setAttribute('data-lucide', 'maximize');
        }
        if (window.lucide) {
            window.lucide.createIcons();
        }
    });

    // Swipe Touch Gestures for mobile/tablet
    let startX = 0;
    let endX = 0;

    window.addEventListener('touchstart', (e) => {
        startX = e.changedTouches[0].screenX;
    }, false);

    window.addEventListener('touchend', (e) => {
        endX = e.changedTouches[0].screenX;
        handleSwipe();
    }, false);

    function handleSwipe() {
        const threshold = 50; // swipe offset threshold
        if (startX - endX > threshold) {
            // Swiped left -> next slide
            if (activeIndex < totalSlides - 1) {
                goToSlide(activeIndex + 1);
            }
        } else if (endX - startX > threshold) {
            // Swiped right -> prev slide
            if (activeIndex > 0) {
                goToSlide(activeIndex - 1);
            }
        }
    }

    // Initialize state
    goToSlide(0);
});
