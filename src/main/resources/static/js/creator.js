/**
 * EDUSLOP - Creator Controller
 * Manages topic prompt inputs, preset pills, API key persistence, and generation progress.
 */

document.addEventListener('DOMContentLoaded', () => {
  const promptInput = document.getElementById('topicPrompt');
  const apiKeyInput = document.getElementById('geminiApiKey');
  const generateBtn = document.getElementById('generateBtn');
  const terminalEl = document.getElementById('loadingTerminal');
  const terminalLogs = document.getElementById('terminalLogs');
  const existingLessonsList = document.getElementById('existingLessonsList');

  // Load saved API key from localStorage if present
  const savedKey = localStorage.getItem('eduslop_gemini_key');
  if (savedKey && apiKeyInput) {
    apiKeyInput.value = savedKey;
  }

  // Handle Preset Pills
  document.querySelectorAll('.prompt-pill').forEach((pill) => {
    pill.addEventListener('click', () => {
      promptInput.value = pill.getAttribute('data-prompt');
      promptInput.focus();
    });
  });

  // Handle Generate Submission
  generateBtn.addEventListener('click', async () => {
    const prompt = promptInput.value.trim();
    if (!prompt) {
      alert('Please enter a lesson topic or prompt!');
      promptInput.focus();
      return;
    }

    const apiKey = apiKeyInput.value.trim();
    if (apiKey) {
      localStorage.setItem('eduslop_gemini_key', apiKey);
    }

    // UI State: Loading
    generateBtn.disabled = true;
    promptInput.disabled = true;
    terminalEl.classList.add('active');
    terminalLogs.innerHTML = '';

    addTerminalLog(`> Booting EDUSLOP generative pipeline for: "${prompt}"...`);

    // Simulated terminal steps
    const step1Timer = setTimeout(() => {
      addTerminalLog('> [1/3] Directing 2D pixel art scenes and sprite choreographies...');
    }, 1200);

    const step2Timer = setTimeout(() => {
      addTerminalLog('> [2/3] Distilling key takeaways into interactive cue cards...');
    }, 2800);

    const step3Timer = setTimeout(() => {
      addTerminalLog('> [3/3] Engineering multi-choice assessment and invariants...');
    }, 4500);

    try {
      const res = await fetch('/api/lessons/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt, apiKey: apiKey || null })
      });

      clearTimeout(step1Timer);
      clearTimeout(step2Timer);
      clearTimeout(step3Timer);

      if (!res.ok) {
        throw new Error(`Server returned error ${res.status}`);
      }

      const lesson = await res.json();
      addTerminalLog(`> SUCCESS! Lesson generated: "${lesson.topic}" (ID: ${lesson.id})`);
      addTerminalLog('> Launching interactive retro player...');

      setTimeout(() => {
        window.location.href = `/lesson/${lesson.id}`;
      }, 800);

    } catch (err) {
      console.error('Generation error:', err);
      addTerminalLog(`> [ERROR] Generation encountered an issue: ${err.message}`);
      addTerminalLog('> Resetting form...');
      generateBtn.disabled = false;
      promptInput.disabled = false;
    }
  });

  function addTerminalLog(msg) {
    const line = document.createElement('div');
    line.className = 'terminal-line';
    line.textContent = msg;
    terminalLogs.appendChild(line);
    terminalEl.scrollTop = terminalEl.scrollHeight;
  }

  // Load existing lessons list
  loadExistingLessons();

  async function loadExistingLessons() {
    if (!existingLessonsList) return;
    try {
      const res = await fetch('/api/lessons');
      if (!res.ok) return;
      const lessons = await res.json();

      if (lessons.length === 0) {
        existingLessonsList.innerHTML = '<div style="color: var(--text-muted); font-size: 16px;">No lessons generated yet. Create your first one above!</div>';
        return;
      }

      existingLessonsList.innerHTML = '';
      lessons.forEach((l) => {
        const item = document.createElement('a');
        item.href = `/lesson/${l.id}`;
        item.className = 'pixel-btn secondary';
        item.style.marginRight = '8px';
        item.style.marginBottom = '8px';
        item.textContent = `▶ ${l.topic}`;
        existingLessonsList.appendChild(item);
      });
    } catch (e) {
      console.error('Error fetching lessons:', e);
    }
  }
});
