/**
 * EDUSLOP 2D Retro Pixel Canvas Animation Engine
 * Renders structured JSON scenes with authentic 8-bit / 16-bit pixel aesthetics.
 */

class PixelEngine {
  constructor(canvasElement, dialogueElement, sceneTitleElement, timeDisplayElement, progressBarElement) {
    this.canvas = canvasElement;
    this.ctx = canvasElement.getContext('2d');
    this.dialogueEl = dialogueElement;
    this.sceneTitleEl = sceneTitleElement;
    this.timeDisplayEl = timeDisplayElement;
    this.progressBarEl = progressBarElement;

    // Internal virtual resolution (16:9 crisp retro canvas)
    this.virtualWidth = 480;
    this.virtualHeight = 270;
    this.canvas.width = this.virtualWidth;
    this.canvas.height = this.virtualHeight;

    this.animationData = null;
    this.isPlaying = false;
    this.playbackSpeed = 1.0;
    this.currentTime = 0; // seconds
    this.totalDuration = 60; // seconds
    this.lastTimestamp = 0;
    this.currentSceneIndex = -1;

    this.typewriterTimer = null;
    this.displayedText = '';

    // Pixel color palette constants
    this.palette = {
      cyan: '#00f0ff',
      green: '#39ff14',
      amber: '#ffb703',
      pink: '#ff007f',
      purple: '#9d4edd',
      white: '#ffffff',
      darkNavy: '#0a0e17',
      gridLine: 'rgba(0, 240, 255, 0.12)'
    };

    this.setupDisplay();
  }

  setupDisplay() {
    this.ctx.imageSmoothingEnabled = false;
    this.renderBlankState();
  }

  loadAnimation(animationData) {
    this.animationData = animationData;
    this.totalDuration = animationData.totalDurationSeconds || 60;
    this.currentTime = 0;
    this.currentSceneIndex = -1;
    this.isPlaying = false;
    this.updateTimelineUI();
    this.renderAtTime(0);
  }

  play() {
    if (!this.animationData) return;
    if (this.currentTime >= this.totalDuration) {
      this.currentTime = 0;
    }
    this.isPlaying = true;
    this.lastTimestamp = performance.now();
    requestAnimationFrame((t) => this.loop(t));
  }

  pause() {
    this.isPlaying = false;
  }

  togglePlay() {
    if (this.isPlaying) this.pause();
    else this.play();
  }

  restart() {
    this.currentTime = 0;
    this.currentSceneIndex = -1;
    this.updateTimelineUI();
    this.renderAtTime(0);
    this.play();
  }

  seek(ratio) {
    this.currentTime = Math.max(0, Math.min(this.totalDuration, ratio * this.totalDuration));
    this.updateTimelineUI();
    this.renderAtTime(this.currentTime);
  }

  setSpeed(speed) {
    this.playbackSpeed = speed;
  }

  loop(timestamp) {
    if (!this.isPlaying) return;

    const delta = (timestamp - this.lastTimestamp) / 1000;
    this.lastTimestamp = timestamp;

    this.currentTime += delta * this.playbackSpeed;

    if (this.currentTime >= this.totalDuration) {
      this.currentTime = this.totalDuration;
      this.isPlaying = false;
      this.renderAtTime(this.currentTime);
      this.updateTimelineUI();
      return;
    }

    this.renderAtTime(this.currentTime);
    this.updateTimelineUI();

    requestAnimationFrame((t) => this.loop(t));
  }

  getCurrentScene(time) {
    if (!this.animationData || !this.animationData.scenes) return null;
    let accumulated = 0;
    for (let i = 0; i < this.animationData.scenes.length; i++) {
      const scene = this.animationData.scenes[i];
      const duration = scene.durationSeconds || 15;
      if (time >= accumulated && time < accumulated + duration) {
        return { scene, index: i, sceneLocalTime: time - accumulated, sceneDuration: duration };
      }
      accumulated += duration;
    }
    // Return last scene if past total duration
    const lastIdx = this.animationData.scenes.length - 1;
    const lastScene = this.animationData.scenes[lastIdx];
    return { scene: lastScene, index: lastIdx, sceneLocalTime: lastScene.durationSeconds, sceneDuration: lastScene.durationSeconds };
  }

  renderAtTime(time) {
    const sceneInfo = this.getCurrentScene(time);
    if (!sceneInfo) return;

    const { scene, index, sceneLocalTime, sceneDuration } = sceneInfo;

    // Check if scene changed
    if (this.currentSceneIndex !== index) {
      this.currentSceneIndex = index;
      this.onSceneTransition(scene, index);
    }

    // Clear canvas
    this.ctx.clearRect(0, 0, this.virtualWidth, this.virtualHeight);

    // Draw background
    this.drawBackground(scene.background || 'retro_dark', sceneLocalTime);

    // Draw scene border / viewport frame
    this.drawPixelFrame();

    // Draw connections/actions
    this.drawSceneActions(scene, sceneLocalTime, sceneDuration);

    // Draw entities
    this.drawEntities(scene.entities || [], sceneLocalTime, scene);

    // Update typewriter dialogue
    this.updateTypewriter(scene.narration, sceneLocalTime, sceneDuration);
  }

  onSceneTransition(scene, index) {
    if (this.sceneTitleEl) {
      this.sceneTitleEl.textContent = `SCENE ${index + 1}/${this.animationData.scenes.length}: ${scene.title.toUpperCase()}`;
    }
  }

  updateTypewriter(narration, sceneLocalTime, sceneDuration) {
    if (!this.dialogueEl) return;
    const progress = Math.min(1, sceneLocalTime / (sceneDuration * 0.75)); // Complete text by 75% of scene
    const charsToShow = Math.floor(narration.length * progress);
    this.dialogueEl.textContent = narration.substring(0, charsToShow);
  }

  updateTimelineUI() {
    if (this.progressBarEl) {
      const progress = (this.currentTime / this.totalDuration) * 100;
      this.progressBarEl.style.width = `${progress}%`;
    }
    if (this.timeDisplayEl) {
      const curM = Math.floor(this.currentTime / 60);
      const curS = Math.floor(this.currentTime % 60).toString().padStart(2, '0');
      const totM = Math.floor(this.totalDuration / 60);
      const totS = Math.floor(this.totalDuration % 60).toString().padStart(2, '0');
      this.timeDisplayEl.textContent = `${curM}:${curS} / ${totM}:${totS}`;
    }
  }

  // ==========================================
  // BACKGROUND RENDERING
  // ==========================================
  drawBackground(theme, localTime) {
    const w = this.virtualWidth;
    const h = this.virtualHeight;

    if (theme === 'blueprint_blue') {
      this.ctx.fillStyle = '#0a1d37';
      this.ctx.fillRect(0, 0, w, h);

      // Blueprint grid lines
      this.ctx.strokeStyle = 'rgba(0, 240, 255, 0.15)';
      this.ctx.lineWidth = 1;
      const gridSize = 24;
      for (let x = 0; x < w; x += gridSize) {
        this.ctx.beginPath();
        this.ctx.moveTo(x, 0);
        this.ctx.lineTo(x, h);
        this.ctx.stroke();
      }
      for (let y = 0; y < h; y += gridSize) {
        this.ctx.beginPath();
        this.ctx.moveTo(0, y);
        this.ctx.lineTo(w, y);
        this.ctx.stroke();
      }
    } else if (theme === 'terminal_green') {
      this.ctx.fillStyle = '#061309';
      this.ctx.fillRect(0, 0, w, h);

      // Terminal scan lines
      this.ctx.fillStyle = 'rgba(57, 255, 20, 0.05)';
      for (let y = 0; y < h; y += 4) {
        this.ctx.fillRect(0, y, w, 2);
      }
    } else if (theme === 'cyber_purple') {
      this.ctx.fillStyle = '#160a2b';
      this.ctx.fillRect(0, 0, w, h);

      // Cyberpunk perspective grid floor
      const horizon = h * 0.65;
      this.ctx.fillStyle = '#100520';
      this.ctx.fillRect(0, horizon, w, h - horizon);

      this.ctx.strokeStyle = 'rgba(255, 0, 127, 0.25)';
      this.ctx.lineWidth = 1;
      for (let x = 0; x <= w; x += 36) {
        this.ctx.beginPath();
        this.ctx.moveTo(w / 2, horizon);
        this.ctx.lineTo(x, h);
        this.ctx.stroke();
      }
      for (let y = horizon; y <= h; y += 14) {
        this.ctx.beginPath();
        this.ctx.moveTo(0, y);
        this.ctx.lineTo(w, y);
        this.ctx.stroke();
      }
    } else {
      // retro_dark default
      this.ctx.fillStyle = '#080c14';
      this.ctx.fillRect(0, 0, w, h);

      // Subtle twinkling star pixels
      this.ctx.fillStyle = 'rgba(255, 255, 255, 0.4)';
      const stars = [
        [30, 25], [110, 40], [210, 18], [340, 45], [420, 30],
        [75, 180], [180, 220], [380, 200], [450, 160], [260, 240]
      ];
      stars.forEach(([sx, sy], i) => {
        const twinkle = Math.sin(localTime * 4 + i) > 0;
        if (twinkle) {
          this.ctx.fillRect(sx, sy, 2, 2);
        }
      });
    }
  }

  drawPixelFrame() {
    this.ctx.strokeStyle = '#223049';
    this.ctx.lineWidth = 4;
    this.ctx.strokeRect(2, 2, this.virtualWidth - 4, this.virtualHeight - 4);
  }

  // ==========================================
  // SCENE ACTIONS (CONNECTIONS, ARROWS, HIGHLIGHTS)
  // ==========================================
  drawSceneActions(scene, localTime, sceneDuration) {
    if (!scene.actions) return;

    scene.actions.forEach((act) => {
      if (act.type === 'connect') {
        const fromEntity = (scene.entities || []).find((e) => e.id === act.target);
        if (fromEntity && act.toX != null && act.toY != null) {
          const x1 = (fromEntity.x / 100) * this.virtualWidth;
          const y1 = (fromEntity.y / 100) * this.virtualHeight;
          const x2 = (act.toX / 100) * this.virtualWidth;
          const y2 = (act.toY / 100) * this.virtualHeight;

          this.drawPixelArrow(x1, y1, x2, y2, localTime);
        }
      }
    });
  }

  drawPixelArrow(x1, y1, x2, y2, time) {
    this.ctx.save();
    this.ctx.strokeStyle = this.palette.cyan;
    this.ctx.lineWidth = 2;
    this.ctx.setLineDash([4, 4]);
    this.ctx.lineDashOffset = -time * 20;

    this.ctx.beginPath();
    this.ctx.moveTo(x1, y1);
    this.ctx.lineTo(x2, y2);
    this.ctx.stroke();

    // Flowing energy pixel
    const t = (time * 1.5) % 1;
    const px = x1 + (x2 - x1) * t;
    const py = y1 + (y2 - y1) * t;
    this.ctx.fillStyle = '#ffffff';
    this.ctx.fillRect(px - 3, py - 3, 6, 6);

    this.ctx.restore();
  }

  // ==========================================
  // ENTITY SPRITE RENDERING
  // ==========================================
  drawEntities(entities, localTime, scene) {
    entities.forEach((ent) => {
      // Calculate coordinates with any active action transforms
      let px = (ent.x / 100) * this.virtualWidth;
      let py = (ent.y / 100) * this.virtualHeight;

      // Check actions affecting this entity
      const action = (scene.actions || []).find((a) => a.target === ent.id);
      let scale = 1.0;

      if (action) {
        if (action.type === 'pulse') {
          scale = 1.0 + Math.sin(localTime * 6) * 0.12;
        } else if (action.type === 'move' && action.toX != null && action.toY != null) {
          const t = Math.min(1, localTime / (action.duration || 3.0));
          px = px + ((action.toX / 100) * this.virtualWidth - px) * t;
          py = py + ((action.toY / 100) * this.virtualHeight - py) * t;
        } else if (action.type === 'highlight') {
          this.drawHighlightBox(px, py, 40, localTime);
        }
      }

      this.ctx.save();
      this.ctx.translate(px, py);
      this.ctx.scale(scale, scale);

      // Draw the sprite
      this.drawSprite(ent.sprite || 'model', ent.color || this.palette.cyan, localTime);

      // Draw label
      this.drawEntityLabel(ent.label || ent.name, ent.color || this.palette.cyan);

      this.ctx.restore();
    });
  }

  drawHighlightBox(x, y, radius, time) {
    this.ctx.save();
    const blink = Math.floor(time * 4) % 2 === 0;
    if (blink) {
      this.ctx.strokeStyle = this.palette.amber;
      this.ctx.lineWidth = 2;
      this.ctx.strokeRect(x - radius, y - radius, radius * 2, radius * 2);
    }
    this.ctx.restore();
  }

  drawEntityLabel(text, color) {
    this.ctx.save();
    this.ctx.font = '10px monospace';
    this.ctx.textAlign = 'center';

    const textWidth = this.ctx.measureText(text).width;
    this.ctx.fillStyle = 'rgba(10, 14, 23, 0.85)';
    this.ctx.fillRect(-textWidth / 2 - 4, 24, textWidth + 8, 14);

    this.ctx.strokeStyle = color;
    this.ctx.lineWidth = 1;
    this.ctx.strokeRect(-textWidth / 2 - 4, 24, textWidth + 8, 14);

    this.ctx.fillStyle = '#ffffff';
    this.ctx.fillText(text, 0, 35);
    this.ctx.restore();
  }

  drawSprite(spriteType, color, time) {
    const bob = Math.sin(time * 4) * 2;

    switch (spriteType) {
      case 'character':
      case 'user':
        this.drawCharacterSprite(spriteType === 'user' ? '#ffaa00' : '#00f0ff', bob);
        break;
      case 'database':
        this.drawDatabaseSprite(color, bob);
        break;
      case 'aggregate':
        this.drawAggregateSprite(color, time);
        break;
      case 'bounded_context':
        this.drawBoundedContextSprite(color, time);
        break;
      case 'event':
        this.drawEventSprite(color, time);
        break;
      case 'service':
        this.drawServiceSprite(color, time);
        break;
      case 'box':
        this.drawBoxSprite(color, bob);
        break;
      default:
        this.drawModelSprite(color, bob);
    }
  }

  // 1. Character / Avatar Sprite (16x16 pixel block representation)
  drawCharacterSprite(hatColor, bob) {
    const y = bob;
    // Hair/Hat
    this.ctx.fillStyle = hatColor;
    this.ctx.fillRect(-8, -16 + y, 16, 6);
    // Face
    this.ctx.fillStyle = '#ffd199';
    this.ctx.fillRect(-6, -10 + y, 12, 8);
    // Eyes
    this.ctx.fillStyle = '#000000';
    this.ctx.fillRect(-4, -8 + y, 2, 2);
    this.ctx.fillRect(2, -8 + y, 2, 2);
    // Torso / Jacket
    this.ctx.fillStyle = '#1e3a5f';
    this.ctx.fillRect(-8, -2 + y, 16, 12);
    // Legs
    this.ctx.fillStyle = '#111927';
    this.ctx.fillRect(-6, 10 + y, 4, 8);
    this.ctx.fillRect(2, 10 + y, 4, 8);
  }

  // 2. Database Cylinder Stack
  drawDatabaseSprite(color, bob) {
    const y = bob;
    this.ctx.fillStyle = '#1c2538';
    this.ctx.strokeStyle = color;
    this.ctx.lineWidth = 2;

    for (let layer = 0; layer < 3; layer++) {
      const ly = -14 + layer * 10 + y;
      this.ctx.fillRect(-14, ly, 28, 8);
      this.ctx.strokeRect(-14, ly, 28, 8);

      // Blinking status LED pixel
      this.ctx.fillStyle = (layer + Math.floor(Date.now() / 300)) % 2 === 0 ? this.palette.green : this.palette.pink;
      this.ctx.fillRect(8, ly + 2, 3, 3);
      this.ctx.fillStyle = '#1c2538';
    }
  }

  // 3. Aggregate Root (Guardian Shield / Invariant Enclosure)
  drawAggregateSprite(color, time) {
    this.ctx.save();
    // Glowing perimeter boundary
    this.ctx.strokeStyle = color;
    this.ctx.lineWidth = 2;
    this.ctx.setLineDash([4, 2]);
    this.ctx.lineDashOffset = time * 8;
    this.ctx.beginPath();
    this.ctx.arc(0, 0, 22, 0, Math.PI * 2);
    this.ctx.stroke();

    // Shield Emblem inside
    this.ctx.fillStyle = color;
    this.ctx.beginPath();
    this.ctx.moveTo(-10, -10);
    this.ctx.lineTo(10, -10);
    this.ctx.lineTo(10, 2);
    this.ctx.lineTo(0, 12);
    this.ctx.lineTo(-10, 2);
    this.ctx.closePath();
    this.ctx.fill();

    // Center invariant core dot
    this.ctx.fillStyle = '#000000';
    this.ctx.fillRect(-2, -2, 4, 4);
    this.ctx.restore();
  }

  // 4. Bounded Context Box
  drawBoundedContextSprite(color, time) {
    this.ctx.save();
    this.ctx.strokeStyle = color;
    this.ctx.lineWidth = 3;
    this.ctx.fillStyle = 'rgba(20, 30, 50, 0.45)';

    // Dashed boundary
    this.ctx.setLineDash([6, 3]);
    this.ctx.lineDashOffset = -time * 6;
    this.ctx.fillRect(-32, -20, 64, 40);
    this.ctx.strokeRect(-32, -20, 64, 40);

    // Corner pixel notches
    this.ctx.fillStyle = color;
    this.ctx.fillRect(-34, -22, 5, 5);
    this.ctx.fillRect(29, -22, 5, 5);
    this.ctx.fillRect(-34, 17, 5, 5);
    this.ctx.fillRect(29, 17, 5, 5);
    this.ctx.restore();
  }

  // 5. Domain Event / Message Packet
  drawEventSprite(color, time) {
    this.ctx.save();
    // Lightning / envelope shape
    this.ctx.fillStyle = color;
    this.ctx.beginPath();
    this.ctx.moveTo(-6, -14);
    this.ctx.lineTo(4, -14);
    this.ctx.lineTo(-2, -2);
    this.ctx.lineTo(8, -2);
    this.ctx.lineTo(-4, 14);
    this.ctx.lineTo(-1, 2);
    this.ctx.lineTo(-8, 2);
    this.ctx.closePath();
    this.ctx.fill();

    // Pixel halo
    this.ctx.strokeStyle = '#ffffff';
    this.ctx.lineWidth = 1;
    this.ctx.stroke();
    this.ctx.restore();
  }

  // 6. Microservice Engine
  drawServiceSprite(color, time) {
    this.ctx.save();
    this.ctx.fillStyle = '#111c2e';
    this.ctx.strokeStyle = color;
    this.ctx.lineWidth = 2;
    this.ctx.fillRect(-14, -14, 28, 28);
    this.ctx.strokeRect(-14, -14, 28, 28);

    // Chip pins on edges
    this.ctx.fillStyle = color;
    [-8, 0, 8].forEach((offset) => {
      this.ctx.fillRect(-17, offset - 1, 3, 2);
      this.ctx.fillRect(14, offset - 1, 3, 2);
      this.ctx.fillRect(offset - 1, -17, 2, 3);
      this.ctx.fillRect(offset - 1, 14, 2, 3);
    });

    // Spinning core
    this.ctx.rotate(time * 2);
    this.ctx.fillRect(-4, -4, 8, 8);
    this.ctx.restore();
  }

  // 7. Value Object / Box
  drawBoxSprite(color, bob) {
    const y = bob;
    this.ctx.fillStyle = '#1b2a3a';
    this.ctx.strokeStyle = color;
    this.ctx.lineWidth = 2;
    this.ctx.fillRect(-12, -12 + y, 24, 24);
    this.ctx.strokeRect(-12, -12 + y, 24, 24);

    // Ribbon / seal
    this.ctx.fillStyle = color;
    this.ctx.fillRect(-2, -12 + y, 4, 24);
    this.ctx.fillRect(-12, -2 + y, 24, 4);
  }

  // 8. Domain Model Diamond
  drawModelSprite(color, bob) {
    const y = bob;
    this.ctx.fillStyle = '#162338';
    this.ctx.strokeStyle = color;
    this.ctx.lineWidth = 2;

    this.ctx.beginPath();
    this.ctx.moveTo(0, -16 + y);
    this.ctx.lineTo(16, y);
    this.ctx.lineTo(0, 16 + y);
    this.ctx.lineTo(-16, y);
    this.ctx.closePath();
    this.ctx.fill();
    this.ctx.stroke();

    this.ctx.fillStyle = color;
    this.ctx.fillRect(-3, -3 + y, 6, 6);
  }

  renderBlankState() {
    this.ctx.fillStyle = '#060a12';
    this.ctx.fillRect(0, 0, this.virtualWidth, this.virtualHeight);
    this.ctx.fillStyle = '#313d52';
    this.ctx.font = '12px monospace';
    this.ctx.textAlign = 'center';
    this.ctx.fillText('[ NO ANIMATION LOADED ]', this.virtualWidth / 2, this.virtualHeight / 2);
  }
}
