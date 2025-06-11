// Fortune Teller Application JavaScript

// Print functionality
function printResults() {
    window.print();
}

// Form validation and enhancement
document.addEventListener('DOMContentLoaded', function() {
    console.log("Fortune Teller application loaded");
    
    // Add print button event listener if it exists
    const printBtn = document.getElementById('printBtn');
    if (printBtn) {
        printBtn.addEventListener('click', printResults);
    }
});
