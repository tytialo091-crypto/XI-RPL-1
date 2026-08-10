document.addEventListener("DOMContentLoaded", () => {
    
    const menuToggle = document.getElementById("menuToggle");
    const navMenu = document.getElementById("navMenu");
    
    if (menuToggle && navMenu) {
        menuToggle.addEventListener("click", () => {
            navMenu.classList.toggle("active");
            menuToggle.classList.toggle("active");
        });
        
        navMenu.querySelectorAll("a").forEach(link => {
            link.addEventListener("click", () => {
                navMenu.classList.remove("active");
                menuToggle.classList.remove("active");
            });
        });
    }
    
    
    const studentSearch = document.getElementById("studentSearch");
    const studentsGrid = document.getElementById("studentsGrid");
    
    if (studentSearch && studentsGrid) {
        
        studentSearch.addEventListener("input", () => {
            
            const keyword = studentSearch.value
                .toLowerCase()
                .trim();
            
            const cards = studentsGrid.querySelectorAll(".student-card");
            
            cards.forEach(card => {
                
                const name = card.querySelector("h3");
                
                if (!name) return;
                
                const studentName = name.textContent
                    .toLowerCase();
                
                if (studentName.includes(keyword)) {
                    card.style.display = "";
                } else {
                    card.style.display = "none";
                }
                
            });
            
        });
        
    }
    
    
    const sections = document.querySelectorAll("section[id]");
    const navLinks = document.querySelectorAll(".nav-menu a");
    
    function updateActiveNav() {
        
        let currentSection = "";
        
        sections.forEach(section => {
            
            const sectionTop = section.offsetTop - 150;
            const sectionHeight = section.offsetHeight;
            
            if (
                window.scrollY >= sectionTop &&
                window.scrollY < sectionTop + sectionHeight
            ) {
                currentSection = section.getAttribute("id");
            }
            
        });
        
        navLinks.forEach(link => {
            
            link.classList.remove("active");
            
            if (
                link.getAttribute("href") === "#" + currentSection
            ) {
                link.classList.add("active");
            }
            
        });
        
    }
    
    window.addEventListener("scroll", updateActiveNav);
    
    updateActiveNav();
    
    
    const studentCards = document.querySelectorAll(
        "#studentsGrid .student-card"
    );
    
    studentCards.forEach((card, index) => {
        
        const info = card.querySelector(".student-info");
        
        if (!info) return;
        
        const role = info.querySelector("span");
        
        if (!role) return;
        
        if (index === 33) {
            
            role.textContent = "34 · Teacher";
            role.classList.add("teacher-role");
            
            const className = info.querySelector("p");
            
            if (className) {
                className.textContent = "Wali Kelas XI RPL 1";
            }
            
        }
        
    });
    
    
    const images = document.querySelectorAll("img");
    
    images.forEach(image => {
        
        image.addEventListener("error", () => {
            
            image.style.objectFit = "cover";
            
        });
        
    });
    
});