/**
 * EDUSLOP - Lesson Interactive Controller
 * Coordinates the 2D Pixel Animation, Cue Cards Stack, and Stepped Quiz.
 */

document.addEventListener('DOMContentLoaded', async () => {
  // Extract lesson ID from URL path: /lesson/{id}
  const pathParts = window.location.pathname.split('/');
  const lessonId = pathParts[pathParts.length - 1] || 'ddd-intro';

  const titleEl = document.getElementById('lessonTopic');
  const summaryEl = document.getElementById('lessonSummary');

  try {
    const res = await fetch(`/api/lessons/${lessonId}`);
    if (!res.ok) {
      alert(`Lesson '${lessonId}' not found. Redirecting to creator...`);
      window.location.href = '/';
      return;
    }

    const lesson = await res.json();
    document.title = `${lesson.topic} - EDUSLOP`;
    if (titleEl) titleEl.textContent = lesson.topic;
    if (summaryEl) summaryEl.textContent = lesson.summary;

    // 1. Initialize Animation Engine
    setupAnimationPlayer(lesson.animation);

    // 2. Initialize Cue Cards Stack
    setupCueCards(lesson.cueCards || []);

    // 3. Initialize Stepped Quiz
    setupSteppedQuiz(lesson.quiz || [], lesson.id);

  } catch (err) {
    console.error('Error loading lesson:', err);
    alert('Failed to load lesson data.');
  }
});

// ==========================================
// 1. ANIMATION PLAYER SETUP
// ==========================================
function setupAnimationPlayer(animationData) {
  const canvas = document.getElementById('pixelCanvas');
  const dialogueEl = document.getElementById('dialogueText');
  const sceneTitleEl = document.getElementById('sceneTitle');
  const timeDisplayEl = document.getElementById('timeDisplay');
  const progressBarEl = document.getElementById('timelineProgress');
  const timelineBar = document.getElementById('timelineBar');

  const playBtn = document.getElementById('playPauseBtn');
  const restartBtn = document.getElementById('restartBtn');
  const speedBtn = document.getElementById('speedBtn');

  if (!canvas || !animationData) return;

  const engine = new PixelEngine(canvas, dialogueEl, sceneTitleEl, timeDisplayEl, progressBarEl);
  engine.loadAnimation(animationData);

  // Auto-play on start
  engine.play();
  updatePlayButton(engine.isPlaying);

  playBtn.addEventListener('click', () => {
    engine.togglePlay();
    updatePlayButton(engine.isPlaying);
  });

  restartBtn.addEventListener('click', () => {
    engine.restart();
    updatePlayButton(engine.isPlaying);
  });

  let currentSpeed = 1.0;
  speedBtn.addEventListener('click', () => {
    currentSpeed = currentSpeed === 1.0 ? 1.5 : (currentSpeed === 1.5 ? 2.0 : 1.0);
    engine.setSpeed(currentSpeed);
    speedBtn.textContent = `${currentSpeed}x`;
  });

  timelineBar.addEventListener('click', (e) => {
    const rect = timelineBar.getBoundingClientRect();
    const ratio = (e.clientX - rect.left) / rect.width;
    engine.seek(ratio);
  });

  function updatePlayButton(playing) {
    playBtn.textContent = playing ? '❚❚ PAUSE' : '▶ PLAY';
  }
}

// ==========================================
// 2. CUE CARDS STACK CONTROLLER
// ==========================================
function setupCueCards(cards) {
  if (!cards || cards.length === 0) return;

  let currentCardIndex = 0;
  let isFlipped = false;

  const cardElement = document.getElementById('cueCardElement');
  const categoryEl = document.getElementById('cardCategory');
  const sideIndicatorEl = document.getElementById('cardSideIndicator');
  const titleEl = document.getElementById('cardTitleText');
  const descEl = document.getElementById('cardDescText');
  const indexEl = document.getElementById('cardIndexIndicator');
  const prevBtn = document.getElementById('prevCardBtn');
  const nextBtn = document.getElementById('nextCardBtn');

  function renderCard() {
    const card = cards[currentCardIndex];
    isFlipped = false;
    cardElement.classList.remove('flipped');

    categoryEl.textContent = card.category || 'Core Concept';
    sideIndicatorEl.textContent = '[ FRONT : PROMPT ]';
    titleEl.textContent = card.title;
    descEl.textContent = card.frontPrompt;
    indexEl.textContent = `CARD ${currentCardIndex + 1} / ${cards.length}`;

    prevBtn.disabled = currentCardIndex === 0;
    nextBtn.disabled = currentCardIndex === cards.length - 1;
  }

  // Click card to flip
  cardElement.addEventListener('click', () => {
    isFlipped = !isFlipped;
    const card = cards[currentCardIndex];

    if (isFlipped) {
      cardElement.classList.add('flipped');
      sideIndicatorEl.textContent = '[ BACK : TAKEAWAY ]';
      descEl.textContent = card.backAnswer;
    } else {
      cardElement.classList.remove('flipped');
      sideIndicatorEl.textContent = '[ FRONT : PROMPT ]';
      descEl.textContent = card.frontPrompt;
    }
  });

  prevBtn.addEventListener('click', () => {
    if (currentCardIndex > 0) {
      currentCardIndex--;
      renderCard();
    }
  });

  nextBtn.addEventListener('click', () => {
    if (currentCardIndex < cards.length - 1) {
      currentCardIndex++;
      renderCard();
    }
  });

  renderCard();
}

// ==========================================
// 3. STEPPED MULTI-CHOICE QUIZ CONTROLLER
// ==========================================
function setupSteppedQuiz(questions, lessonId) {
  if (!questions || questions.length === 0) return;

  let currentIdx = 0;
  // questionId -> selectedOptionIndex
  const userAnswers = {};

  const progressLabel = document.getElementById('quizProgressLabel');
  const stepperDotsContainer = document.getElementById('quizStepperDots');
  const questionPromptEl = document.getElementById('questionPrompt');
  const optionsListContainer = document.getElementById('optionsList');
  const nextBtn = document.getElementById('quizNextBtn');
  const prevBtn = document.getElementById('quizPrevBtn');
  const resultsBanner = document.getElementById('quizResultsBanner');
  const scoreDisplay = document.getElementById('resultsScoreDisplay');
  const breakdownContainer = document.getElementById('resultsBreakdown');
  const retakeBtn = document.getElementById('retakeQuizBtn');

  function renderStepperDots() {
    stepperDotsContainer.innerHTML = '';
    questions.forEach((q, idx) => {
      const dot = document.createElement('div');
      dot.className = 'step-dot';
      if (idx === currentIdx) dot.classList.add('active');
      else if (userAnswers[q.id] !== undefined) dot.classList.add('completed');
      stepperDotsContainer.appendChild(dot);
    });
  }

  function renderCurrentQuestion() {
    const q = questions[currentIdx];
    progressLabel.textContent = `QUESTION ${currentIdx + 1} OF ${questions.length}`;
    questionPromptEl.textContent = `${currentIdx + 1}. ${q.question}`;

    renderStepperDots();

    // Render options
    optionsListContainer.innerHTML = '';
    const optionLetters = ['A', 'B', 'C', 'D'];

    q.options.forEach((optText, optIdx) => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'option-btn';
      if (userAnswers[q.id] === optIdx) {
        btn.classList.add('selected');
      }

      btn.innerHTML = `
        <span class="option-key">${optionLetters[optIdx]}</span>
        <span>${optText}</span>
      `;

      btn.addEventListener('click', () => {
        userAnswers[q.id] = optIdx;
        // Update selection UI
        document.querySelectorAll('.option-btn').forEach((b) => b.classList.remove('selected'));
        btn.classList.add('selected');
        updateNextButtonState();
        renderStepperDots();
      });

      optionsListContainer.appendChild(btn);
    });

    // Update navigation controls
    prevBtn.style.visibility = currentIdx > 0 ? 'visible' : 'hidden';

    const isLastQuestion = currentIdx === questions.length - 1;
    if (isLastQuestion) {
      nextBtn.textContent = 'SUBMIT TEST 🚀';
      nextBtn.classList.remove('secondary');
      nextBtn.classList.add('amber');
    } else {
      nextBtn.textContent = 'NEXT QUESTION ➔';
      nextBtn.classList.remove('amber');
    }

    updateNextButtonState();
  }

  function updateNextButtonState() {
    const currentQ = questions[currentIdx];
    const isAnswered = userAnswers[currentQ.id] !== undefined;
    nextBtn.disabled = !isAnswered;
  }

  prevBtn.addEventListener('click', () => {
    if (currentIdx > 0) {
      currentIdx--;
      renderCurrentQuestion();
    }
  });

  nextBtn.addEventListener('click', async () => {
    const isLastQuestion = currentIdx === questions.length - 1;

    if (!isLastQuestion) {
      currentIdx++;
      renderCurrentQuestion();
    } else {
      // Final Submit
      await submitQuiz();
    }
  });

  async function submitQuiz() {
    nextBtn.disabled = true;
    nextBtn.textContent = 'EVALUATING...';

    try {
      const res = await fetch(`/api/lessons/${lessonId}/quiz/submit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          studentName: 'Learner',
          answers: userAnswers
        })
      });

      if (!res.ok) {
        alert('Failed to submit quiz results.');
        nextBtn.disabled = false;
        nextBtn.textContent = 'SUBMIT TEST 🚀';
        return;
      }

      const result = await res.json();
      displayResults(result);

    } catch (err) {
      console.error('Quiz submission error:', err);
      alert('Error submitting quiz.');
      nextBtn.disabled = false;
      nextBtn.textContent = 'SUBMIT TEST 🚀';
    }
  }

  function displayResults(result) {
    // Hide question area or show banner below
    resultsBanner.classList.add('active');
    scoreDisplay.textContent = `SCORE: ${result.score} / ${result.total} (${result.percentage}%)`;

    breakdownContainer.innerHTML = '';
    result.questionResults.forEach((evalItem, idx) => {
      const card = document.createElement('div');
      card.className = `eval-card ${evalItem.isCorrect ? 'correct' : 'incorrect'}`;

      const icon = evalItem.isCorrect ? '✔ CORRECT' : '✖ INCORRECT';
      const userSelected = evalItem.selectedOptionIndex >= 0
        ? questions[idx].options[evalItem.selectedOptionIndex]
        : 'None selected';
      const correctAnswer = questions[idx].options[evalItem.correctOptionIndex];

      card.innerHTML = `
        <div class="eval-q-title" style="color: ${evalItem.isCorrect ? 'var(--neon-green)' : 'var(--neon-pink)'}">
          Q${idx + 1}: ${icon}
        </div>
        <div class="eval-detail"><strong>Question:</strong> ${evalItem.question}</div>
        <div class="eval-detail" style="color: ${evalItem.isCorrect ? 'var(--neon-green)' : 'var(--neon-pink)'}">
          <strong>Your Answer:</strong> ${userSelected}
        </div>
        ${!evalItem.isCorrect ? `<div class="eval-detail" style="color: var(--neon-cyan)"><strong>Correct Answer:</strong> ${correctAnswer}</div>` : ''}
        <div class="eval-explanation"><strong>Explanation:</strong> ${evalItem.explanation}</div>
      `;
      breakdownContainer.appendChild(card);
    });

    // Smooth scroll down to results
    resultsBanner.scrollIntoView({ behavior: 'smooth' });
  }

  retakeBtn.addEventListener('click', () => {
    // Clear answers and reset
    for (const key in userAnswers) delete userAnswers[key];
    currentIdx = 0;
    resultsBanner.classList.remove('active');
    renderCurrentQuestion();
    document.getElementById('quizSection').scrollIntoView({ behavior: 'smooth' });
  });

  renderCurrentQuestion();
}
