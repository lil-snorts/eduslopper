/**
 * EDUSLOP - Creator Controller
 *
 * Step 1: capture the teacher's prompt.
 * Step 2: iterate a shared design brief with the tutor, including clarifying
 *         questions when the prompt lacks audience, objectives, or scope.
 * Step 3: generate the lesson from the aligned brief.
 */

document.addEventListener('DOMContentLoaded', () => {
  const promptInput = document.getElementById('topicPrompt');
  const apiKeyInput = document.getElementById('geminiApiKey');
  const aiStatus = document.getElementById('aiStatus');
  const tutorBtn = document.getElementById('tutorBtn');
  const terminalEl = document.getElementById('loadingTerminal');
  const terminalLogs = document.getElementById('terminalLogs');
  const existingLessonsList = document.getElementById('existingLessonsList');

  const tutorPanel = document.getElementById('tutorPanel');
  const iterationBadge = document.getElementById('iterationBadge');
  const tutorMessage = document.getElementById('tutorMessage');
  const briefTopic = document.getElementById('briefTopic');
  const briefAudience = document.getElementById('briefAudience');
  const briefTone = document.getElementById('briefTone');
  const briefStatus = document.getElementById('briefStatus');
  const briefObjectivesWrap = document.getElementById('briefObjectivesWrap');
  const briefObjectives = document.getElementById('briefObjectives');
  const briefConceptsWrap = document.getElementById('briefConceptsWrap');
  const briefConcepts = document.getElementById('briefConcepts');
  const briefOutlineWrap = document.getElementById('briefOutlineWrap');
  const briefOutline = document.getElementById('briefOutline');
  const briefAssumptionsWrap = document.getElementById('briefAssumptionsWrap');
  const briefAssumptions = document.getElementById('briefAssumptions');
  const questionList = document.getElementById('questionList');
  const questionCount = document.getElementById('questionCount');
  const staleWarning = document.getElementById('staleWarning');
  const additionalContext = document.getElementById('additionalContext');
  const iterateBtn = document.getElementById('iterateBtn');
  const generateBtn = document.getElementById('generateBtn');

  let currentBrief = null;
  let currentIteration = 0;
  let answerDraft = {};
  let designPrompt = '';
  let isBusy = false;
  let serverConfig = null;

  // Load saved API key from localStorage if present
  const savedKey = localStorage.getItem('eduslop_gemini_key');
  if (savedKey && apiKeyInput) {
    apiKeyInput.value = savedKey;
  }

  loadAiStatus();
  apiKeyInput.addEventListener('input', updateAiStatusForTypedKey);

  // Handle Preset Pills
  document.querySelectorAll('.prompt-pill').forEach((pill) => {
    pill.addEventListener('click', () => {
      promptInput.value = pill.getAttribute('data-prompt');
      promptInput.dispatchEvent(new Event('input'));
      promptInput.focus();
    });
  });

  // Editing the prompt invalidates the agreed design until the tutor is re-run.
  promptInput.addEventListener('input', updateStaleState);

  tutorBtn.addEventListener('click', () => consultTutor(true));
  iterateBtn.addEventListener('click', () => consultTutor(false));
  generateBtn.addEventListener('click', generateLesson);

  async function loadAiStatus() {
    if (!aiStatus) return;
    try {
      const res = await fetch('/api/config');
      if (!res.ok) throw new Error(`Config returned ${res.status}`);
      serverConfig = await res.json();
      updateAiStatusForTypedKey();
    } catch (err) {
      aiStatus.textContent = `Could not read server generation config: ${err.message}`;
      aiStatus.className = 'system-status error';
    }
  }

  function updateAiStatusForTypedKey() {
    if (!aiStatus || !serverConfig) return;
    const typedKey = apiKeyInput.value.trim();
    if (serverConfig.mode === 'mock') {
      aiStatus.textContent = 'DEMO MODE: USE_MOCK=true is set on the server. Mock lessons are returned without calling Gemini.';
      aiStatus.className = 'system-status mock';
    } else if (typedKey) {
      aiStatus.textContent = `LIVE MODE: requests will use the API key from this form with ${serverConfig.model}.`;
      aiStatus.className = 'system-status live';
    } else if (serverConfig.geminiKeyConfigured) {
      aiStatus.textContent = `LIVE MODE: using the server's GEMINI_API_KEY with ${serverConfig.model}.`;
      aiStatus.className = 'system-status live';
    } else {
      aiStatus.textContent = `LIVE MODE: no Gemini API key detected. Enter one above, set GEMINI_API_KEY in .env, or run with USE_MOCK=true.`;
      aiStatus.className = 'system-status error';
    }
  }

  async function readErrorMessage(res) {
    const fallback = `Server returned error ${res.status}`;
    try {
      const body = await res.text();
      if (!body) return fallback;
      try {
        const parsed = JSON.parse(body);
        return parsed.error || parsed.message || fallback;
      } catch {
        return body;
      }
    } catch {
      return fallback;
    }
  }

  async function consultTutor(isFirstPass) {
    if (isBusy) return;

    const prompt = promptInput.value.trim();
    if (!prompt) {
      alert('Please enter a lesson topic or prompt first!');
      promptInput.focus();
      return;
    }

    const promptChanged = prompt !== designPrompt;
    const restarting = isFirstPass && Boolean(currentBrief) && !promptChanged;
    if (restarting && Object.keys(collectAnswers()).length > 0) {
      const restart = confirm('Restart the design review? Your current answers will be cleared.');
      if (!restart) return;
    }

    if (promptChanged || restarting) {
      // A new prompt (or an explicit restart) begins a new design conversation.
      currentBrief = null;
      currentIteration = 0;
      answerDraft = {};
      additionalContext.value = '';
    }

    // The Step 1 button always starts a fresh review. The refine button keeps
    // the current brief and sends the teacher's answers back to the tutor.
    const firstPass = isFirstPass || promptChanged || !currentBrief;
    const apiKey = apiKeyInput.value.trim();
    if (apiKey) {
      localStorage.setItem('eduslop_gemini_key', apiKey);
    }

    const answers = firstPass ? {} : collectAnswers();
    const previousBrief = firstPass ? null : currentBrief;
    const iteration = firstPass ? 0 : currentIteration;

    setBusy(true);
    showTerminal();

    if (firstPass) {
      addTerminalLog(`> [TUTOR 1/2] Reading your lesson intent: "${prompt}"...`);
      addTerminalLog('> Checking for audience, learning objective, prior knowledge, and must-cover content...');
    } else {
      addTerminalLog(`> [TUTOR 1/2] Folding ${Object.keys(answers).length} answer(s) into the shared brief...`);
    }

    const timer = setTimeout(() => {
      addTerminalLog('> [TUTOR 2/2] Drafting the aligned lesson design...');
    }, 900);

    try {
      const res = await fetch('/api/tutor/design', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          prompt,
          apiKey: apiKey || null,
          answers,
          additionalContext: additionalContext.value.trim() || null,
          previousBrief,
          iteration
        })
      });

      clearTimeout(timer);

      if (!res.ok) {
        throw new Error(await readErrorMessage(res));
      }

      const data = await res.json();
      currentBrief = data.brief;
      currentIteration = data.iteration;
      designPrompt = prompt;

      // Fold tutor-normalised answers back into the editable draft.
      answerDraft = { ...(data.brief.answers || {}) };

      const open = (data.brief.clarifyingQuestions || []).length;
      addTerminalLog(
        data.readyToGenerate
          ? `> [TUTOR] Pass ${data.iteration} complete. Design aligned - ready to generate.`
          : `> [TUTOR] Pass ${data.iteration} complete. ${open} question(s) still open.`
      );

      renderBrief(data);
      tutorPanel.hidden = false;
      updateStaleState();
      tutorPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (err) {
      clearTimeout(timer);
      console.error('Tutor error:', err);
      addTerminalLog(`> [ERROR] Tutor review failed: ${err.message}`);
      addTerminalLog('> You can try again, or adjust your prompt and re-run the tutor.');
    } finally {
      setBusy(false);
      updateStaleState();
    }
  }

  function renderBrief(data) {
    const brief = data.brief;
    iterationBadge.textContent = `PASS ${data.iteration}`;
    tutorMessage.textContent = data.tutorMessage || '';

    briefTopic.textContent = brief.topic || 'Untitled Lesson';
    briefAudience.textContent = brief.audience || 'Not specified';
    briefTone.textContent = brief.tone || 'Default retro pixel';

    renderList(briefObjectives, briefObjectivesWrap, brief.learningObjectives);
    renderList(briefConcepts, briefConceptsWrap, brief.keyConcepts);
    renderOutline(brief.proposedOutline || []);
    renderList(briefAssumptions, briefAssumptionsWrap, brief.assumptions);

    renderQuestions(brief.clarifyingQuestions || []);
    updateAlignmentDisplay();
  }

  function renderList(listEl, wrapEl, items) {
    listEl.innerHTML = '';
    const values = (items || []).filter((item) => item && item.trim());
    if (values.length === 0) {
      wrapEl.hidden = true;
      return;
    }
    wrapEl.hidden = false;
    values.forEach((item) => {
      const li = document.createElement('li');
      li.textContent = item;
      listEl.appendChild(li);
    });
  }

  function renderOutline(beats) {
    briefOutline.innerHTML = '';
    const values = (beats || []).filter((beat) => beat && beat.title);
    if (values.length === 0) {
      briefOutlineWrap.hidden = true;
      return;
    }
    briefOutlineWrap.hidden = false;
    values.forEach((beat) => {
      const li = document.createElement('li');
      const title = document.createElement('strong');
      title.textContent = beat.title;
      li.appendChild(title);
      if (beat.purpose) {
        const purpose = document.createElement('span');
        purpose.className = 'outline-purpose';
        purpose.textContent = ` - ${beat.purpose}`;
        li.appendChild(purpose);
      }
      briefOutline.appendChild(li);
    });
  }

  function renderQuestions(questions) {
    questionList.innerHTML = '';

    if (questions.length === 0) {
      const empty = document.createElement('div');
      empty.className = 'no-questions';
      empty.textContent = '✓ No open questions - this design is aligned. Review the brief and generate the lesson.';
      questionList.appendChild(empty);
      updateAlignmentDisplay();
      return;
    }

    questions.forEach((q) => {
      const card = document.createElement('div');
      card.className = 'question-card';
      card.dataset.qid = q.id;

      const title = document.createElement('div');
      title.className = 'question-title';
      title.textContent = q.question;
      card.appendChild(title);

      if (q.whyItMatters) {
        const why = document.createElement('div');
        why.className = 'question-why';
        why.textContent = q.whyItMatters;
        card.appendChild(why);
      }

      const suggestions = document.createElement('div');
      suggestions.className = 'suggestion-container';
      const selected = (answerDraft[q.id] || '').trim();

      (q.suggestions || []).forEach((suggestion) => {
        const pill = document.createElement('button');
        pill.type = 'button';
        pill.className = 'suggestion-pill' + (selected === suggestion ? ' selected' : '');
        pill.textContent = suggestion;
        pill.addEventListener('click', () => {
          const willClear = (answerDraft[q.id] || '').trim() === suggestion;
          answerDraft[q.id] = willClear ? '' : suggestion;
          answerInput.value = willClear ? '' : suggestion;
          card.querySelectorAll('.suggestion-pill').forEach((btn) => {
            btn.classList.toggle('selected', btn === pill && !willClear);
          });
          updateAlignmentDisplay();
        });
        suggestions.appendChild(pill);
      });
      card.appendChild(suggestions);

      const answerInput = document.createElement('input');
      answerInput.type = 'text';
      answerInput.className = 'pixel-input question-answer';
      answerInput.placeholder = 'Type a custom answer...';
      answerInput.value = selected;
      answerInput.addEventListener('input', () => {
        const value = answerInput.value.trim();
        answerDraft[q.id] = value;
        card.querySelectorAll('.suggestion-pill').forEach((btn) => {
          btn.classList.toggle('selected', btn.textContent === value && value !== '');
        });
        updateAlignmentDisplay();
      });
      card.appendChild(answerInput);

      const skip = document.createElement('button');
      skip.type = 'button';
      skip.className = 'skip-link';
      skip.textContent = 'Let the tutor assume this';
      skip.addEventListener('click', () => {
        delete answerDraft[q.id];
        answerInput.value = '';
        card.querySelectorAll('.suggestion-pill').forEach((btn) => btn.classList.remove('selected'));
        updateAlignmentDisplay();
      });
      card.appendChild(skip);

      questionList.appendChild(card);
    });

    updateAlignmentDisplay();
  }

  function collectAnswers() {
    const answers = {};
    Object.entries(answerDraft).forEach(([id, value]) => {
      if (value && value.trim()) {
        answers[id] = value.trim();
      }
    });
    return answers;
  }

  function openQuestionCount() {
    if (!currentBrief) return 0;
    return (currentBrief.clarifyingQuestions || []).filter(
      (q) => !(answerDraft[q.id] || '').trim()
    ).length;
  }

  function updateAlignmentDisplay() {
    if (!currentBrief) return;

    const open = openQuestionCount();
    questionCount.textContent = `${open} OPEN`;
    questionCount.classList.toggle('aligned', open === 0);

    briefStatus.textContent = open === 0 ? 'ALIGNED ✓' : `${open} OPEN`;
    briefStatus.classList.toggle('aligned', open === 0);
    briefStatus.classList.toggle('open', open > 0);
  }

  function updateStaleState() {
    const stale = Boolean(currentBrief) && promptInput.value.trim() !== designPrompt;
    staleWarning.hidden = !stale;
    if (stale) {
      briefStatus.textContent = 'STALE';
      briefStatus.classList.remove('aligned');
      briefStatus.classList.add('open');
    } else if (currentBrief) {
      updateAlignmentDisplay();
    }

    tutorBtn.disabled = isBusy;
    iterateBtn.disabled = isBusy || stale;
    generateBtn.disabled = isBusy || stale;

    if (!currentBrief) {
      tutorBtn.textContent = '🧑‍🏫 DRAFT DESIGN WITH TUTOR ➔';
    } else if (stale) {
      tutorBtn.textContent = '🔄 REALIGN DESIGN WITH TUTOR ➔';
    } else {
      tutorBtn.textContent = '🔄 RESTART DESIGN REVIEW';
    }
  }

  function setBusy(busy) {
    isBusy = busy;
    promptInput.disabled = busy;
    apiKeyInput.disabled = busy;
    tutorBtn.disabled = busy;
    iterateBtn.disabled = busy;
    generateBtn.disabled = busy;
  }

  async function generateLesson() {
    if (isBusy || !currentBrief) return;

    const prompt = promptInput.value.trim();
    if (prompt !== designPrompt) {
      updateStaleState();
      return;
    }

    const open = openQuestionCount();
    if (open > 0) {
      const proceed = confirm(
        `${open} clarifying question${open === 1 ? ' is' : 's are'} still open.\n\n` +
        'Generate anyway? The tutor will turn the unanswered items into explicit assumptions.'
      );
      if (!proceed) {
        iterateBtn.focus();
        return;
      }
    }

    const apiKey = apiKeyInput.value.trim();
    const briefForGeneration = {
      ...currentBrief,
      answers: collectAnswers(),
      additionalContext: additionalContext.value.trim() || null
    };

    setBusy(true);
    showTerminal();
    addTerminalLog(`> Generating an aligned lesson for: "${prompt}"`);
    addTerminalLog(`> Applying design brief - audience: ${briefForGeneration.audience || 'inferred'}; pass: ${currentIteration}`);

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
        body: JSON.stringify({
          prompt,
          apiKey: apiKey || null,
          designBrief: briefForGeneration
        })
      });

      clearTimeout(step1Timer);
      clearTimeout(step2Timer);
      clearTimeout(step3Timer);

      if (!res.ok) {
        throw new Error(await readErrorMessage(res));
      }

      const lesson = await res.json();
      addTerminalLog(`> SUCCESS! Lesson generated: "${lesson.topic}" (ID: ${lesson.id})`);
      addTerminalLog('> Launching interactive retro player...');

      setTimeout(() => {
        window.location.href = `/lesson/${lesson.id}`;
      }, 800);
    } catch (err) {
      clearTimeout(step1Timer);
      clearTimeout(step2Timer);
      clearTimeout(step3Timer);

      console.error('Generation error:', err);
      addTerminalLog(`> [ERROR] Generation encountered an issue: ${err.message}`);
      addTerminalLog('> Your design brief is still here - adjust answers or retry.');
      setBusy(false);
      updateStaleState();
    }
  }

  function showTerminal() {
    terminalEl.classList.add('active');
    terminalLogs.innerHTML = '';
    terminalEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

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
