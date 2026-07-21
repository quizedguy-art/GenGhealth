// Main JS for GenGhealth Landing Page

// 1. Navbar Glassmorphism on Scroll
const nav = document.querySelector('nav');
window.addEventListener('scroll', () => {
    if (window.scrollY > 50) {
        nav.style.background = 'rgba(15, 23, 42, 0.95)';
        nav.style.boxShadow = '0 10px 50px rgba(0, 0, 0, 0.4)';
    } else {
        nav.style.background = 'rgba(15, 23, 42, 0.8)';
        nav.style.boxShadow = 'none';
    }
});

// 2. Smooth Scroll for Navigation Links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const targetId = this.getAttribute('href');
        const targetElement = document.querySelector(targetId);
        
        if (targetElement) {
            window.scrollTo({
                top: targetElement.offsetTop - 80,
                behavior: 'smooth'
            });
        }
    });
});

// 3. Reveal Elements on Scroll (Simple AOS-like Implementation)
const observerOptions = {
    threshold: 0.15
};

const revealObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.animate([
                { opacity: 0, transform: 'translateY(30px)' },
                { opacity: 1, transform: 'translateY(0)' }
            ], {
                duration: 600,
                easing: 'ease-out',
                fill: 'forwards'
            });
            revealObserver.unobserve(entry.target);
        }
    });
}, observerOptions);

document.querySelectorAll('.feature-card, .step, h2, #rewards img').forEach(el => {
    el.style.opacity = '0';
    revealObserver.observe(el);
});

// 4. Parallax Effect for Hero Background
window.addEventListener('mousemove', (e) => {
    const amount = 20;
    const x = (e.clientX / window.innerWidth - 0.5) * amount;
    const y = (e.clientY / window.innerHeight - 0.5) * amount;
    
    const heroVisual = document.querySelector('.hero-visual img');
    if (heroVisual) {
        heroVisual.style.transform = `translate(${x}px, ${y}px)`;
    }
});
