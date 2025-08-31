document.addEventListener('DOMContentLoaded', () => {
    const dropzone = document.getElementById('dropzone');
    const fileInput = document.getElementById('fileInput');
    const uploadStatus = document.getElementById('uploadStatus');
    const feedContainer = document.getElementById('feedContainer');
    const emptyState = document.getElementById('emptyState');
    const clearBtn = document.getElementById('clearBtn');

    let previousPayloads = [];

    // --- Drag and Drop Logic ---

    // Prevent default behaviors
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, preventDefaults, false);
    });

    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    // Highlight
    ['dragenter', 'dragover'].forEach(eventName => {
        dropzone.addEventListener(eventName, () => dropzone.classList.add('dragover'), false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, () => dropzone.classList.remove('dragover'), false);
    });

    // Handle dropping files
    dropzone.addEventListener('drop', (e) => {
        let files = e.dataTransfer.files;
        handleFiles(files);
    });

    // Handle clicking to upload
    dropzone.addEventListener('click', () => {
        fileInput.click();
    });

    fileInput.addEventListener('change', function () {
        handleFiles(this.files);
    });

    function handleFiles(files) {
        if (files.length === 0) return;

        const file = files[0];

        // Basic validation
        if (!file.name.endsWith('.csv') && !file.name.endsWith('.xml')) {
            showStatus('Only CSV or XML files are supported.', 'error');
            return;
        }

        uploadFile(file);
    }

    function uploadFile(file) {
        showStatus('Uploading...', 'success');

        const formData = new FormData();
        formData.append('file', file);

        fetch('/api/v1/upload', {
            method: 'POST',
            body: formData
        })
            .then(response => {
                if (!response.ok) throw new Error('Network response was not ok');
                return response.text();
            })
            .then(data => {
                showStatus('File accepted! Polling for canonical JSON...', 'success');
                // Reset status after a few seconds
                setTimeout(() => {
                    uploadStatus.classList.add('hidden');
                }, 3000);
            })
            .catch(error => {
                showStatus('Upload failed: ' + error.message, 'error');
            });
    }

    function showStatus(message, type) {
        uploadStatus.textContent = message;
        uploadStatus.className = `upload-status ${type}`; // reset classes
        uploadStatus.classList.remove('hidden');
    }

    // --- Live Feed Logic ---

    function formatTime(date) {
        return new Date(date).toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit', fractionalSecondDigits: 3 });
    }

    function createPayloadCard(jsonString, timestamp) {
        const card = document.createElement('div');
        card.className = 'payload-card';

        // Format the JSON nicely
        let formattedJson = jsonString;
        try {
            const parsed = JSON.parse(jsonString);
            formattedJson = JSON.stringify(parsed, null, 2);
        } catch (e) { /* keep as is if not valid json for some reason */ }

        card.innerHTML = `
            <div class="payload-header">
                <span>[HTTP 200 OK] Delivery Confirmed</span>
                <span class="payload-time">${formatTime(timestamp)}</span>
            </div>
            <div class="payload-body">
                <pre>${formattedJson}</pre>
            </div>
        `;
        return card;
    }

    function pollCanonicalPayloads() {
        fetch('/api/v1/canonical')
            .then(res => res.json())
            .then(data => {
                // If the backend list changed, rebuild the UI
                // Compare by length for simplicity in this demo
                if (data.length !== previousPayloads.length) {

                    if (data.length > 0) {
                        emptyState.style.display = 'none';
                    } else {
                        emptyState.style.display = 'flex';
                    }

                    // To avoid full repaint, ideally we diff, but for this portfolio piece wiping allows clean entry animations.
                    // We will preserve the empty state div though.
                    feedContainer.innerHTML = '';
                    feedContainer.appendChild(emptyState);

                    data.forEach((payload, index) => {
                        // Invent a synthetic timestamp since the backend doesn't provide one right now,
                        // cascading backwards to simulate logical order.
                        const time = Date.now() - (index * 1000);
                        const card = createPayloadCard(payload, time);
                        feedContainer.appendChild(card);
                    });

                    previousPayloads = data;
                }
            })
            .catch(console.error);
    }

    // Poll every 1.5 seconds
    setInterval(pollCanonicalPayloads, 1500);

    // --- Clear Action ---
    clearBtn.addEventListener('click', () => {
        fetch('/api/v1/canonical', { method: 'DELETE' })
            .then(() => {
                previousPayloads = [];
                feedContainer.innerHTML = '';
                feedContainer.appendChild(emptyState);
                emptyState.style.display = 'flex';
            });
    });

});
