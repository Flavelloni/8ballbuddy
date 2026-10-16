export class ChallengeError extends Error {
  constructor(code, message) {
    super(message);
    this.code = code;
  }
}

export function validateAlias(value) {
  if (typeof value !== "string" || /[\p{Cc}\p{Cf}]/u.test(value)) {
    throw new ChallengeError("invalid-argument", "Choose a leaderboard name using letters, numbers, spaces, underscores or hyphens.");
  }
  const alias = value.normalize("NFKC").trim().replace(/\s+/gu, " ");
  if ([...alias].length < 3 || [...alias].length > 20) {
    throw new ChallengeError("invalid-argument", "Your leaderboard name must have 3 to 20 characters.");
  }
  if (!/^[\p{L}\p{M}\p{N} _-]+$/u.test(alias)) {
    throw new ChallengeError("invalid-argument", "Choose a leaderboard name using letters, numbers, spaces, underscores or hyphens.");
  }
  return alias;
}

export function pickerBalls(group) {
  if (group === 1) return [1, 2, 3, 4, 5, 6, 7];
  if (group === 2) return [9, 10, 11, 12, 13, 14, 15];
  if (group === 0) return [...pickerBalls(1), ...pickerBalls(2)];
  return [];
}

export function utcDate(now) {
  return new Date(now).toISOString().slice(0, 10);
}

function shotHash(date, id) {
  let hash = 0x811c9dc5;
  for (const char of `${date}/${id}`) hash = Math.imul(hash ^ char.charCodeAt(0), 16777619);
  return hash >>> 0;
}

export function createDailyChallenge(date, catalog) {
  const shots = [...new Map(catalog.map(shot => [shot.id, shot])).values()]
    .filter(shot => pickerBalls(shot.group).includes(shot.correctBall))
    .sort((a, b) => shotHash(date, a.id) - shotHash(date, b.id) || (a.id < b.id ? -1 : 1))
    .slice(0, 10);
  if (!shots.length) throw new ChallengeError("failed-precondition", "No challenge shots are available.");
  return { date, shotIds: shots.map(shot => shot.id), shots };
}

export function createAttempt(challenge, uid, displayName, now) {
  return {
    uid, date: challenge.date, displayName, answers: [],
    currentShotStartedAtMs: null, createdAtMs: now, updatedAtMs: now,
  };
}

export function startShot(challenge, attempt, shotId, now) {
  if (attempt.answers.some(answer => answer.shotId === shotId)) return attempt;
  if (shotId !== challenge.shotIds[attempt.answers.length]) {
    throw new ChallengeError("failed-precondition", "Please continue with the current shot.");
  }
  return {
    ...attempt,
    currentShotStartedAtMs: attempt.currentShotStartedAtMs ?? now,
    updatedAtMs: now,
  };
}

export function submitAnswer(challenge, attempt, shotId, selectedBall, now) {
  if (!Number.isInteger(selectedBall)) {
    throw new ChallengeError("invalid-argument", "Choose a valid ball.");
  }
  // Retrying an accepted answer returns its original result, including after a lost response.
  if (attempt.answers.some(answer => answer.shotId === shotId)) return attempt;
  const shot = challenge.shots[attempt.answers.length];
  if (!shot || shot.id !== shotId) {
    throw new ChallengeError("failed-precondition", "Please continue with the current shot.");
  }
  if (!pickerBalls(shot.group).includes(selectedBall)) {
    throw new ChallengeError("invalid-argument", "Choose a ball from this player's group.");
  }
  if (attempt.currentShotStartedAtMs == null) {
    throw new ChallengeError("failed-precondition", "The shot clock has not started yet.");
  }
  const answer = {
    shotId, selectedBall, correctBall: shot.correctBall,
    elapsedMs: Math.max(1, now - attempt.currentShotStartedAtMs),
  };
  return {
    ...attempt, answers: [...attempt.answers, answer],
    currentShotStartedAtMs: null, updatedAtMs: now,
  };
}

export function publicAttempt(challenge, attempt, now) {
  return {
    displayName: attempt.displayName,
    date: challenge.date, shotIds: challenge.shotIds, answers: attempt.answers,
    currentShotElapsedMs: attempt.currentShotStartedAtMs == null ? 0 : Math.max(0, now - attempt.currentShotStartedAtMs),
    clockStarted: attempt.currentShotStartedAtMs != null,
  };
}

export function completedResult(challenge, attempt) {
  if (attempt.answers.length !== challenge.shotIds.length) return null;
  return {
    uid: attempt.uid, displayName: attempt.displayName,
    correctCount: attempt.answers.filter(answer => answer.selectedBall === answer.correctBall).length,
    totalElapsedMs: attempt.answers.reduce((total, answer) => total + answer.elapsedMs, 0),
    shotCount: challenge.shotIds.length, finishedAtMs: attempt.updatedAtMs,
  };
}
