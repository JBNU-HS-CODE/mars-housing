const side = 60;
const sqrt3 = Math.sqrt(3);

let currentZoom = 0.7;
const minZoom = 0.5;
const maxZoom = 3.0;
const zoomStep = 0.1;

let currentPanX = 0;
let currentPanY = 0;
let isDragging = false;
let startX, startY;

function updateGrid() {
    const centerX = window.innerWidth / 2;
    const centerY = window.innerHeight / 2 - 100; // 헤더 여백

    Object.keys(window.rooms).forEach(id => {
        const room = window.rooms[id];
        const q = room.q;
        const r = room.r;

        // Standard flat-top axial to pixel: no manual offset needed
        const pixelX = side * (sqrt3 * q + (sqrt3 / 2) * r);
        const pixelY = side * (1.5 * r);
        const hexClass = `.hex-${id}`;
        const elem = document.querySelector(hexClass);
        if (elem) {
            elem.style.left = `${centerX + pixelX}px`;
            elem.style.top = `${centerY + pixelY}px`;
            const dist = Math.abs(q) + Math.abs(r);
            elem.style.zIndex = 20 - dist;
        }
    });
}

function updateZoom(zoom) {
    currentZoom = Math.max(minZoom, Math.min(maxZoom, zoom));
    document.documentElement.style.setProperty('--zoom', currentZoom);
    document.getElementById('zoom-level').textContent = `${Math.round(currentZoom * 100)}%`;

    updateGrid();
}

function updatePan(newX, newY) {
    currentPanX = newX;
    currentPanY = newY;
    document.documentElement.style.setProperty('--pan-x', `${currentPanX}px`);
    document.documentElement.style.setProperty('--pan-y', `${currentPanY}px`);
}

const gridContainer = document.querySelector('.hex-grid-container');
gridContainer.addEventListener('mousedown', (e) => {
    isDragging = true;
    startX = e.clientX - currentPanX;
    startY = e.clientY - currentPanY;
    gridContainer.style.cursor = 'grabbing';
});

gridContainer.addEventListener('mousemove', (e) => {
    if (!isDragging) return;
    const newPanX = e.clientX - startX;
    const newPanY = e.clientY - startY;
    updatePan(newPanX, newPanY);
});

gridContainer.addEventListener('mouseup', () => {
    isDragging = false;
    gridContainer.style.cursor = 'grab';
});

gridContainer.addEventListener('mouseleave', () => {
    isDragging = false;
    gridContainer.style.cursor = 'default';
});

document.getElementById('zoom-in').addEventListener('click', () => updateZoom(currentZoom + zoomStep));
document.getElementById('zoom-out').addEventListener('click', () => updateZoom(currentZoom - zoomStep));

gridContainer.addEventListener('wheel', (e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -zoomStep : zoomStep;
    updateZoom(currentZoom + delta);
});

window.addEventListener('load', () => {
    updateZoom(0.7);
    updateGrid();
    gridContainer.style.cursor = 'grab';
});
window.addEventListener('resize', updateGrid);