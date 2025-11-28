const side = 60;
const sqrt3 = Math.sqrt(3);

// Safety: ensure window.rooms is always a proper object even if URL/session altered the page rendering
function ensureRoomsObject() {
    if (typeof window.rooms === 'undefined' || window.rooms === null) {
        window.rooms = {};
        return;
    }
    // If server rendered rooms as a JSON string for any reason, try to parse it
    if (typeof window.rooms === 'string') {
        try {
            window.rooms = JSON.parse(window.rooms);
        } catch (e) {
            // fallback to empty object
            console.warn('Failed to parse window.rooms string, using empty object', e);
            window.rooms = {};
        }
    }
    // If it's not an an object at this point, normalize to empty object
    if (typeof window.rooms !== 'object' || Array.isArray(window.rooms)) {
        window.rooms = {};
    }
}

let currentZoom = 0.7;
const minZoom = 0.5;
const maxZoom = 3.0;
const zoomStep = 0.1;

let currentPanX = 0;
let currentPanY = 0;
let isDragging = false;
let startX, startY;

function updateGrid() {
    try {
        ensureRoomsObject();
        const rooms = window.rooms || {};

        // Grid center based on window size
        const centerX = window.innerWidth / 2;
        const centerY = window.innerHeight / 2 - 100; // Header offset

        Object.keys(rooms).forEach(id => {
            const room = rooms[id];
            if (!room) return;
            const q = room.q;
            const r = room.r;

            // Standard flat-top axial to pixel coordinates
            const pixelX = side * (sqrt3 * q + (sqrt3 / 2) * r);
            const pixelY = side * (1.5 * r);
            const hexClass = `.hex-${id}`;
            const elem = document.querySelector(hexClass);
            if (elem) {
                // Apply calculated position
                elem.style.left = `${centerX + pixelX}px`;
                elem.style.top = `${centerY + pixelY}px`;

                // Z-index for overlap management (closer to 0, closer to front)
                const dist = Math.abs(q) + Math.abs(r);
                elem.style.zIndex = 20 - dist;
            }
        });
    } catch (err) {
        console.error('updateGrid failed:', err);
    }
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

// 이미지 경로를 결정하는 함수 (⚠️ 실제 이미지 파일이 필요합니다.)
function getRoomImageUrl(size) {
    // 이미지 파일은 {프로젝트_경로}/images/ 폴더에 있다고 가정합니다.
    // currentPath는 window.location.pathname 기준으로 찾습니다.
    const currentPath = window.location.pathname.substring(0, window.location.pathname.lastIndexOf('/') + 1);
    const basePath = currentPath + 'images/';
    const normalizedSize = size.toLowerCase();

    // 방 크기(small, medium, large)에 따라 이미지를 분기합니다.
    if (normalizedSize === 'small') return basePath + 'view_small.jpg';
    if (normalizedSize === 'medium') return basePath + 'view_medium.jpg';
    if (normalizedSize === 'large') return basePath + 'view_large.jpg';

    // 기본 이미지 경로
    return basePath + 'view_small.jpg';
}


// --- 페이지 로드 후 실행되는 핵심 로직 ---
window.addEventListener('load', () => {
    ensureRoomsObject();

    // DOM ELEMENTS
    const gridContainer = document.querySelector('.hex-grid-container');
    const zoomInBtn = document.getElementById('zoom-in');
    const zoomOutBtn = document.getElementById('zoom-out');
    const paymentBtn = document.getElementById('payment-button');
    const hexElements = document.querySelectorAll('.hex');

    // MODAL ELEMENTS
    const modal = document.getElementById('room-modal');
    const closeBtn = document.querySelector('.modal-close-btn');

    if (!gridContainer) {
        console.warn('hex-grid-container not found');
        return;
    }

    // 1. GRID DRAG & PAN LOGIC
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

    // 2. ZOOM LOGIC
    if (zoomInBtn) zoomInBtn.addEventListener('click', () => updateZoom(currentZoom + zoomStep));
    if (zoomOutBtn) zoomOutBtn.addEventListener('click', () => updateZoom(currentZoom - zoomStep));

    gridContainer.addEventListener('wheel', (e) => {
        e.preventDefault();
        const delta = e.deltaY > 0 ? -zoomStep : zoomStep;
        updateZoom(currentZoom + delta);
    });

    // 3. PAYMENT BUTTON LOGIC
    if (paymentBtn) {
        paymentBtn.addEventListener('click', (e) => {
            e.preventDefault();
            const popupWidth = 480;
            const popupHeight = 700;
            const left = window.screenX + Math.max(0, (window.innerWidth - popupWidth) / 2);
            const top = window.screenY + Math.max(0, (window.innerHeight - popupHeight) / 2);
            const features = `width=${popupWidth},height=${popupHeight},left=${left},top=${top},resizable=yes,scrollbars=yes`;
            const popup = window.open('/checkout', 'checkoutPopup', features);
            if (popup) popup.focus();
            else alert('팝업이 차단되었습니다. 팝업 허용 후 다시 시도하세요.');
        });
    }


    // 4. ROOM DETAIL MODAL LOGIC (통합된 부분)

    // 모달 닫기 이벤트
    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            modal.style.display = 'none';
        });
    }

    // 배경 클릭 시 모달 닫기
    window.addEventListener('click', (event) => {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });

    // 각 방 클릭 이벤트 리스너 추가
    hexElements.forEach(hex => {
        hex.addEventListener('click', (e) => {
            // **주의:** 육각형 내부의 구매 버튼(form)을 클릭하면 모달을 띄우지 않고 폼 제출을 진행합니다.
            if (e.target.closest('form')) {
                return;
            }

            // 방 ID 추출 (예: class="hex hex-100 hex-q-..." 에서 '100' 추출)
            const classList = Array.from(hex.classList);
            const roomIdClass = classList.find(c => c.startsWith('hex-'));
            if (!roomIdClass) return;

            const roomId = roomIdClass.substring(4);

            // HTML에서 받아온 전역 rooms 객체 및 userCoupons 변수 사용
            const roomData = window.rooms[roomId];
            const userCoupons = window.userCoupons || 0; // HTML 스크립트에서 초기화된 값

            if (roomData) {
                // 1. 모달 내용 채우기
                document.getElementById('modal-room-id').textContent = `[ID ${roomId}] ${roomData.size} 룸 상세 정보`;
                document.getElementById('modal-size').textContent = roomData.size;
                document.getElementById('modal-price').textContent = roomData.price.toLocaleString();
                document.getElementById('modal-desc').textContent = roomData.desc;

                // 2. 이미지 설정
                document.getElementById('modal-image').src = getRoomImageUrl(roomData.size);

                // 3. 소유자 정보/구매 버튼 동적 생성
                const ownerInfoDiv = document.getElementById('modal-owner-info');
                ownerInfoDiv.innerHTML = '';

                if (roomData.ownerId) {
                    // 소유자가 있을 경우
                    ownerInfoDiv.innerHTML = `
                        <p style="color: #ff5555; font-weight: bold;">
                            ✅ 소유자: ${roomData.ownerNickname}
                        </p>
                        <button class="button disabled-btn" disabled>판매 완료된 방</button>
                    `;
                } else {
                    // 소유자가 없을 경우 (구매 가능)
                    const canAfford = userCoupons >= roomData.price;
                    // HTML 경로 설정 (루트 경로를 기준으로 다시 설정)
                    const currentPath = window.location.pathname.substring(0, window.location.pathname.lastIndexOf('/') + 1);

                    ownerInfoDiv.innerHTML = `
                        <p style="color: #ffcc00; font-weight: bold;">
                            구매 가능! 지금 바로 소유하세요.
                        </p>
                        <form action="${currentPath}purchase-room/${roomId}" method="post" style="margin-top: 10px;">
                            <button type="submit" class="button buy-btn"
                                    ${canAfford ? '' : 'disabled'}
                                    style="width: 100%; background-color: ${canAfford ? '#007bff' : '#6c757d'};">
                                ${canAfford ? '🚀 구매하기' : `잔액 부족 (필요: ${roomData.price.toLocaleString()} 🪙)`}
                            </button>
                        </form>
                    `;
                }

                // 4. 모달 표시
                modal.style.display = 'block';
            }
        });
    });

    // 5. INITIALIZATION
    updateZoom(0.7);
    updateGrid();
    gridContainer.style.cursor = 'grab';
});

// 6. RESIZE HANDLER
window.addEventListener('resize', updateGrid);